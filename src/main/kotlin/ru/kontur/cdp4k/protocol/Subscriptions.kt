package ru.kontur.cdp4k.protocol

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

typealias EventSubscriber<T> = (event: T) -> Unit

interface EventSubscription : AutoCloseable

@Suppress("DeferredIsResult")
suspend fun <E : CdpEvent> CdpDomain<E>.subscribeFirst(
    eventCompanion: CdpEventCompanion<E>,
    predicate: (E) -> Boolean = { true }
): Deferred<E> {

    val eventDeferred = CompletableDeferred<E>()
    val subscription = subscribe(eventCompanion) { event ->
        if (predicate(event)) {
            eventDeferred.complete(event)
        }
    }

    eventDeferred.invokeOnCompletion {
        subscription.close()
    }

    return eventDeferred
}
