package ru.kontur.cdp4k.impl.stream

import java.io.InputStream
import java.io.OutputStream

internal interface CdpStreamCodec {

    fun createMessageStream(input: InputStream, output: OutputStream): CdpMessageStream

}
