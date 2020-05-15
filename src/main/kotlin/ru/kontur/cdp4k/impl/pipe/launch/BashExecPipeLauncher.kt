package ru.kontur.cdp4k.impl.pipe.launch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object BashExecPipeLauncher : PipeLauncher {
    /*
     * Chrome uses file descriptors 3 and 4 for CDP I/O.
     * These descriptors are inaccessible from plain Java.
     * The only available are 0 (stdin), 1 (stdout), and 2 (stderr).
     * This class starts Chrome via a Bash command,
     * which redirects descriptors 3 and 4 to 0 and 1 respectively.
     */

    private val specialChars = listOf(' ', '\'', '"', '<', '>', '&', '\\')

    override suspend fun launchChrome(executable: String, args: List<String>): Process {
        @OptIn(ExperimentalStdlibApi::class)
        val bashCommand = buildList {
            add("exec")
            add(executable)
            for (arg in args) {
                add(quoteArg(arg))
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
