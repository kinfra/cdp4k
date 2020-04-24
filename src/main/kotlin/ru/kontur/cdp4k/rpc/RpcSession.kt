package ru.kontur.cdp4k.rpc

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.protocol.target.TargetDomain

/**
 * Client side of CDP session.
 */
interface RpcSession : AutoCloseable {

    val connection: RpcConnection

    /**
     * Execute an RPC request.
     *
     * @throws RpcErrorException if server signals an error
     * @throws IllegalStateException if this session is closed
     */
    suspend fun executeRequest(methodName: String, params: ObjectNode): ObjectNode

    fun subscribe(methodName: String, callback: (ObjectNode) -> Unit): EventSubscription

    /**
     * Close this session, cancelling active requests and subscriptions.
     *
     * Note that session should also be closed on the browser side via [TargetDomain.detachFromTarget].
     */
    override fun close()

    interface EventSubscription : AutoCloseable

}
