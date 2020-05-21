package ru.kontur.cdp4k.launch

import java.util.*

class ChromeOption internal constructor(
    internal val switch: ChromeSwitch,
    internal val value: String
) {

    val name: String
        get() = switch.name

    override fun equals(other: Any?): Boolean {
        return other is ChromeOption
            && other.switch == switch
            && other.value == value
    }

    override fun hashCode(): Int {
        return Objects.hash(switch, value)
    }

    override fun toString(): String {
        return if (value.isEmpty()) {
            "--$name"
        } else {
            "--$name=$value"
        }
    }

}
