package ru.kontur.cdp4k.launch.pipe

import ru.kontur.cdp4k.connection.ChromeConnection
import ru.kontur.cdp4k.connection.pipe.PipeChromeConnection
import ru.kontur.cdp4k.connection.pipe.stream.NullSeparatedJsonStreamCodec
import ru.kontur.cdp4k.launch.AbstractChromeLauncher
import ru.kontur.cdp4k.launch.ChromeCommandLine
import ru.kontur.cdp4k.launch.ChromeSwitches

object PipeChromeLauncher : AbstractChromeLauncher() {

    private val pipeLauncher = BashExecPipeLauncher
    private val streamCodec = NullSeparatedJsonStreamCodec

    override suspend fun launchChrome(commandLine: ChromeCommandLine): ChromeConnection {
        requireNoDebuggingOptionsSet(commandLine)

        val commandWithDebugging = commandLine.modify {
            set(ChromeSwitches.remoteDebuggingPipe, streamCodec.encoding)
        }

        val process = pipeLauncher.launchChrome(commandWithDebugging)
        return PipeChromeConnection.open(process, streamCodec)
    }

}
