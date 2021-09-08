package ru.kontur.cdp4k.rpc.impl

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import ru.kontur.cdp4k.connection.ChromeConnection
import ru.kontur.cdp4k.connection.ConnectionClosedException
import ru.kontur.cdp4k.protocol.browser.BrowserDomain
import ru.kontur.cdp4k.protocol.browser.BrowserVersion
import ru.kontur.cdp4k.rpc.RpcConnection
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.cdp4k.util.getStringOrNull
import ru.kontur.kinfra.logging.Logger
import ru.kontur.kinfra.commons.binary.toHexString
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class DefaultRpcConnection private constructor(
    private val connection: ChromeConnection
) : RpcConnection {

    private val closed = AtomicBoolean(false)
    private val nextRequestId = AtomicLong(1)

    // Modifications must be performed with lock
    private val sessions = ConcurrentHashMap<String, MutableCollection<RpcSessionImpl>>()

    internal lateinit var browserVersion: BrowserVersion
        private set

    private suspend fun open() {
        connection.subscribe(
            object : ChromeConnection.Subscriber {
                override suspend fun onIncomingMessage(message: ObjectNode) {
                    this@DefaultRpcConnection.onIncomingMessage(message)
                }

                override fun onConnectionClosed() {
                    this@DefaultRpcConnection.onConnectionClosed()
                }
            }
        )

        val version = useBrowserSession { browserSession ->
            val browserDomain = BrowserDomain(browserSession)
            browserDomain.getVersion()
        }
        this.browserVersion = version
        logger.info { "Connected to ${version.product} (protocol version: ${version.protocolVersion})" }
    }

    override suspend fun <R> useBrowserSession(block: suspend CoroutineScope.(RpcSession) -> R): R {
        return useSessionInternal(null, block)
    }

    override suspend fun <R> useSession(id: String, block: suspend CoroutineScope.(RpcSession) -> R): R {
        return useSessionInternal(id, block)
    }

    private suspend fun <R> useSessionInternal(id: String?, block: suspend CoroutineScope.(RpcSession) -> R): R {
        return withContext(CoroutineName("CDP session $id")) {
            val session = openSession(id, this)
            try {
                coroutineScope {
                    block(session)
                }
            } finally {
                closeSession(session)
            }
        }
    }

    private fun findSessions(id: String?): Collection<RpcSessionImpl> {
        return sessions.getOrDefault(getSessionKey(id), emptyList())
    }

    private fun openSession(id: String?, scope: CoroutineScope): RpcSessionImpl {
        return synchronized(sessions) {
            if (closed.get()) {
                throw ConnectionClosedException("Connection $connection is closed")
            }

            val key = getSessionKey(id)
            val sessionsById = sessions.computeIfAbsent(key) { CopyOnWriteArrayList() }
            RpcSessionImpl(id, this, nextRequestId, scope).also {
                it.open()
                sessionsById.add(it)
            }
        }
    }

    private fun closeSession(session: RpcSessionImpl) {
        try {
            synchronized(sessions) {
                val key = getSessionKey(session.sessionId)
                val sessionsById = checkNotNull(sessions[key]) { "No sessions with ID ${session.sessionId} are opened" }
                val removed = sessionsById.remove(session)
                check(removed) { "Session $session not found" }
                if (sessionsById.isEmpty()) {
                    sessions.remove(key)
                }
            }
        } finally {
            session.close()
        }
    }

    private fun getSessionKey(id: String?) = id ?: BROWSER_SESSION_KEY

    internal suspend fun sendRequest(sessionId: String?, id: Long, methodName: String, params: ObjectNode) {
        val requestMessage = ObjectNode(JsonNodeFactory.instance).apply {
            put("id", id)
            if (sessionId != null) put("sessionId", sessionId)
            put("method", methodName)
            replace("params", params)
        }
        connection.send(requestMessage)
    }

    private suspend fun onIncomingMessage(message: ObjectNode) {
        val sessionId = message.getStringOrNull("sessionId")
        val targetSessions = findSessions(sessionId)
        if (targetSessions.isEmpty()) {
            logger.debug { "Received a message for unknown session $sessionId" }
            return
        }

        val parsedMessage = IncomingMessage.parse(message)
        logger.debug { "Received message ${parsedMessage.messageId} for session $sessionId: $parsedMessage" }
        for (session in targetSessions) {
            session.onIncomingMessage(parsedMessage)
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
        synchronized(sessions) {
            sessions.values.asSequence()
                .flatten()
                .forEach { it.onDisconnect() }
        }
    }

    companion object {

        private val logger = Logger.currentClass()

        // random string
        private val BROWSER_SESSION_KEY = "browser_" + Random.nextBytes(8).toHexString()

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
