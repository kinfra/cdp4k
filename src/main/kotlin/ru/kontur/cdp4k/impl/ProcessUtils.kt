package ru.kontur.cdp4k.impl

import kotlinx.coroutines.future.await
import ru.kontur.jinfra.logging.Logger

private val logger = Logger.currentClass()

internal suspend fun ProcessHandle.kill() {
    val success = destroyForcibly()
    if (!success) {
        logger.warn { "Failed to kill PID ${pid()} (isAlive: $isAlive)" }
    }
    onExit().await()
}
