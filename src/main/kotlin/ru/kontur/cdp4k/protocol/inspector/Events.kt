package ru.kontur.cdp4k.protocol.inspector

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.util.getString
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.protocol.CdpEventCompanion

@CdpExperimental
class DetachedEvent(
    val reason: String
) : InspectorEvent() {

    companion object : CdpEventCompanion<DetachedEvent>("detached") {

        override fun parse(tree: ObjectNode): DetachedEvent {
            return DetachedEvent(
                reason = tree.getString("reason")
            )
        }

    }

}

@CdpExperimental
class TargetCrashed : InspectorEvent() {

    companion object : CdpEventCompanion<TargetCrashed>("targetCrashed") {

        override fun parse(tree: ObjectNode): TargetCrashed {
            return TargetCrashed()
        }

    }

}

@CdpExperimental
class TargetReloadedAfterCrash : InspectorEvent() {

    companion object : CdpEventCompanion<TargetReloadedAfterCrash>("targetReloadedAfterCrash") {

        override fun parse(tree: ObjectNode): TargetReloadedAfterCrash {
            return TargetReloadedAfterCrash()
        }

    }

}
