package ru.kontur.cdp4k.rpc

import ru.kontur.kinfra.io.SuspendingCloseable

interface RpcConnection : SuspendingCloseable {

    suspend fun <R> useBrowserSession(block: SessionUsage<R>): R

    suspend fun <R> useSession(id: String, block: SessionUsage<R>): R

}

