package ru.kontur.cdp4k.launch

class ChromeOption internal constructor(
    internal val switch: ChromeSwitch,
    internal val value: String
) {

    val name: String
        get() = switch.name

    override fun toString(): String {
        return if (value.isEmpty()) {
            "--$name"
        } else {
            "--$name=$value"
        }
    }

}
