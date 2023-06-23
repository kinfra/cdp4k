package ru.kontur.cdp4k.protocol.page

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.protocol.io.StreamHandle
import ru.kontur.cdp4k.protocol.network.LoaderId
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.cdp4k.util.getString
import ru.kontur.cdp4k.util.getStringOrNull
import ru.kontur.cdp4k.util.jsonObject
import java.nio.ByteBuffer
import java.util.*

/**
 * Actions and events related to the inspected page belong to the page domain.
 */
class PageDomain(session: RpcSession) : CdpDomain<PageEvent>(session) {

    override val id: String
        get() = "Page"

    /**
     * Capture page screenshot.
     *
     * @param format Image compression format (defaults to png).
     * @param clip Capture the screenshot of a given region only.
     */
    suspend fun captureScreenshot(
        format: ImageFormat? = null,
        clip: Viewport? = null,
        captureBeyondViewport: Boolean? = null,
    ): ByteBuffer {

        val params = jsonObject().apply {
            when (format) {
                is ImageFormat.Jpeg -> {
                    put("format", format.format)
                    if (format.quality != null) {
                        put("quality", format.quality)
                    }
                }
                ImageFormat.Png -> {
                    put("format", format.format)
                }
                null -> {
                }
            }
            if (clip != null) {
                val clipJson = jsonObject().apply {
                    put("x", clip.x)
                    put("y", clip.y)
                    put("width", clip.width)
                    put("height", clip.height)
                    put("scale", clip.scale)
                }
                replace("clip", clipJson)
            }
            if (captureBeyondViewport != null) {
                put("captureBeyondViewport", captureBeyondViewport)
            }
        }
        return invoke("captureScreenshot", params) {
            val data = it.getString("data")
            val decoder = Base64.getDecoder()
            ByteBuffer.wrap(decoder.decode(data))
        }
    }

    /**
     * Crashes renderer on the IO thread, generates minidumps.
     */
    @CdpExperimental
    suspend fun crash() {
        invoke("crash")
    }

    /**
     * Disables page domain notifications.
     */
    suspend fun disable() {
        invoke("disable")
    }

    /**
     * Enables page domain notifications.
     */
    suspend fun enable() {
        invoke("enable")
    }

    /**
     * Navigates current page to the given URL.
     *
     * @param url URL to navigate the page to.
     * @param referrer Referrer URL.
     * @param transitionType Intended transition type.
     * @param frameId Frame id to navigate, if not specified navigates the top frame.
     */
    suspend fun navigate(
        url: String,
        referrer: String? = null,
        transitionType: TransitionType? = null,
        frameId: FrameId? = null
    ): NavigateResult {

        val params = jsonObject().apply {
            put("url", url)
            if (referrer != null) put("referrer", referrer)
            if (transitionType != null) put("transitionType", transitionType.value)
            if (frameId != null) put("frameId", frameId.value)
        }
        return invoke("navigate", params) { NavigateResult.fromTree(it) }
    }

