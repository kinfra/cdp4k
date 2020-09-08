package ru.kontur.cdp4k.connection.pipe.stream

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.CharArrayReader
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

internal object NullSeparatedJsonStreamCodec : CdpMessageStream.Codec {

    private val jsonFactory = ObjectMapper().factory

    override val encoding: String
        get() = "JSON"

    override fun createMessageStream(input: InputStream, output: OutputStream): CdpMessageStream {
        val messageReader = StreamReader(input)
        val charDecoder = StandardCharsets.UTF_8.newDecoder().apply {
            onMalformedInput(CodingErrorAction.REPORT)
            onUnmappableCharacter(CodingErrorAction.REPORT)
        }
        val generator = jsonFactory.createGenerator(output).apply {
            disable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)
            disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
        }

        return object : CdpMessageStream {
            override fun readMessage(): ObjectNode? {
                val bytes = messageReader.readNext()
                if (bytes.remaining() == 0) return null

                val chars = charDecoder.decode(bytes)
                val charsReader = with(chars) { CharArrayReader(array(), arrayOffset() + position(), remaining()) }
                val parser = jsonFactory.createParser(charsReader)
                return try {
                    parser.readValueAsTree()
                } catch (e: Exception) {
                    throw IllegalArgumentException("Malformed message: $chars", e)
                }
            }

            override fun writeMessage(message: ObjectNode) {
                generator.writeTree(message)
                output.write(0)
                output.flush()
            }
        }
    }

    private class StreamReader(private val input: InputStream) {

        private val chunk = ByteBuffer.allocate(8192).apply { flip() }
        private var builder = BufferBuilder()

        fun readNext(): ByteBuffer {
            while (true) {
                while (chunk.remaining() == 0) {
                    // read some data
                    chunk.clear()
                    try {
                        val eof = input.read(chunk) == -1
                        if (eof) {
                            return builder.build()
                        }
                    } finally {
                        chunk.flip()
                    }
                }

                val nullIndex = chunk.findNull()
                if (nullIndex == -1) {
                    builder.put(chunk)
                } else {
                    chunk.withLimit(nullIndex) {
                        builder.put(it)
                    }
                    // skip the null character
                    chunk.position(nullIndex + 1)
                    return builder.build().also {
                        builder = BufferBuilder()
                    }
                }
            }
        }

        private fun InputStream.read(buffer: ByteBuffer): Int {
            val count = read(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining())
            if (count > 0) {
                buffer.position(buffer.position() + count)
            }
            return count
        }

        private fun ByteBuffer.findNull(): Int {
            for (index in position() until limit()) {
                if (get(index) == 0.toByte()) return index
            }
            return -1
        }

        // todo: move to kinfra-io
        private inline fun <R> ByteBuffer.withLimit(limit: Int, block: (ByteBuffer) -> R): R {
            val oldLimit = limit()
            return try {
                limit(limit)
                block(this)
            } finally {
                limit(oldLimit)
            }
        }

    }

    private class BufferBuilder {

        private var data = ByteArray(1024)
        private var index = 0

        fun put(src: ByteBuffer) {
            val count = src.remaining()
            if (data.size < index + count) {
                val newSize = maxOf(index + count, data.size * 3 / 2)
                data = data.copyOf(newSize)
            }
            src.get(data, index, count)
            index += count
        }

        fun build(): ByteBuffer {
            return ByteBuffer.wrap(data, 0, index)
        }

    }

}
