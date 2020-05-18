package ru.kontur.cdp4k.launch

import java.nio.file.Path

class ChromeCommandLine private constructor(
    val command: String,
    val options: Collection<ChromeOption>
) {

    // todo: add retrieval methods for options?

    inline fun modify(block: Builder.() -> Unit): ChromeCommandLine {
        return toBuilder().apply(block).build()
    }

    @PublishedApi
    internal fun toBuilder(): Builder {
        return Builder(command, options)
    }

    class Builder internal constructor(
        internal val command: String,
        options: Collection<ChromeOption>
    ) {

        private val options = LinkedHashMap<ChromeSwitch, ChromeOption>().apply {
            options.forEach { put(it.switch, it) }
        }

        fun add(switch: ChromeSwitch.Binary) {
            set(switch, true)
        }

        operator fun set(switch: ChromeSwitch.Binary, value: Boolean) {
            if (value) {
                options[switch] = switch.option
            } else {
                options.remove(switch)
            }
        }

        operator fun set(switch: ChromeSwitch.SingleValue, value: String) {
            options[switch] = switch.withValue(value)
        }

        fun add(switch: ChromeSwitch.MultiValue, firstValue: String, vararg otherValues: String) {
            val current = options[switch]
            options[switch] = if (current == null) {
                switch.withValue(firstValue, *otherValues)
            } else {
                switch.plusValues(current, firstValue, *otherValues)
            }
        }

        fun remove(switch: ChromeSwitch) {
            require(switch != ChromeSwitches.userDataDir) { "$switch cannot be removed" }
        }

        @PublishedApi
        internal fun build(): ChromeCommandLine {
            return ChromeCommandLine(command, options.values.toList())
        }

    }

    companion object {

        inline fun build(command: String, dataDir: Path, block: Builder.() -> Unit): ChromeCommandLine {
            return createBuilder(command, dataDir).apply(block).build()
        }

        @PublishedApi
        internal fun createBuilder(command: String, dataDir: Path): Builder {
            require(command.isNotEmpty()) { "Empty command specified" }
            return Builder(command, emptyList()).apply {
                set(ChromeSwitches.userDataDir, dataDir.toAbsolutePath().toString())
            }
        }

    }

}