    /**
     * Print page as PDF.
     *
     * @param landscape Paper orientation. Defaults to false.
     * @param displayHeaderFooter Display header and footer. Defaults to false.
     * @param printBackground Print background graphics. Defaults to false.
     * @param scale Scale of the webpage rendering. Defaults to 1.
     * @param paperWidth Paper width in inches. Defaults to 8.5 inches.
     * @param paperHeight Paper height in inches. Defaults to 11 inches.
     * @param marginTop Top margin in inches. Defaults to 1cm (~0.4 inches).
     * @param marginBottom Bottom margin in inches. Defaults to 1cm (~0.4 inches).
     * @param marginLeft Left margin in inches. Defaults to 1cm (~0.4 inches).
     * @param marginRight Right margin in inches. Defaults to 1cm (~0.4 inches).
     * @param pageRanges Paper ranges to print, e.g., '1-5, 8, 11-13'.
     * Defaults to the empty string, which means print all pages.
     * @param ignoreInvalidPageRanges Whether to silently ignore invalid but successfully parsed page ranges,
     * such as '3-2'. Defaults to false.
     * @param headerTemplate HTML template for the print header.
     * Should be valid HTML markup with following classes used to inject printing values into them:
     *
     *   * date: formatted print date
     *   * title: document title
     *   * url: document location
     *   * pageNumber: current page number
     *   * totalPages: total pages in the document
     *
     * For example, <span class=title></span> would generate span containing the title.
     * @param footerTemplate HTML template for the print footer. Should use the same format as the [headerTemplate].
     * @param preferCSSPageSize Whether or not to prefer page size as defined by css.
     * Defaults to false, in which case the content will be scaled to fit the paper size.
     * @param transferMode return as stream
     */
    suspend fun printToPdf(
        landscape: Boolean? = null,
        displayHeaderFooter: Boolean? = null,
        printBackground: Boolean? = null,
        scale: Double? = null,
        paperWidth: Double? = null,
        paperHeight: Double? = null,
        marginTop: Double? = null,
        marginBottom: Double? = null,
        marginLeft: Double? = null,
        marginRight: Double? = null,
        pageRanges: String? = null,
        ignoreInvalidPageRanges: Boolean? = null,
        headerTemplate: String? = null,
        footerTemplate: String? = null,
        preferCSSPageSize: Boolean? = null,
        transferMode: PdfTransferMode? = null,
    ): PrintToPdfResult {

        val params = jsonObject().apply {
            if (landscape != null) put("landscape", landscape)
            if (displayHeaderFooter != null) put("displayHeaderFooter", displayHeaderFooter)
            if (printBackground != null) put("printBackground", printBackground)
            if (scale != null) put("scale", scale)
            if (paperWidth != null) put("paperWidth", paperWidth)
            if (paperHeight != null) put("paperHeight", paperHeight)
            if (marginTop != null) put("marginTop", marginTop)
            if (marginBottom != null) put("marginBottom", marginBottom)
            if (marginLeft != null) put("marginLeft", marginLeft)
            if (marginRight != null) put("marginRight", marginRight)
            if (pageRanges != null) put("pageRanges", pageRanges)
            if (ignoreInvalidPageRanges != null) put("ignoreInvalidPageRanges", ignoreInvalidPageRanges)
            if (headerTemplate != null) put("headerTemplate", headerTemplate)
            if (footerTemplate != null) put("footerTemplate", footerTemplate)
            if (preferCSSPageSize != null) put("preferCSSPageSize", preferCSSPageSize)
            if (transferMode != null) put("transferMode", transferMode.value)
        }
        return invoke("printToPDF", params) { PrintToPdfResult.fromTree(it) }
    }

    /**
     * Force the page stop all navigations and pending resource fetches.
     */
    suspend fun stopLoading() {
        invoke("stopLoading")
    }

    override suspend fun enableEvents() {
        enable()
    }

    /**
     * Value of `transferMode` parameter of [PageDomain.printToPdf].
     */
    enum class PdfTransferMode(val value: String) {
        BASE64("ReturnAsBase64"),
        STREAM("ReturnAsStream"),
    }

    /**
     * Result of invoking [PageDomain.navigate].
     *
     * @property frameId Frame id that has navigated (or failed to navigate)
     * @property loaderId Loader identifier.
     * @property errorText User friendly error message, present if and only if navigation has failed.
     */
    class NavigateResult(
        val frameId: FrameId,
        val loaderId: LoaderId?,
        val errorText: String?
    ) {

        companion object {

            fun fromTree(tree: ObjectNode): NavigateResult {
                return NavigateResult(
                    frameId = FrameId(tree.getString("frameId")),
                    loaderId = tree.getStringOrNull("loaderId")?.let { LoaderId(it) },
                    errorText = tree.getStringOrNull("errorText")
                )
            }

        }

    }

    enum class TransitionType(val value: String) {
        LINK("link"),
        TYPED("typed"),
        ADDRESS_BAR("address_bar"),
        AUTO_BOOKMARK("auto_bookmark"),
        AUTO_SUBFRAME("auto_subframe"),
        MANUAL_SUBFRAME("manual_subframe"),
        GENERATED("generated"),
        AUTO_TOPLEVEL("auto_toplevel"),
        FORM_SUBMIT("form_submit"),
        RELOAD("reload"),
        KEYWORD("keyword"),
        KEYWORD_GENERATED("keyword_generated"),
        OTHER("other"),
    }

    /**
     * Result of invoking [PageDomain.printToPdf].
     *
     * @property data Base64-encoded pdf data.
     * @property stream A handle of the stream that holds resulting PDF data.
     */
    class PrintToPdfResult(
        val data: String,
        @property:CdpExperimental
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
