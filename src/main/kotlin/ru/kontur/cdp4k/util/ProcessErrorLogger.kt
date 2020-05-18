package ru.kontur.cdp4k.util

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.kontur.jinfra.logging.Logger
import java.io.IOException

internal class ProcessErrorLogger(
    private val process: Process
) {

    private val logger = Logger.currentClass().withoutContext()
        .addContext("pid", process.pid())

    fun startLogging() {
        val stderr = process.errorStream
        GlobalScope.launch(CoroutineName("stderr logger (pid: ${process.pid()})") + Dispatchers.IO) {
            try {
                val reader = stderr.bufferedReader()
                while (true) {
                    val line = reader.readLine() ?: break
                    logger.debug { "stderr: $line" }
                }
                logger.debug { "End of stderr" }
            } catch (e: IOException) {
                logger.error(e) { "Failed to read stderr" }
            }
        }
    }

}
