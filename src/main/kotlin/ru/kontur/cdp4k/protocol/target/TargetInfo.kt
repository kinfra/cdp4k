package ru.kontur.cdp4k.protocol.target

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.util.getBoolean
import ru.kontur.cdp4k.util.getString
import ru.kontur.cdp4k.util.getStringOrNull
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.protocol.browser.BrowserContextId

class TargetInfo(
    val targetId: TargetId,
    val type: String,
    val title: String,
    val url: String,
    val attached: Boolean,
    val openerId: TargetId?,
    @CdpExperimental
    val browserContextId: BrowserContextId?
) {

    companion object {

        internal fun parse(tree: ObjectNode): TargetInfo {
            return TargetInfo(
                targetId = TargetId(tree.getString("targetId")),
                type = tree.getString("type"),
                title = tree.getString("title"),
                url = tree.getString("url"),
                attached = tree.getBoolean("attached"),
                openerId = tree.getStringOrNull("openerId")?.let { TargetId(it) },
                browserContextId = tree.getStringOrNull("browserContextId")?.let { BrowserContextId(it) }
            )
        }

    }

}
