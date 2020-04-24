package ru.kontur.cdp4k.protocol.page

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.impl.getString
import ru.kontur.cdp4k.impl.getStringOrNull
import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.protocol.io.StreamHandle
import ru.kontur.cdp4k.rpc.RpcSession

class PageDomain(session: RpcSession) : CdpDomain<PageEvent>(session) {

    override val id: String
        get() = "Page"

    suspend fun enable() {
        invoke("enable")
    }

    suspend fun disable() {
        invoke("disable")
    }

    suspend fun navigate(
        url: String,
        frameId: FrameId? = null,
        referrer: String? = null
    ): NavigateResult {

        val params = JsonNodeFactory.instance.objectNode().apply {
            put("url", url)
            if (frameId != null) put("frameId", frameId.value)
            if (referrer != null) put("referrer", referrer)
        }
        return invoke("navigate", params) { NavigateResult.fromTree(it) }
    }

    suspend fun printToPdf(
        transferMode: PdfTransferMode = PdfTransferMode.BASE64
    ): PrintToPdfResult {

        val params = JsonNodeFactory.instance.objectNode().apply {
            put("transferMode", transferMode.value)
        }
        return invoke("printToPDF", params) { PrintToPdfResult.fromTree(it) }
    }

    suspend fun stopLoading() {
        invoke("stopLoading")
    }

    override suspend fun enableEvents() {
        enable()
    }

    enum class PdfTransferMode(val value: String) {
        BASE64("ReturnAsBase64"),
        STREAM("ReturnAsStream"),
    }

    class NavigateResult(
        val frameId: FrameId,
        val errorText: String?
        /*loaderId*/
    ) {

        companion object {

            fun fromTree(tree: ObjectNode): NavigateResult {
                return NavigateResult(
                    frameId = FrameId(tree.getString("frameId")),
                    errorText = tree.getStringOrNull("errorText")
                )
            }

        }

    }

    class PrintToPdfResult(
        val data: String,
        val stream: StreamHandle?
    ) {

        companion object {

            fun fromTree(tree: ObjectNode): PrintToPdfResult {
                return PrintToPdfResult(
                    data = tree.getString("data"),
                    stream = tree.getStringOrNull("stream")?.let { StreamHandle(it) }
                )
            }

        }

    }
}
