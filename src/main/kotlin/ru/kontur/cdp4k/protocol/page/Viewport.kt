package ru.kontur.cdp4k.protocol.page

data class Viewport(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val scale: Double = 1.0
) {
    init {
        require(x >= 0) { "x should be positive or zero" }
        require(y >= 0) { "y should be positive or zero" }
        require(width > 0) { "width should be positive" }
        require(height > 0) { "height should be positive" }
        require(scale > 0) { "scale should be positive" }
    }
}
