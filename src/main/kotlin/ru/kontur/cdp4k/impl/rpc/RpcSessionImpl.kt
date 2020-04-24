package ru.kontur.cdp4k.impl.rpc

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
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
    private val nextRequestId: AtomicLong
): RpcSession {

    private val state = AtomicReference(SessionState.ACTIVE)

    private val activeRequests = ConcurrentHashMap<Long, Request>()
    private val subscriptions = ConcurrentHashMap<String, MutableCollection<Subscription>>()

    override suspend fun executeRequest(methodName: String, params: ObjectNode): ObjectNode {
        checkNotClosed()

        val id = nextRequestId.getAndIncrement()
        val request = Request(id)
        activeRequests[id] = request

        logger.debug { "Sending request $id (session: $sessionId) $methodName $params" }
        connection.sendRequest(sessionId, id, methodName, params)

        val result = try {
            checkActive()
            request.result.await()
        } finally {
            request.result.cancel()
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

    internal fun onResult(requestId: Long, rpcResult: RpcResult) {
        val logger = logger.withoutContext()
        val request = activeRequests.remove(requestId)
        if (request != null) {
            logger.debug { "Response for request $requestId (session: $sessionId): $rpcResult" }
            request.result.complete(rpcResult)
        } else {
            logger.debug { "Received response for unknown request $requestId (session: $sessionId)" }
        }
    }

    internal fun onEvent(methodName: String, data: ObjectNode) {
        val logger = logger.withoutContext()
        logger.debug { "Received an event for session $sessionId: $methodName $data" }

        subscriptions[methodName]?.let { methodSubscriptions ->
            for (subscription in methodSubscriptions) {
                subscription.notifyEvent(data)
            }
        }
    }

    internal fun onDisconnect() {
        if (!state.compareAndSet(SessionState.ACTIVE, SessionState.DISCONNECTED)) return
        cancelRequests("Connection is closed")
    }

    private fun cancelRequests(reason: String) {
        val count = activeRequests.size
        if (count > 0) {
            for (request in activeRequests.values) {
                request.result.cancel(reason)
            }
            logger.withoutContext().debug { "Cancelled $count requests in session $sessionId" }
        }
    }

    override fun close() {
        checkNotNull(sessionId) { "Browser session cannot be closed" }
        if (!state.compareAndSet(SessionState.ACTIVE, SessionState.CLOSED)) return
        connection.detachSession(sessionId)
        cancelRequests("Session is closed")
    }

    override fun toString(): String {
        return "DefaultRpcConnection.Session(id: $sessionId)"
    }

    private enum class SessionState {
        ACTIVE,
        DISCONNECTED,
        CLOSED,
    }

    private class Request(val id: Long) {
        val result = CompletableDeferred<RpcResult>()
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
