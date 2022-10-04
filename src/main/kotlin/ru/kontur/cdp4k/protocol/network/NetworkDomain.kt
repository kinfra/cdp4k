package ru.kontur.cdp4k.protocol.network

import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.protocol.page.PageDomain
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.cdp4k.util.getBoolean
import ru.kontur.cdp4k.util.jsonObject
import java.nio.file.Path

/**
 * Network domain allows tracking network activities of the page. It exposes information about http, file, data and other requests and responses, their headers, bodies, timing, etc.
 */
class NetworkDomain(session: RpcSession) : CdpDomain<Nothing>(session) {

    override val id: String
        get() = "Network"

    /**
     * Close network gracefully.
     */
    suspend fun close() {
        invoke("close")
    }

    suspend fun setCookie(
        name: String,
        value: String,
        domain: String? = null,
        path: String? = null,
        secure: Boolean? = null,
        httpOnly: Boolean? = null
    ): Boolean {
        val params = jsonObject().apply {
            put("name", name)
            put("value", value)
            if (domain != null) {
                put("domain", domain)
            }
            if (path != null) {
                put("path", path)
            }
            if (secure != null) {
                put("secure", secure)
            }
            if (httpOnly != null) {
                put("httpOnly", httpOnly)
            }
        }
        return invoke("setCookie", params) { it.getBoolean("success") }
    }

}
