package ru.kontur.cdp4k.protocol

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.kontur.cdp4k.impl.EMPTY_TREE
import ru.kontur.cdp4k.rpc.RpcSession
import java.util.concurrent.atomic.AtomicReference

abstract class CdpDomain<E : CdpEvent> internal constructor(
    private val session: RpcSession
) {

    protected abstract val id: String

    private var eventsEnabled = false
    private var eventsEnableMutex = Mutex()

    protected suspend fun <T : Any> invoke(
        methodName: String,
        params: ObjectNode = EMPTY_TREE,
        parser: (ObjectNode) -> T
    ): T {

        val result = session.executeRequest("$id.$methodName", params)
        return parser(result)
    }

    protected suspend fun invoke(methodName: String, params: ObjectNode = EMPTY_TREE) {
        invoke(methodName, params) { }
    }

    suspend fun <T : E> subscribe(
        eventCompanion: EventCompanion<T>,
        subscriber: EventSubscriber<T>
    ): EventSubscription {

        eventsEnableMutex.withLock {
            if (!eventsEnabled) {
                enableEvents()
                eventsEnabled = true
            }
        }

        val subscriptionRef = AtomicReference<EventSubscription>()
        val subscriberWrapper = { data: ObjectNode ->
            val subscription = subscriptionRef.get()
            if (subscription != null) {
                // todo: avoid parsing the tree for each subscriber
                val parsedData = eventCompanion.parse(data)
                subscriber(parsedData, subscription)
            }
        }

        val rpcSubscription = session.subscribe("$id.${eventCompanion.methodName}", subscriberWrapper)
        val subscription = object : EventSubscription {
            override fun close() {
                rpcSubscription.close()
            }
        }
        subscriptionRef.set(subscription)
        return subscription
    }

    protected open suspend fun enableEvents() = Unit

}
