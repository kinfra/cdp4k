package ru.kontur.cdp4k.connection.pipe.stream

import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.InputStream
import java.io.OutputStream

internal interface CdpMessageStream {

    fun readMessage(): ObjectNode?

    fun writeMessage(message: ObjectNode)

    interface Codec {

        val encoding: String

        fun createMessageStream(input: InputStream, output: OutputStream): CdpMessageStream

    }

}
