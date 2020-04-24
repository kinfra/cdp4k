package ru.kontur.cdp4k.impl.stream

import com.fasterxml.jackson.databind.node.ObjectNode

internal interface CdpMessageStream {

    fun readMessage(): ObjectNode?

    fun writeMessage(message: ObjectNode)

}
