package ru.kontur.cdp4k.protocol.io

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.impl.getBoolean
import ru.kontur.cdp4k.impl.getBooleanOrNull
import ru.kontur.cdp4k.impl.getString
import ru.kontur.cdp4k.impl.jsonObject
import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.rpc.RpcSession

class IoDomain(session: RpcSession) : CdpDomain<Nothing>(session) {

    override val id: String
        get() = "IO"

    suspend fun close(handle: StreamHandle) {
        val params = jsonObject().apply {
            put("handle", handle.value)
        }
        invoke("close", params)
    }

    suspend fun read(
        handle: StreamHandle,
        offset: Long? = null,
        size: Long? = null
    ): ReadResult {

        val params = jsonObject().apply {
            put("handle", handle.value)
            if (offset != null) put("offset", offset)
            if (size != null) put("size", size)
        }
        return invoke("read", params) { ReadResult.fromTree(it) }
    }

    class ReadResult(
        val data: String,
        val eof: Boolean,
        val base64Encoded: Boolean?
    ) {

        companion object {

            fun fromTree(tree: ObjectNode): ReadResult {
                return ReadResult(
                    data = tree.getString("data"),
                    eof = tree.getBoolean("eof"),
                    base64Encoded = tree.getBooleanOrNull("base64Encoded")
                )
            }

        }

    }

}
