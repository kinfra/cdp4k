package ru.kontur.cdp4k.protocol

import com.fasterxml.jackson.databind.node.ObjectNode

abstract class CdpEventCompanion<T : CdpEvent> internal constructor(
    internal val methodName: String
) {

    internal abstract fun parse(tree: ObjectNode): T

}
