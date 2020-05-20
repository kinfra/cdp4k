package ru.kontur.cdp4k.protocol.target

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.protocol.CdpEventCompanion
import ru.kontur.cdp4k.util.getObject

/**
 * Issued when a possible inspection target is created.
 */
class TargetCreated(
    val targetInfo: TargetInfo
) : TargetEvent() {

    companion object : CdpEventCompanion<TargetCreated>("targetCreated") {

        override fun parse(tree: ObjectNode): TargetCreated {
            return TargetCreated(
                targetInfo = TargetInfo.parse(tree.getObject("targetInfo"))
            )
        }

    }

}
