package ru.kontur.cdp4k.impl.rpc

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.*
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.time.withTimeout
import ru.kontur.cdp4k.connection.ChromeConnection
import ru.kontur.cdp4k.impl.EMPTY_TREE
import ru.kontur.cdp4k.impl.getObjectOrNull
import ru.kontur.cdp4k.impl.getString
import ru.kontur.cdp4k.impl.getStringOrNull
import ru.kontur.cdp4k.protocol.browser.BrowserDomain
import ru.kontur.cdp4k.rpc.RpcConnection
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.jinfra.logging.Logger
import ru.kontur.kinfra.commons.Either
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal typealias RpcResult = Either<ObjectNode, ObjectNode>

class DefaultRpcConnection private constructor(
    private val connection: ChromeConnection
) : RpcConnection {

    private val closed = AtomicBoolean(false)
    private val nextRequestId = AtomicLong(1)

    private val _browserSession = createSession(id = null)
    override val browserSession: RpcSession get() = _browserSession

    // Modifications must be performed with lock
    private val sessions = ConcurrentHashMap<String, RpcSessionImpl>()

    private suspend fun open() {
        connection.subscribe(
            object : ChromeConnection.Subscriber {
                override fun onIncomingMessage(message: ObjectNode) {
                    this@DefaultRpcConnection.onIncomingMessage(message)
                }

                override fun onConnectionClosed() {
                    this@DefaultRpcConnection.onConnectionClosed()
                }
            }
        )

        val browserDomain = BrowserDomain(browserSession)
        val version = browserDomain.getVersion()
        logger.info { "Connected to ${version.product} (protocol version: ${version.protocolVersion})" }

        startConnectionHealthCheck()
    }

    // fixme: move away to a decorator
    private fun startConnectionHealthCheck() {
        val browserDomain = BrowserDomain(browserSession)
        GlobalScope.launch(CoroutineName("RPC connection health check")) {
            while (isActive) {
                delay(HEALTH_CHECK_PERIOD)
                try {
                    withTimeout(HEALTH_CHECK_TIMEOUT) {
                        browserDomain.getVersion()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error(e) { "Health check failed, closing connection" }
                    close()
                    break
                }
            }
        }
    }

    override fun openSession(id: String): RpcSession {
        return synchronized(sessions) {
            check(sessions[id] == null) { "Session $id already exists" }
            createSession(id).also {
                sessions[id] = it
            }
        }
    }

    internal fun detachSession(id: String) {
        synchronized(sessions) {
            val session = sessions.remove(id)
            checkNotNull(session) { "No such session: $id" }
        }
    }

    private fun findSession(id: String?): RpcSessionImpl? {
        return if (id == null) {
            _browserSession
        } else {
            sessions[id]
        }
    }

    private fun createSession(id: String?): RpcSessionImpl {
        return RpcSessionImpl(id, this, nextRequestId)
    }

    internal suspend fun sendRequest(sessionId: String?, id: Long, methodName: String, params: ObjectNode) {
        val requestMessage = ObjectNode(JsonNodeFactory.instance).apply {
            put("id", id)
            if (sessionId != null) put("sessionId", sessionId)
            put("method", methodName)
            replace("params", params)
        }
        connection.send(requestMessage)
    }

    private fun onIncomingMessage(message: ObjectNode) {
        val logger = logger.withoutContext()

        val sessionId = message.getStringOrNull("sessionId")
        val session = findSession(sessionId) ?: run {
            logger.debug { "Received a message for unknown session $sessionId" }
            return
        }

        val requestId = message.get("id")
        if (requestId != null) {
            if (requestId !is NumericNode) {
                logger.warn { "Unexpected response id: $requestId" }
                return
            }

            val id = requestId.longValue()
            val rpcResult = parseResult(message)
            session.onResult(id, rpcResult)
        } else {
            val method = message.getString("method")
            val params = message.getObjectOrNull("params") ?: EMPTY_TREE
            session.onEvent(method, params)
        }
    }

    private fun parseResult(message: ObjectNode): RpcResult {
        val result = message.getObjectOrNull("result")
        val error = message.getObjectOrNull("error")
        return when {
            error == null -> {
                requireNotNull(result) {
                    val messageFields = message.fieldNames().asSequence().toList()
                    "Response must contain either result or error, got $messageFields"
                }
                Either.right(result)
            }

            result == null -> Either.left(error)

            else -> throw IllegalArgumentException("Response must contain either result or error, got both")
        }
    }

    private fun onConnectionClosed() {
        if (!closed.compareAndSet(false, true)) return
        cleanup()
    }

    override suspend fun close() {
        if (!closed.compareAndSet(false, true)) return
        logger.debug { "Closing connection" }
        try {
            connection.close()
        } finally {
            cleanup()
        }
    }

    private fun cleanup() {
        _browserSession.onDisconnect()
        for (session in sessions.values) {
            session.onDisconnect()
        }
    }

    companion object {

        private val logger = Logger.currentClass()

        private val HEALTH_CHECK_PERIOD = Duration.ofSeconds(5)
        private val HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(3)

        suspend fun open(chromeConnection: ChromeConnection): DefaultRpcConnection {
            return DefaultRpcConnection(chromeConnection).also {
                try {
                    it.open()
                } catch (e: Throwable) {
                    it.close()
                    throw e
                }
            }
        }

    }

}
