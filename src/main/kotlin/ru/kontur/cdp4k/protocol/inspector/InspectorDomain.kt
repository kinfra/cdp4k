package ru.kontur.cdp4k.protocol.inspector

import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.rpc.RpcSession

@CdpExperimental
class InspectorDomain(session: RpcSession) : CdpDomain<InspectorEvent>(session) {

    override val id: String
        get() = "Inspector"

    /**
     * Disables inspector domain notifications.
     */
    suspend fun disable() {
        invoke("disable")
    }

    /**
     * Enables inspector domain notifications.
     */
    suspend fun enable() {
        invoke("enable")
    }

    override suspend fun enableEvents() {
        enable()
    }
}
