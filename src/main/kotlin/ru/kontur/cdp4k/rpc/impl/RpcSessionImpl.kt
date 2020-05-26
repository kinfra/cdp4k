package ru.kontur.cdp4k.rpc.impl

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import ru.kontur.cdp4k.connection.ConnectionClosedException
import ru.kontur.cdp4k.rpc.RpcErrorException
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.jinfra.logging.Logger
import ru.kontur.kinfra.commons.getOrThrow
import ru.kontur.kinfra.commons.lowerCaseName
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal class RpcSessionImpl(
    internal val sessionId: String?,
    override val connection: DefaultRpcConnection,
    private val nextRequestId: AtomicLong,
    private val scope: CoroutineScope
) : RpcSession {

    private val state = AtomicReference(SessionState.ACTIVE)

    private val activeRequests = ConcurrentHashMap<Long, CompletableDeferred<RpcResult>>()
    private val subscriptions = ConcurrentHashMap<String, MutableCollection<Subscription>>()

    private lateinit var incomingMessages: SendChannel<IncomingMessage>

    internal fun open() {
        @OptIn(ObsoleteCoroutinesApi::class)
        incomingMessages = scope.actor(capacity = 1) {
            for (message in channel) {
                handleIncomingMessage(message)
            }
        }
    }

    override suspend fun executeRequest(methodName: String, params: ObjectNode): ObjectNode {
        checkNotClosed()

        val id = nextRequestId.getAndIncrement()
        val resultDeferred = CompletableDeferred<RpcResult>()
        activeRequests[id] = resultDeferred

        logger.debug { "Sending request $id (session: $sessionId) $methodName $params" }
        connection.sendRequest(sessionId, id, methodName, params)

        val result = try {
            checkActive()
            resultDeferred.await()
        } catch (e: Exception) {
            logger.debug { "Interrupted waiting for a response (id: $id): $e" }
            throw e
        } finally {
            resultDeferred.cancel()
            activeRequests.remove(id)
        }

        return result.getOrThrow { error ->
            val code = error.get("code").intValue()
            val message = error.get("message").textValue()
            val data = error.get("data")
            RpcErrorException(code, message, data)
        }
    }

    override fun subscribe(methodName: String, callback: (ObjectNode) -> Unit): RpcSession.EventSubscription {
        checkNotClosed()
        val methodSubscriptions = subscriptions.computeIfAbsent(methodName) { CopyOnWriteArrayList() }
        return Subscription(methodSubscriptions, callback).also { it.register() }
    }

    private fun checkNotClosed() {
        check(state.get() != SessionState.CLOSED) { "Session is closed" }
    }

    private fun checkActive() {
        check(state.get() == SessionState.ACTIVE) { "Session is ${state.get().lowerCaseName}" }
    }

    internal suspend fun onIncomingMessage(message: IncomingMessage) {
        if (state.get() == SessionState.CLOSED) return

        try {
            incomingMessages.send(message)
        } catch (e: ClosedSendChannelException) {
            logger.debug { "Ignoring message ${message.messageId}: channel is closed" }
        } catch (e: CancellationException) {
            logger.debug { "Ignoring message ${message.messageId}: handler is cancelled" }
        }
    }

    private suspend fun handleIncomingMessage(message: IncomingMessage) {
        val handled = when (message) {
            is IncomingMessage.Response -> handleResponse(message)
            is IncomingMessage.Event -> handleEvent(message)
        }
        logger.debug { "Processed message ${message.messageId} (handled: $handled)" }
    }

    private fun handleResponse(response: IncomingMessage.Response): Boolean {
        val (requestId, rpcResult) = response
        val request = activeRequests.remove(requestId)
        return if (request != null) {
            request.complete(rpcResult)
            true
        } else {
            false
        }
    }

    private suspend fun handleEvent(event: IncomingMessage.Event): Boolean {
        val (methodName, data) = event
        var handled = false
        subscriptions[methodName]?.let { methodSubscriptions ->
            try {
                for (subscription in methodSubscriptions) {
                    subscription.notifyEvent(data)
                    handled = true
                }
            } catch (e: Exception) {
                logger.error {
                    "Failed to process event message ${event.messageId}:" +
                        " subscriber of $methodName threw an exception;" +
                        " session will be closed"
                }
                throw e
            }
        }
        return handled
    }

    internal fun onDisconnect() {
        if (!state.compareAndSet(SessionState.ACTIVE, SessionState.DISCONNECTED)) return
        logger.withoutContext().debug { "${this@RpcSessionImpl} is disconnected" }
        incomingMessages.close(ConnectionClosedException("Connection is closed"))
    }

    internal fun close() {
        if (!state.compareAndSet(SessionState.ACTIVE, SessionState.CLOSED)) return
        logger.withoutContext().debug { "${this@RpcSessionImpl} is closed" }
        incomingMessages.close()
    }

    override fun toString(): String {
        return "RpcSession(id: $sessionId, hash: ${hashCode().toString(16)})"
    }

    private enum class SessionState {
        ACTIVE,
        DISCONNECTED,
        CLOSED,
    }

    private class Subscription(
        private val container: MutableCollection<Subscription>,
        private val callback: (ObjectNode) -> Unit
    ) : RpcSession.EventSubscription {

        fun register() {
            container.add(this)
        }

        fun notifyEvent(data: ObjectNode) {
            callback(data)
        }

        override fun close() {
            container.remove(this)
        }

    }

    companion object {

        private val logger = Logger.currentClass()

    }

}
