package ru.kontur.cdp4k.rpc

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.connection.ConnectionClosedException

/**
 * Client side of CDP session.
 */
interface RpcSession {

    val connection: RpcConnection

    /**
     * Execute an RPC request.
     *
     * @throws RpcErrorException if server signals an error
     * @throws ConnectionClosedException if this session is disconnected
     * @throws IllegalStateException if this session is closed
     */
    suspend fun executeRequest(methodName: String, params: ObjectNode): ObjectNode

    fun subscribe(methodName: String, callback: (ObjectNode) -> Unit): EventSubscription

    interface EventSubscription : AutoCloseable

}
