package ru.kontur.cdp4k.protocol.target

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.protocol.browser.BrowserContextId
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.cdp4k.util.getArray
import ru.kontur.cdp4k.util.getBoolean
import ru.kontur.cdp4k.util.getString
import ru.kontur.cdp4k.util.jsonObject

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
     * Creates a new empty BrowserContext. Similar to an incognito profile but you can have more than one.
     */
    @CdpExperimental
    suspend fun createBrowserContext(disposeOnDetach: Boolean? = null): BrowserContextId {
        val params = jsonObject().apply {
            if (disposeOnDetach != null) put("disposeOnDetach", disposeOnDetach)
        }
        return invoke("createBrowserContext", params) { BrowserContextId(it.getString("browserContextId")) }
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

    /**
     * Deletes a BrowserContext. All the belonging pages will be closed without calling their beforeunload hooks.
     */
    @CdpExperimental
    suspend fun disposeBrowserContext(browserContextId: BrowserContextId) {
        val params = jsonObject().apply {
            put("browserContextId", browserContextId.value)
        }
        invoke("disposeBrowserContext", params)
    }

    /**
     * Returns all browser contexts created with `Target.createBrowserContext` method.
     */
    @CdpExperimental
    suspend fun getBrowserContexts(): List<BrowserContextId> {
        return invoke("getBrowserContexts") { result ->
            result.getArray("browserContextIds").elements().asSequence()
                .map { it as TextNode }
                .map { BrowserContextId(it.textValue()) }
                .toList()
        }
    }

    /**
     * Retrieves a list of available targets.
     */
    suspend fun getTargets(): List<TargetInfo> {
        return invoke("getTargets") { result ->
            result.getArray("targetInfos").elements().asSequence()
                .map { TargetInfo.parse(it as ObjectNode) }
                .toList()
        }
    }

    /**
     * Controls whether to discover available targets
     * and notify via `targetCreated`/`targetInfoChanged`/`targetDestroyed` events.
     */
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
