package ru.kontur.cdp4k.protocol.io

import kotlinx.coroutines.CancellationException
import ru.kontur.kinfra.io.InputByteStream
import ru.kontur.kinfra.io.OutputByteStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

class RemoteInputStream(
    private val handle: StreamHandle,
    private val ioDomain: IoDomain
) : InputByteStream {

    private var eof = false
    private var closed = false

    override suspend fun read(buffer: ByteBuffer): Boolean {
        check(!closed) { "Stream is closed" }

        val count = buffer.remaining()
        val data = readChunk(count)
        return if (data != null) {
            buffer.put(data)
            true
        } else {
            false
        }
    }

    private suspend fun readChunk(count: Int): ByteArray? {
        if (eof) return null

        val result = try {
            ioDomain.read(handle, size = count.toLong())
        } catch (e: Exception) {
            throw IOException("Failed to read from stream $handle", e)
        }

        eof = result.eof

        return if (eof && result.data.isEmpty()) {
            null
        } else {
            if (result.base64Encoded == true) {
                Base64.getDecoder().decode(result.data)
            } else {
                result.data.toByteArray()
            }
        }
    }

    override suspend fun transferTo(output: OutputByteStream): Long {
        check(!closed) { "Stream is closed" }

        var totalCount = 0L
        while (true) {
            val chunk = readChunk(TRANSFER_CHUNK_SIZE) ?: break
            totalCount += chunk.size
            output.put(ByteBuffer.wrap(chunk))
        }
        return totalCount
    }

    override suspend fun close() {
        if (closed) return
        closed = true

        try {
            // This call may fail when the current coroutine is cancelled
            // and connection's outgoing buffer is full.
            // It is ok if the current session is closing,
            // otherwise it may lead to a leak on the Chrome side.
            // todo: prevent stream leak on cancellation
            ioDomain.close(handle)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Failed to close stream $handle", e)
        }
    }

    companion object {

        /*
         * Chromium's default is 10M, which seems too large
         */
        private const val TRANSFER_CHUNK_SIZE = 32 * 1024

    }

}
