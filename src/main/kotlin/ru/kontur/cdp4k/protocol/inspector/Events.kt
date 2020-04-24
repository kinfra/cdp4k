package ru.kontur.cdp4k.protocol.inspector

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.impl.getString
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.protocol.EventCompanion

@CdpExperimental
class DetachedEvent(
    val reason: String
) : InspectorEvent() {

    companion object : EventCompanion<DetachedEvent>("detached") {

        override fun parse(tree: ObjectNode): DetachedEvent {
            return DetachedEvent(
                reason = tree.getString("reason")
            )
        }

    }

}

@CdpExperimental
class TargetCrashed : InspectorEvent() {

    companion object : EventCompanion<TargetCrashed>("targetCrashed") {

        override fun parse(tree: ObjectNode): TargetCrashed {
            return TargetCrashed()
        }

    }

}

@CdpExperimental
class TargetReloadedAfterCrash : InspectorEvent() {

    companion object : EventCompanion<TargetReloadedAfterCrash>("targetReloadedAfterCrash") {

        override fun parse(tree: ObjectNode): TargetReloadedAfterCrash {
            return TargetReloadedAfterCrash()
        }

    }

}
