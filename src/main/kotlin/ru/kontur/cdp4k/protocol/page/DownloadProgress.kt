package ru.kontur.cdp4k.protocol.page

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.protocol.CdpEventCompanion
import ru.kontur.cdp4k.util.getString

class DownloadProgress(
    val state: DownloadState
) : PageEvent() {

    enum class DownloadState(val value: String) {
        IN_PROGRESS("inProgress"),
        COMPLETED("completed"),
        CANCELED("canceled");

        companion object {
            private val BY_VALUE = values().associateBy { it.value }

            fun of(value: String): DownloadState = BY_VALUE.getValue(value)
        }
    }

    companion object : CdpEventCompanion<DownloadProgress>("downloadProgress") {

        override fun parse(tree: ObjectNode): DownloadProgress {
            return DownloadProgress(
                state = tree.getString("state").let { DownloadState.of(it) }
            )
        }

    }

}
