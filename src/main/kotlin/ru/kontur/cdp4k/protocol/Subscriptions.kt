package ru.kontur.cdp4k.protocol

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

// todo: remove subscription argument
typealias EventSubscriber<T> = (event: T) -> Unit

interface EventSubscription : AutoCloseable

suspend fun <T : E, E : CdpEvent, D : CdpDomain<E>> D.subscribeFirst(
    eventCompanion: CdpEventCompanion<T>,
    predicate: (T) -> Boolean = { true }
): Deferred<T> {

    val eventDeferred = CompletableDeferred<T>()
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

/**
 * This function is internal because it can be easily misused:
 * ```
 * coroutineScope {
 *     launch { domain.waitFor(SomeEvent) }
 *     domain.triggerEvent()
 * }
 * ```
 * In the code above a race condition occurs between enabling events for the domain and triggering the event.
 */
internal suspend fun <T : E, E : CdpEvent, D : CdpDomain<E>> D.waitFor(
    eventCompanion: CdpEventCompanion<T>,
    predicate: (T) -> Boolean = { true }
): T {

    return subscribeFirst(eventCompanion).await()
}
