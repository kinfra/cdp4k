package ru.kontur.cdp4k.protocol.emulation

import ru.kontur.cdp4k.protocol.CdpDomain
import ru.kontur.cdp4k.protocol.page.PageEvent
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.cdp4k.util.jsonObject

/**
 * This domain emulates different environments for the page.
 */
class EmulationDomain(session: RpcSession) : CdpDomain<PageEvent>(session) {

    override val id: String
        get() = "Emulation"

    /**
     * Emulates the given media type or media feature for CSS media queries.
     *
     * @param media Media type to emulate. Empty string disables the override.
     * @param features Media features to emulate.
     */
    suspend fun setEmulatedMedia(
        media: String? = null,
        features: List<MediaFeature> = emptyList()
    ) {
        val params = jsonObject().apply {
            media?.let { put("media", it) }

            if (features.isNotEmpty()) {
                val featuresArray = putArray("features")
                features.forEach {
                    val feature = objectNode()
                        .put("name", it.name)
                        .put("value", it.value)
                    featuresArray.add(feature)
                }
            }
        }

        invoke("setEmulatedMedia", params)
    }

    /**
     * Overrides default host system locale with the specified one.
     *
     * @param locale ICU style C locale (e.g. "en_US"). If not specified or empty, disables the override and restores default host system locale.
     */
    suspend fun setLocaleOverride(
        locale: String? = null,
    ) {
        val params = jsonObject().apply {
            locale?.let { put("locale", it) }
        }

        invoke("setLocaleOverride", params)
    }
}
