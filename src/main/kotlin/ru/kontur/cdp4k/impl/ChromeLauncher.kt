package ru.kontur.cdp4k.impl

import ru.kontur.cdp4k.connection.ChromeConnection
import ru.kontur.cdp4k.impl.stream.NullSeparatedJsonStreamCodec

object ChromeLauncher {

    suspend fun launchWithPipe(chromeBinary: String, flags: List<String>): ChromeConnection {
        flags.forEach { require(it.startsWith("--")) { "Invalid flag: $it" } }

        val (encoding, codec) = "JSON" to NullSeparatedJsonStreamCodec

        @OptIn(ExperimentalStdlibApi::class)
        val chromeArguments = buildList {
            add("--remote-debugging-pipe=$encoding")
            addAll(flags)
        }

        val process = BashPipeRedirection.runWithRedirection(chromeBinary, chromeArguments)
        ProcessErrorLogger(process).startLogging()
        return PipeChromeConnection.open(process, codec)
    }

}
