package ru.kontur.cdp4k.launch

sealed class ChromeSwitch(val name: String) {

    init {
        require(name.matches(nameRegex)) { "Invalid switch name: $name" }
    }

    final override fun equals(other: Any?): Boolean {
        return other is ChromeSwitch && other.name == name
    }

    final override fun hashCode(): Int {
        return name.hashCode()
    }

    final override fun toString(): String {
        return "--$name (${javaClass.simpleName})"
    }

    class Binary(name: String) : ChromeSwitch(name) {

        val option = ChromeOption(this, "")

    }

    class SingleValue(name: String) : ChromeSwitch(name) {

        fun withValue(value: String): ChromeOption {
            require(value.isNotEmpty()) { "Empty value specified for $name" }
            return ChromeOption(this, value)
        }

    }

    class MultiValue(name: String) : ChromeSwitch(name) {

        fun withValue(firstValue: String, vararg otherValues: String): ChromeOption {
            @OptIn(ExperimentalStdlibApi::class)
            val allValues = buildList<String>(otherValues.size + 1) {
                add(firstValue)
                addAll(otherValues)
            }
            allValues.forEach {
                require(it.isNotEmpty()) { "Empty value specified for $name" }
                require(!it.contains(',')) { "Invalid value specified for $name: $it" }
            }
            return ChromeOption(this, allValues.joinToString(","))
        }

        internal fun plusValues(option: ChromeOption, vararg values: String): ChromeOption {
            require(option.switch == this) { "Cannot merge ${option.switch} with $this" }
            return withValue(option.value, *values)
        }

        internal fun getValues(option: ChromeOption): Collection<String> {
            require(option.switch == this) { "Invalid option: $option, expected $this" }
            return option.value.split(',')
        }

    }

    companion object {

        private val nameRegex = Regex("[a-z0-9]+([-][a-z0-9]+)*")

    }

}
