package ru.kontur.cdp4k.launch.pipe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.kontur.cdp4k.launch.ChromeCommandLine

internal object BashExecPipeLauncher : PipeLauncher {
    /*
     * Chrome uses file descriptors 3 and 4 for CDP I/O.
     * These descriptors are inaccessible from plain Java.
     * The only available are 0 (stdin), 1 (stdout), and 2 (stderr).
     * This class starts Chrome via a Bash command,
     * which redirects descriptors 3 and 4 to 0 and 1 respectively.
     *
     * This approach have a downside: any garbage in Chrome's stdout will break the communication.
     * Some launcher scripts for Chromium in Linux distributions print debug output there.
     */

    private val specialChars = listOf(' ', '\'', '"', '<', '>', '&', '\\')

    override suspend fun launchChrome(commandLine: ChromeCommandLine): Process {
        @OptIn(ExperimentalStdlibApi::class)
        val bashCommand = buildList {
            add("exec")
            add(commandLine.command)
            for (option in commandLine.options) {
                add(quoteArg(option.toString()))
            }
            add("3<&0")
            add("4>&1")
        }

        val bashCommandLine = listOf("bash", "-c", bashCommand.joinToString(" "))
        return withContext(Dispatchers.IO) {
            ProcessBuilder(bashCommandLine).start()
        }
    }

    private fun quoteArg(input: String): String {
        return if (input.any { it in specialChars }) {
            val escaped = input
                .replace("\\", "\\\\")
                .replace("'", "\\\'")
            "'$escaped'"
        } else {
            input
        }
    }

}
