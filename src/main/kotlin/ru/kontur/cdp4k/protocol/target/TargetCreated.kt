package ru.kontur.cdp4k.protocol.target

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.impl.getObject
import ru.kontur.cdp4k.protocol.EventCompanion

class TargetCreated(
    val targetInfo: TargetInfo
) : TargetEvent() {

    companion object : EventCompanion<TargetCreated>("targetCreated") {

        override fun parse(tree: ObjectNode): TargetCreated {
            return TargetCreated(
                targetInfo = TargetInfo.parse(tree.getObject("targetInfo"))
            )
        }

    }

}
