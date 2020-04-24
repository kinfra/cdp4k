package ru.kontur.cdp4k.protocol

@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
    message = "DevTools team doesn't commit to maintaining experimental APIs and changes/removes them regularly",
    level = RequiresOptIn.Level.WARNING
)
annotation class CdpExperimental
