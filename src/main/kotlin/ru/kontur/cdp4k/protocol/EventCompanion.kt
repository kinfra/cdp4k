package ru.kontur.cdp4k.protocol

import com.fasterxml.jackson.databind.node.ObjectNode

abstract class EventCompanion<T : CdpEvent>(
    val methodName: String
) {

    abstract fun parse(tree: ObjectNode): T

}
