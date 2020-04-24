package ru.kontur.cdp4k.rpc

import ru.kontur.kinfra.io.SuspendingCloseable

interface RpcConnection : SuspendingCloseable {

    val browserSession: RpcSession

    fun openSession(id: String): RpcSession

}

