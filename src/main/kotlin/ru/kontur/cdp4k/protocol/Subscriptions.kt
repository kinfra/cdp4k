package ru.kontur.cdp4k.protocol

typealias EventSubscriber<T> = (event: T, subscription: EventSubscription) -> Unit

interface EventSubscription : AutoCloseable

suspend fun <T : E, E : CdpEvent, D : CdpDomain<E>> D.subscribeOnce(
    eventCompanion: EventCompanion<T>,
    callback: (T) -> Unit
): EventSubscription {

    return subscribe(eventCompanion) { event, subscription ->
        subscription.close()
        callback(event)
    }
}
