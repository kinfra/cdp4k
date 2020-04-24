package ru.kontur.cdp4k.protocol.browser

import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.rpc.RpcSession

class BrowserDomain(session: RpcSession) : CdpDomain<Nothing>(session) {

    override val id: String
        get() = "Browser"

    suspend fun getVersion(): BrowserVersion {
        return invoke("getVersion") { BrowserVersion.fromTree(it) }
    }

    suspend fun close() {
        invoke("close")
    }

    @CdpExperimental
    suspend fun crash() {
        invoke("crash")
    }

    @CdpExperimental
    suspend fun crashGpuProcess() {
        invoke("crashGpuProcess")
    }

}
