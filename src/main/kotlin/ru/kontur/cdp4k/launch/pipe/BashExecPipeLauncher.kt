package ru.kontur.cdp4k.launch.pipe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.kontur.cdp4k.launch.ChromeCommandLine

/*
 * Starts Chrome via a Bash command,
 * which redirects descriptors 3 and 4 to 0 and 1 respectively.
 *
 * This approach has a downside: any garbage in Chrome's stdout will break the communication.
 * Some launcher scripts for Chromium in Linux distributions print debug output there.
 */
internal object BashExecPipeLauncher : PipeLauncher {

    private val specialChars = listOf(' ', '\'', '"', '<', '>', '&', '\\')

    override suspend fun launchChrome(commandLine: ChromeCommandLine): Process {
        val bashCommand = buildList {
            add("exec")
            add(commandLine.command)
            for (option in commandLine.options) {
                add(quoteArg(option.toString()))
            }
            add("3<&0")
            add("4>&1")
        }

        val bashCommandLine = buildList {
            addAll(commandLine.prefix)
            add("bash")
            add("-c")
            add(bashCommand.joinToString(" "))
        }
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
