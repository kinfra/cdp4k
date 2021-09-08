package ru.kontur.cdp4k.util

import kotlinx.coroutines.future.await
import ru.kontur.kinfra.logging.Logger

private val logger = Logger.currentClass()

internal suspend fun ProcessHandle.kill() {
    val success = destroyForcibly()
    if (!success) {
        logger.warn { "Failed to kill PID ${pid()} (isAlive: $isAlive)" }
    }
    onExit().await()
}
