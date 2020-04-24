package ru.kontur.cdp4k.protocol.network

class MonotonicTime(val value: Double) : Comparable<MonotonicTime> {

    init {
        require(value.isFinite()) { "Value must be a finite number: $value" }
    }

    override fun compareTo(other: MonotonicTime): Int {
        return value.compareTo(other.value)
    }

    override fun equals(other: Any?): Boolean {
        return other is MonotonicTime && other.value == value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "MonotonicTime($value)"
    }
}
