package ru.kontur.cdp4k.rpc

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.CopyableThrowable
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class RpcErrorException private constructor(
    val code: Int,
    val description: String,
    val data: JsonNode?,
    cause: RpcErrorException?
) : RuntimeException(buildExceptionMessage(code, description, data), cause),
    CopyableThrowable<RpcErrorException> {

    constructor(code: Int, description: String, data: JsonNode?) : this(code, description, data, null)

    override fun createCopy(): RpcErrorException {
        return RpcErrorException(code, description, data, this)
    }

    companion object {

        private fun buildExceptionMessage(code: Int, description: String, data: JsonNode?): String {
            return buildString {
                append(description)
                append(" (")
                append(code)
                append(")")
                if (data != null && data.isTextual) {
                    append(": ")
                    append(data.textValue())
                }
            }
        }

    }

}
