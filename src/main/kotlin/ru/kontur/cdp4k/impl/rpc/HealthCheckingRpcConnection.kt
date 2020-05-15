package ru.kontur.cdp4k.impl.rpc

import kotlinx.coroutines.*
import kotlinx.coroutines.time.delay
import ru.kontur.cdp4k.protocol.browser.BrowserDomain
import ru.kontur.cdp4k.protocol.inspector.DetachedEvent
import ru.kontur.cdp4k.protocol.inspector.InspectorDomain
import ru.kontur.cdp4k.protocol.inspector.TargetCrashed
import ru.kontur.cdp4k.protocol.waitFor
import ru.kontur.cdp4k.rpc.RpcConnection
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.cdp4k.rpc.SessionUsage
import ru.kontur.kinfra.commons.time.withDeadlineAfter
import java.time.Duration

private class HealthCheckFailedException(message: String, cause: Exception? = null) : RuntimeException(message, cause)

class HealthCheckingRpcConnection(
    private val delegate: RpcConnection,
    private val period: Duration = DEFAULT_PERIOD,
    private val timeout: Duration = DEFAULT_TIMEOUT
) : RpcConnection {

    override suspend fun <R> useBrowserSession(block: SessionUsage<R>): R {
        return delegate.useBrowserSession(withHealthChecks(true, block))
    }

    override suspend fun <R> useSession(id: String, block: SessionUsage<R>): R {
        return delegate.useSession(id, withHealthChecks(false, block))
    }

    private fun <R> withHealthChecks(isBrowser: Boolean, block: SessionUsage<R>): SessionUsage<R> {
        return { session ->
            val job = launchHealthChecks(session, isBrowser)
            try {
                coroutineScope {
                    block(session)
                }
            } finally {
                job.cancel()
            }
        }
    }

    private fun CoroutineScope.launchHealthChecks(session: RpcSession, isBrowser: Boolean) = launch {
        if (isBrowser) {
            launchBrowserCheck(session)
        } else {
            launchInspectorEventsCheck(session)
        }
    }

    private fun CoroutineScope.launchBrowserCheck(session: RpcSession) = launch {
        val browserDomain = BrowserDomain(session)
        while (true) {
            while (isActive) {
                delay(period)

                try {
                    withDeadlineAfter(timeout) {
                        browserDomain.getVersion()
                    }
                } catch (e: Exception) {
                    if (e is CancellationException && e !is TimeoutCancellationException) {
                        throw e
                    } else {
                        throw HealthCheckFailedException("Health check of browser session is failed", e)
                    }
                }
            }
        }
    }

    private fun CoroutineScope.launchInspectorEventsCheck(session: RpcSession) {
        val inspectorDomain = InspectorDomain(session)
        launch {
            inspectorDomain.waitFor(TargetCrashed)
            throw HealthCheckFailedException("Target is crashed")
        }
        launch {
            val event = inspectorDomain.waitFor(DetachedEvent)
            throw HealthCheckFailedException("Detached from target: ${event.reason}")
        }
    }

    override suspend fun close() {
        delegate.close()
    }

    companion object {

        private val DEFAULT_PERIOD = Duration.ofSeconds(5)
        private val DEFAULT_TIMEOUT = Duration.ofSeconds(3)

    }

}
