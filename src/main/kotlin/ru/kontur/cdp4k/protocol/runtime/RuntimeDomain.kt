package ru.kontur.cdp4k.protocol.runtime

import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.cdp4k.util.getObject
import ru.kontur.cdp4k.util.jsonObject

/**
 * Runtime domain exposes JavaScript runtime by means of remote evaluation and mirror objects.
 * Evaluation results are returned as mirror object that expose object type,
 * string representation and unique identifier that can be used for further object reference.
 * Original objects are maintained in memory unless they are either explicitly released
 * or are released along with the other objects in their object group.
 */
class RuntimeDomain(session: RpcSession) : CdpDomain<Nothing>(session) {

    override val id: String
        get() = "Runtime"

    /**
     * Evaluates expression on global object.
     *
     * @param expression Expression to evaluate.
     * @param returnByValue Whether the result is expected to be a JSON object that should be sent by value.
     * @param awaitPromise Whether execution should await for resulting value
     * and return once awaited promise is resolved.
     */
    suspend fun evaluate(
        expression: String,
        returnByValue: Boolean? = null,
        awaitPromise: Boolean? = null
    ): RemoteObject {

        val params = jsonObject().apply {
            put("expression", expression)
            if (returnByValue != null) put("returnByValue", returnByValue)
            if (awaitPromise != null) put("awaitPromise", awaitPromise)
        }
        return invoke("evaluate", params) { RemoteObject.fromTree(it.getObject("result")) }
    }

    /**
     * Releases remote object with given id.
     *
     * @param objectId Identifier of the object to release.
     */
    suspend fun releaseObject(objectId: RemoteObjectId) {
        val params = jsonObject().apply {
            put("objectId", objectId.value)
        }
        invoke("releaseObject", params)
    }

}
