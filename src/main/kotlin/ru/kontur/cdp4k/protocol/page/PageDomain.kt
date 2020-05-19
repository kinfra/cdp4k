package ru.kontur.cdp4k.protocol.page

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.protocol.io.StreamHandle
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.cdp4k.util.getString
import ru.kontur.cdp4k.util.getStringOrNull
import ru.kontur.cdp4k.util.jsonObject

class PageDomain(session: RpcSession) : CdpDomain<PageEvent>(session) {

    override val id: String
        get() = "Page"

    @CdpExperimental
    suspend fun crash() {
        invoke("crash")
    }

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

        val params = jsonObject().apply {
            put("url", url)
            if (frameId != null) put("frameId", frameId.value)
            if (referrer != null) put("referrer", referrer)
        }
        return invoke("navigate", params) { NavigateResult.fromTree(it) }
    }

    suspend fun printToPdf(
        transferMode: PdfTransferMode = PdfTransferMode.BASE64
    ): PrintToPdfResult {

        val params = jsonObject().apply {
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
