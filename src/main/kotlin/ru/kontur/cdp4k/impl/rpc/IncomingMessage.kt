package ru.kontur.cdp4k.impl.rpc

import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import ru.kontur.cdp4k.impl.EMPTY_TREE
import ru.kontur.cdp4k.impl.getObjectOrNull
import ru.kontur.cdp4k.impl.getString
import ru.kontur.kinfra.commons.Either
import java.util.concurrent.atomic.AtomicInteger

internal typealias RpcResult = Either<ObjectNode, ObjectNode>

internal sealed class IncomingMessage {

    val messageId = nextMessageId.getAndIncrement()

    data class Response(
        val requestId: Long,
        val result: RpcResult
    ) : IncomingMessage()

    data class Event(
        val methodName: String,
        val data: ObjectNode
    ) : IncomingMessage()

    companion object {

        private val nextMessageId = AtomicInteger(1)

        fun parse(message: ObjectNode): IncomingMessage {
            val requestId = message.get("id")
            return if (requestId != null) {
                require(requestId is NumericNode) { "Unexpected response id: $requestId" }
                val id = requestId.longValue()
                val rpcResult = parseResult(message)
                Response(id, rpcResult)
            } else {
                val method = message.getString("method")
                val params = message.getObjectOrNull("params") ?: EMPTY_TREE
                Event(method, params)
            }
        }

        private fun parseResult(message: ObjectNode): RpcResult {
            val result = message.getObjectOrNull("result")
            val error = message.getObjectOrNull("error")
            return when {
                error == null -> {
                    requireNotNull(result) {
                        val messageFields = message.fieldNames().asSequence().toList()
                        "Response must contain either result or error, got $messageFields"
                    }
                    Either.right(result)
                }

                result == null -> Either.left(error)

                else -> throw IllegalArgumentException("Response must contain either result or error, got both")
            }
        }

    }

}
