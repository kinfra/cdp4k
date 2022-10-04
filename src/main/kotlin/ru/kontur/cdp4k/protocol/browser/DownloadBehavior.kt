package ru.kontur.cdp4k.protocol.browser

enum class DownloadBehavior(val value: String) {
    DENY("deny"),
    ALLOW("allow"),
    ALLOW_AND_NAME("allowAndName"),
    DEFAULT("default")
}
