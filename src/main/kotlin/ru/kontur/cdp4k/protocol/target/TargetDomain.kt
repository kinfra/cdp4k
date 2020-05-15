package ru.kontur.cdp4k.protocol.target

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.impl.getArray
import ru.kontur.cdp4k.impl.getBoolean
import ru.kontur.cdp4k.impl.getString
import ru.kontur.cdp4k.impl.jsonObject
import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.protocol.browser.BrowserContextId
import ru.kontur.cdp4k.rpc.RpcSession

class TargetDomain(session: RpcSession) : CdpDomain<TargetEvent>(session) {

    override val id: String
        get() = "Target"

    /**
     * Attaches to the target with given id.
     */
    suspend fun attachToTarget(targetId: TargetId): SessionId {
        val params = jsonObject().apply {
            put("targetId", targetId.value)
            put("flatten", true)
        }
        return invoke("attachToTarget", params) { SessionId(it.getString("sessionId")) }
    }

    /**
     * Closes the target. If the target is a page that gets closed too.
     */
    suspend fun closeTarget(targetId: TargetId): CloseTargetResult {
        val params = jsonObject().apply {
            put("targetId", targetId.value)
        }
        return invoke("closeTarget", params, ::CloseTargetResult)
    }

    /**
     * Creates a new page.
     */
    suspend fun createTarget(
        url: String,
        width: Int? = null,
        height: Int? = null,
        browserContextId: BrowserContextId? = null
    ): CreateTargetResult {

        val params = jsonObject().apply {
            put("url", url)
            if (width != null) put("width", width)
            if (height != null) put("height", height)
            if (browserContextId != null) put("browserContextId", browserContextId.value)
        }
        return invoke("createTarget", params) { CreateTargetResult.fromTree(it) }
    }

    /**
     * Detaches session with given id.
     */
    suspend fun detachFromTarget(sessionId: SessionId) {
        val params = jsonObject().apply {
            put("sessionId", sessionId.value)
        }
        return invoke("detachFromTarget", params)
    }

    suspend fun getTargets(): List<TargetInfo> {
        return invoke("getTargets") { result ->
            result.getArray("targetInfos").elements().asSequence()
                .map { TargetInfo.parse(it as ObjectNode) }
                .toList()
        }
    }

    suspend fun setDiscoverTargets(discover: Boolean) {
        val params = jsonObject().apply {
            put("discover", discover)
        }
        invoke("setDiscoverTargets", params)
    }

    override suspend fun enableEvents() {
        setDiscoverTargets(true)
    }

    class CreateTargetResult(
        val targetId: TargetId
    ) {

        companion object {

            fun fromTree(tree: ObjectNode): CreateTargetResult {
                return CreateTargetResult(
                    targetId = TargetId(tree.getString("targetId"))
                )
            }

        }

    }

    class CloseTargetResult(
        val success: Boolean
    ) {

        constructor(tree: ObjectNode) : this(
            success = tree.getBoolean("success")
        )

    }

}
