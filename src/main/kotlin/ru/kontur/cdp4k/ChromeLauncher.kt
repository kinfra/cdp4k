package ru.kontur.cdp4k

import ru.kontur.cdp4k.connection.ChromeConnection

interface ChromeLauncher {

    // todo: replace flags list with Options object
    suspend fun launchChrome(executable: String, flags: List<String>): ChromeConnection

}
