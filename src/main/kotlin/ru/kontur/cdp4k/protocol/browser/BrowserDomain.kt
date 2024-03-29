package ru.kontur.cdp4k.protocol.browser

import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.cdp4k.util.jsonObject
import java.nio.file.Path

/**
 * The Browser domain defines methods and events for browser managing.
 */
class BrowserDomain(session: RpcSession) : CdpDomain<Nothing>(session) {

    override val id: String
        get() = "Browser"

    /**
     * Close browser gracefully.
     */
    suspend fun close() {
        invoke("close")
    }

    /**
     * Crashes browser on the main thread.
     */
    @CdpExperimental
    suspend fun crash() {
        invoke("crash")
    }

    /**
     * Crashes GPU process.
     */
    @CdpExperimental
    suspend fun crashGpuProcess() {
        invoke("crashGpuProcess")
    }

    /**
     * Returns version information.
     */
    suspend fun getVersion(): BrowserVersion {
        return invoke("getVersion") { BrowserVersion.fromTree(it) }
    }

    /**
     * Set the behavior when downloading a file.
     */
    @CdpExperimental
    suspend fun setDownloadBehavior(
        behavior: DownloadBehavior,
        downloadPath: Path? = null,
        eventsEnabled: Boolean? = null
    ) {
        val params = jsonObject().apply {
            put("behavior", behavior.value)
            if (downloadPath != null) {
                put("downloadPath", downloadPath.toString())
            }
            if (eventsEnabled != null) {
                put("eventsEnabled", eventsEnabled)
            }
        }
        return invoke("setDownloadBehavior", params)
    }
}
