package ru.kontur.cdp4k.impl

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.future.await
import ru.kontur.cdp4k.connection.ChromeConnection
import ru.kontur.cdp4k.connection.ConnectionClosedException
import ru.kontur.cdp4k.impl.stream.CdpStreamCodec
import ru.kontur.jinfra.logging.Logger
import ru.kontur.jinfra.logging.LoggingContext
import ru.kontur.kinfra.commons.thenTake

@OptIn(ExperimentalCoroutinesApi::class)
internal class PipeChromeConnection private constructor(
    private val process: Process,
    private val codec: CdpStreamCodec
) : ChromeConnection {

    private val subscriber = CompletableDeferred<ChromeConnection.Subscriber>()
    private val outgoing = Channel<ObjectNode>()

    private lateinit var job: Job

    private fun open() {
        val messageStream = codec.createMessageStream(process.inputStream, process.outputStream)

        this.job = GlobalScope.launch(Dispatchers.IO + LoggingContext.EMPTY.add("pid", process.pid())) {
            try {
                coroutineScope {
                    launch(CoroutineName("Chrome pipe reader")) {
                        val subscriber = subscriber.await()
                        while (true) {
                            val message = messageStream.readMessage() ?: break
                            subscriber.onIncomingMessage(message)
                            yield()
                        }
                        logger.debug { "End of stream reached" }
                    }

                    launch(CoroutineName("Chrome pipe writer")) {
                        while (true) {
                            val message = outgoing.receiveOrNull() ?: break
                            messageStream.writeMessage(message)
                            yield()
                        }
                    }

                    val exitValue = process.onExit().await().exitValue()
                    val message = "Process finished (exit value: $exitValue)"
                    logger.info { message }
                    coroutineContext[Job]!!.cancelChildren(CancellationException(message))
                }
            } catch (e: Exception) {
                logger.error(e) { "Connection error" }
            } finally {
                outgoing.close()
                subscriber.cancel()
                if (subscriber.isCompleted && !subscriber.isCancelled) {
                    subscriber.getCompleted().onConnectionClosed()
                }
            }
        }
    }

    override suspend fun send(message: ObjectNode) {
        try {
            outgoing.send(message)
        } catch (e: ClosedSendChannelException) {
            throw ConnectionClosedException("Connection to PID ${process.pid()} is closed")
        }
    }

    override fun subscribe(subscriber: ChromeConnection.Subscriber) {
        val success = this.subscriber.complete(subscriber)
        if (!success) {
            val existing = this.subscriber.let { (it.isCompleted && !it.isCancelled).thenTake { it.getCompleted() } }
            if (existing != null) {
                throw IllegalStateException("Already subscribed: $existing")
            }
        }
    }

    override fun toString(): String {
        return "PipeChromeConnection(pid: ${process.pid()})"
    }

    override suspend fun close() {
        process.toHandle().kill()
        job.join()
    }

    companion object {

        private val logger = Logger.currentClass()

        fun open(process: Process, codec: CdpStreamCodec): PipeChromeConnection {
            return PipeChromeConnection(process, codec).also { it.open() }
        }

    }

}
