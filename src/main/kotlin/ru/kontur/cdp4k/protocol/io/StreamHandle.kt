package ru.kontur.cdp4k.protocol.io

import ru.kontur.cdp4k.protocol.StringIdentifier
import java.util.*

class StreamHandle(value: String) : StringIdentifier(value) {

    companion object {

        fun forBlob(blobId: UUID): StreamHandle {
            return StreamHandle("blob:$blobId")
        }

    }

}
