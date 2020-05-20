package ru.kontur.cdp4k.protocol.inspector

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.protocol.CdpEventCompanion
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.util.getString

/**
 * Fired when remote debugging connection is about to be terminated. Contains detach reason.
 */
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

/**
 * Fired when debugging target has crashed.
 */
@CdpExperimental
class TargetCrashed : InspectorEvent() {

    companion object : CdpEventCompanion<TargetCrashed>("targetCrashed") {

        override fun parse(tree: ObjectNode): TargetCrashed {
            return TargetCrashed()
        }

    }

}

/**
 * Fired when debugging target has reloaded after crash.
 */
@CdpExperimental
class TargetReloadedAfterCrash : InspectorEvent() {

    companion object : CdpEventCompanion<TargetReloadedAfterCrash>("targetReloadedAfterCrash") {

        override fun parse(tree: ObjectNode): TargetReloadedAfterCrash {
            return TargetReloadedAfterCrash()
        }

    }

}
