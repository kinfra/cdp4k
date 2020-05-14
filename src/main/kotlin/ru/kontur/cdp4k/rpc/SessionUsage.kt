package ru.kontur.cdp4k.rpc

import kotlinx.coroutines.CoroutineScope

internal typealias SessionUsage<R> = suspend CoroutineScope.(RpcSession) -> R
