package ru.kontur.cdp4k.launch.pipe

import ru.kontur.cdp4k.launch.ChromeLauncher
import ru.kontur.cdp4k.connection.ChromeConnection
import ru.kontur.cdp4k.connection.pipe.PipeChromeConnection
import ru.kontur.cdp4k.util.ProcessErrorLogger
import ru.kontur.cdp4k.connection.pipe.stream.NullSeparatedJsonStreamCodec
import ru.kontur.cdp4k.launch.ChromeCommandLine
import ru.kontur.cdp4k.launch.ChromeSwitches

object PipeChromeLauncher : ChromeLauncher {

    private val pipeLauncher = BashExecPipeLauncher
    private val streamCodec = NullSeparatedJsonStreamCodec

    override suspend fun launchChrome(commandLine: ChromeCommandLine): ChromeConnection {
        commandLine.options.forEach {
            require(!it.name.startsWith("remote-debugging-")) { "Illegal option: $it" }
        }

        val commandWithDebugging = commandLine.modify {
            set(ChromeSwitches.remoteDebuggingPipe, streamCodec.encoding)
        }

        val process = pipeLauncher.launchChrome(commandWithDebugging)
        ProcessErrorLogger(process).startLogging()
        return PipeChromeConnection.open(process,
            streamCodec
        )
    }

}
