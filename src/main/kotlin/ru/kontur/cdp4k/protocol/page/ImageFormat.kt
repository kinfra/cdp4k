package ru.kontur.cdp4k.protocol.page

sealed class ImageFormat(val format: String) {
    object Png : ImageFormat("png")

    class Jpeg(val quality: Int? = null) : ImageFormat("jpeg") {
        init {
            require(quality == null || quality in 0..100) { "compression quality should be from range [0..100]" }
        }
    }
}
