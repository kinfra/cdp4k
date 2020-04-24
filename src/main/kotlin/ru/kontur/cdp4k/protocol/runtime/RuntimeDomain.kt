package ru.kontur.cdp4k.protocol.runtime

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import ru.kontur.cdp4k.impl.getObject
import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.rpc.RpcSession

class RuntimeDomain(session: RpcSession) : CdpDomain<Nothing>(session) {

    override val id: String
        get() = "Runtime"

    suspend fun evaluate(
        expression: String,
        returnByValue: Boolean? = null,
        awaitPromise: Boolean? = null
    ): RemoteObject {

        val params = JsonNodeFactory.instance.objectNode().apply {
            put("expression", expression)
            if (returnByValue != null) put("returnByValue", returnByValue)
            if (awaitPromise != null) put("awaitPromise", awaitPromise)
        }
        return invoke("evaluate", params) { RemoteObject.fromTree(it.getObject("result")) }
    }

    suspend fun releaseObject(objectId: RemoteObjectId) {
        val params = JsonNodeFactory.instance.objectNode().apply {
            put("objectId", objectId.value)
        }
        invoke("releaseObject", params)
    }

}
