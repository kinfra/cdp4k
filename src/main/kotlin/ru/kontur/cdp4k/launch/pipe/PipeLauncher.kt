package ru.kontur.cdp4k.launch.pipe

import ru.kontur.cdp4k.launch.ChromeCommandLine

internal interface PipeLauncher {

    suspend fun launchChrome(commandLine: ChromeCommandLine): Process

}
