package ru.kontur.cdp4k.protocol.io

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.cdp4k.util.getBoolean
import ru.kontur.cdp4k.util.getBooleanOrNull
import ru.kontur.cdp4k.util.getString
import ru.kontur.cdp4k.util.jsonObject

/**
 * Input/Output operations for streams produced by DevTools.
 */
class IoDomain(session: RpcSession) : CdpDomain<Nothing>(session) {

    override val id: String
        get() = "IO"

    /**
     * Close the stream, discard any temporary backing storage.
     *
     * @param handle Handle of the stream to close.
     */
    suspend fun close(handle: StreamHandle) {
        val params = jsonObject().apply {
            put("handle", handle.value)
        }
        invoke("close", params)
    }

    /**
     * Read a chunk of the stream.
     *
     * @param handle Handle of the stream to read.
     * @param offset Seek to the specified offset before reading
     * (if not specified, proceed with offset following the last read).
     * Some types of streams may only support sequential reads.
     * @param size Maximum number of bytes to read (left upon the agent discretion if not specified).
     */
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

    /**
     * Result of invoking [IoDomain.read].
     *
     * @property base64Encoded Set if the data is base64-encoded
     * @property data Data that were read.
     * @property eof Set if the end-of-file condition occurred while reading.
     */
    class ReadResult(
        val base64Encoded: Boolean?,
        val data: String,
        val eof: Boolean
    ) {

        companion object {

            fun fromTree(tree: ObjectNode): ReadResult {
                return ReadResult(
                    base64Encoded = tree.getBooleanOrNull("base64Encoded"),
                    data = tree.getString("data"),
                    eof = tree.getBoolean("eof")
                )
            }

        }

    }

}
