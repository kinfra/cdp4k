package ru.kontur.cdp4k.protocol.page

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.util.getDouble
import ru.kontur.cdp4k.protocol.CdpEventCompanion
import ru.kontur.cdp4k.protocol.network.MonotonicTime

class LoadEventFired(
    val timestamp: MonotonicTime
) : PageEvent() {

    companion object : CdpEventCompanion<LoadEventFired>("loadEventFired") {

        override fun parse(tree: ObjectNode): LoadEventFired {
            return LoadEventFired(
                timestamp = MonotonicTime(tree.getDouble("timestamp"))
            )
        }

    }

}
