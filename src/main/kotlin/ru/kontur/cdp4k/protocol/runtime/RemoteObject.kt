package ru.kontur.cdp4k.protocol.runtime

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.util.getString
import ru.kontur.cdp4k.util.getStringOrNull

class RemoteObject(
    val type: String,
    val subtype: String?,
    val className: String?,
    val value: JsonNode?,
    val unserializableValue: String?,
    val description: String?,
    val objectId: RemoteObjectId?
) {

    companion object {

        fun fromTree(tree: ObjectNode): RemoteObject {
            return RemoteObject(
                type = tree.getString("type"),
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
