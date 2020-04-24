package ru.kontur.cdp4k.connection

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.kinfra.io.SuspendingCloseable

interface ChromeConnection : SuspendingCloseable {

    suspend fun send(message: ObjectNode)

    fun subscribe(subscriber: Subscriber)

    interface Subscriber {

        fun onIncomingMessage(message: ObjectNode)

        fun onConnectionClosed()

    }

}
