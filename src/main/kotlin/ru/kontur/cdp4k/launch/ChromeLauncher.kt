package ru.kontur.cdp4k.launch

import ru.kontur.cdp4k.connection.ChromeConnection

interface ChromeLauncher {

    suspend fun launchChrome(commandLine: ChromeCommandLine): ChromeConnection

}
