package ru.kontur.cdp4k.protocol

abstract class StringIdentifier(val value: String) {

    final override fun equals(other: Any?): Boolean {
        return other is StringIdentifier &&
            other.javaClass == this.javaClass &&
            other.value == this.value
    }

    final override fun hashCode(): Int {
        return value.hashCode()
    }

    final override fun toString(): String {
        return value
    }

}
