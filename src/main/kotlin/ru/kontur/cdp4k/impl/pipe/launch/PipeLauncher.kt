package ru.kontur.cdp4k.impl.pipe.launch

internal interface PipeLauncher {

    suspend fun launchChrome(executable: String, args: List<String>): Process

}
