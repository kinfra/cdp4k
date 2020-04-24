package ru.kontur.cdp4k.impl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*

internal val EMPTY_TREE = JsonNodeFactory.instance.objectNode()

// todo: eliminate duplicate code

fun ObjectNode.getString(fieldName: String): String {
    return getStringOrNull(fieldName) ?: throwMissingField(fieldName)
}

fun ObjectNode.getStringOrNull(fieldName: String): String? {
    val node = get(fieldName) ?: return null
    return node.textValue() ?: throwInvalidType(fieldName, "a string", node)
}

fun ObjectNode.getObject(fieldName: String): ObjectNode {
    return getObjectOrNull(fieldName) ?: throwMissingField(fieldName)
}

fun ObjectNode.getObjectOrNull(fieldName: String): ObjectNode? {
    val node = get(fieldName) ?: return null
    return node as? ObjectNode ?: throwInvalidType(fieldName, "an object", node)
}

fun ObjectNode.getArray(fieldName: String): ArrayNode {
    return getArrayOrNull(fieldName) ?: throwMissingField(fieldName)
}

fun ObjectNode.getArrayOrNull(fieldName: String): ArrayNode? {
    val node = get(fieldName) ?: return null
    return node as? ArrayNode ?: throwInvalidType(fieldName, "an array", node)
}

fun ObjectNode.getBoolean(fieldName: String): Boolean {
    return getBooleanOrNull(fieldName) ?: throwMissingField(fieldName)
}

fun ObjectNode.getBooleanOrNull(fieldName: String): Boolean? {
    val node = get(fieldName) ?: return null
    return (node as? BooleanNode ?: throwInvalidType(fieldName, "a boolean", node)).booleanValue()
}

fun ObjectNode.getDouble(fieldName: String): Double {
    return getDoubleOrNull(fieldName) ?: throwMissingField(fieldName)
}

fun ObjectNode.getDoubleOrNull(fieldName: String): Double? {
    val node = get(fieldName) ?: return null
    return (node as? NumericNode ?: throwInvalidType(fieldName, "a number", node)).doubleValue()
}

private fun throwInvalidType(fieldName: String, expectedType: String, node: JsonNode): Nothing {
    throw IllegalArgumentException("Expected $expectedType at field $fieldName, got ${node.nodeType}")
}

private fun throwMissingField(fieldName: String): Nothing {
    throw NoSuchElementException("Required field $fieldName is missing")
}
