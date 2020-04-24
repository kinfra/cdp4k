package ru.kontur.cdp4k.protocol.inspector

import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.rpc.RpcSession

@CdpExperimental
class InspectorDomain(session: RpcSession) : CdpDomain<InspectorEvent>(session) {

    override val id: String
        get() = "Inspector"

    suspend fun enable() {

    }

    suspend fun disable() {

    }

}
