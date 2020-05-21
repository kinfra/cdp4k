package ru.kontur.cdp4k.launch

abstract class AbstractChromeLauncher internal constructor() : ChromeLauncher {

    protected fun requireNoDebuggingOptionsSet(commandLine: ChromeCommandLine) {
        for (option in commandLine.options) {
            require(!option.name.startsWith("remote-debugging-")) { "Illegal option: $option" }
        }
    }

}
