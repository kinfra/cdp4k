package ru.kontur.cdp4k.launch

import java.nio.file.Path
import java.util.*
import kotlin.collections.LinkedHashMap

class ChromeCommandLine private constructor(
    val command: String,
    private val _options: Map<ChromeSwitch, ChromeOption>
) {

    val options: Collection<ChromeOption>
        get() = _options.values

    operator fun get(switch: ChromeSwitch.Binary): Boolean {
        return _options.containsKey(switch)
    }

    operator fun get(switch: ChromeSwitch.SingleValue): String? {
        return _options[switch]?.value
    }

    operator fun get(switch: ChromeSwitch.MultiValue): Collection<String> {
        return _options[switch]?.let { switch.getValues(it) } ?: emptyList()
    }

    inline fun modify(block: Builder.() -> Unit): ChromeCommandLine {
        return toBuilder().apply(block).build()
    }

    @PublishedApi
    internal fun toBuilder(): Builder {
        return Builder(command, options)
    }

    override fun equals(other: Any?): Boolean {
        return other is ChromeCommandLine
            && other.command == command
            && other._options == _options
    }

    override fun hashCode(): Int {
        return Objects.hash(command, _options)
    }

    override fun toString(): String {
        return "$command $options"
    }

    class Builder internal constructor(
        internal val command: String,
        options: Collection<ChromeOption>
    ) {

        private val options = LinkedHashMap<ChromeSwitch, ChromeOption>(options.size).also { map ->
            options.associateByTo(map) { it.switch }
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
            return ChromeCommandLine(command, options.toMap())
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
