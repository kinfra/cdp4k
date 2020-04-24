package ru.kontur.cdp4k.protocol.browser

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.impl.getString

class BrowserVersion(
    val protocolVersion: String,
    val product: String,
    val revision: String,
    val userAgent: String,
    val jsVersion: String
) {

    override fun toString(): String {
        return "BrowserVersion(protocolVersion='$protocolVersion'" +
            ", product='$product', revision='$revision', userAgent='$userAgent'" +
            ", jsVersion='$jsVersion')"
    }

    companion object {

        internal fun fromTree(tree: ObjectNode): BrowserVersion {
            return BrowserVersion(
                protocolVersion = tree.getString("protocolVersion"),
                product = tree.getString("product"),
                revision = tree.getString("revision"),
                userAgent = tree.getString("userAgent"),
                jsVersion = tree.getString("jsVersion")
            )
        }

    }

}
