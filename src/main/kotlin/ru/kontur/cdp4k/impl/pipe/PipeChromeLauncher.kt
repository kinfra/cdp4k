package ru.kontur.cdp4k.impl.pipe

import ru.kontur.cdp4k.ChromeLauncher
import ru.kontur.cdp4k.connection.ChromeConnection
import ru.kontur.cdp4k.impl.ProcessErrorLogger
import ru.kontur.cdp4k.impl.pipe.launch.BashExecPipeLauncher
import ru.kontur.cdp4k.impl.pipe.stream.NullSeparatedJsonStreamCodec

object PipeChromeLauncher : ChromeLauncher {

    private val pipeLauncher = BashExecPipeLauncher
    private val streamCodec = NullSeparatedJsonStreamCodec

    override suspend fun launchChrome(executable: String, flags: List<String>): ChromeConnection {
        flags.forEach {
            require(it.startsWith("--")) { "Invalid flag: $it" }
            require(!it.startsWith("--remote-debugging-")) { "Remote debugging flags are prohibited: $it" }
        }

        @OptIn(ExperimentalStdlibApi::class)
        val chromeArguments = buildList {
            add("--remote-debugging-pipe=${streamCodec.encoding}")
            addAll(flags)
        }

        val process = pipeLauncher.launchChrome(executable, chromeArguments)
        ProcessErrorLogger(process).startLogging()
        return PipeChromeConnection.open(process, streamCodec)
    }

}
