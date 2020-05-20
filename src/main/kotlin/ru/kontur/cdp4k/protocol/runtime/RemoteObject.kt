package ru.kontur.cdp4k.protocol.runtime

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.util.getString
import ru.kontur.cdp4k.util.getStringOrNull

/**
 * Mirror object referencing original JavaScript object.
 *
 * @property type Object type.
 * @property subtype Object subtype hint. Specified for [object][Type.OBJECT] or [wasm][Type.WASM] type values only.
 * Allowed Values: `array, null, node, regexp, date, map, set, weakmap, weakset, iterator, generator,
 * error, proxy, promise, typedarray, arraybuffer, dataview, i32, i64, f32, f64, v128, anyref`
 * @property className Object class (constructor) name. Specified for [object][Type.OBJECT] type values only.
 * @property value Remote object value in case of primitive values or JSON values (if it was requested).
 * @property unserializableValue Primitive value which can not be JSON-stringified does not have value,
 * but gets this property.
 * @property description String representation of the object.
 * @property objectId Unique object identifier (for non-primitive values).
 */
class RemoteObject(
    val type: Type,
    val subtype: String?,
    val className: String?,
    val value: JsonNode?,
    val unserializableValue: String?,
    val description: String?,
    val objectId: RemoteObjectId?
) {

    enum class Type(val value: String) {
        OBJECT("object"),
        FUNCTION("function"),
        UNDEFINED("undefined"),
        STRING("string"),
        NUMBER("number"),
        BOOLEAN("boolean"),
        SYMBOL("symbol"),
        BIGINT("bigint"),
        WASM("wasm"),
        ;

        companion object {

            fun fromValue(value: String): Type {
                return requireNotNull(values().find { it.value == value }) { "Unknown type: $value" }
            }

        }

    }

    companion object {

        fun fromTree(tree: ObjectNode): RemoteObject {
            return RemoteObject(
                type = Type.fromValue(tree.getString("type")),
                subtype = tree.getStringOrNull("subtype"),
                className = tree.getStringOrNull("className"),
                value = tree.get("value"),
                unserializableValue = tree.getStringOrNull("unserializableValue"),
                description = tree.getStringOrNull("description"),
                objectId = tree.getStringOrNull("objectId")?.let { RemoteObjectId(it) }
            )
        }

    }

}
