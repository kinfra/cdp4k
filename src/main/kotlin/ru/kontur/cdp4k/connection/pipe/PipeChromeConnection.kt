package ru.kontur.cdp4k.connection.pipe

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.future.await
import ru.kontur.cdp4k.connection.ChromeConnection
import ru.kontur.cdp4k.connection.ConnectionClosedException
import ru.kontur.cdp4k.connection.pipe.stream.CdpMessageStream
import ru.kontur.cdp4k.util.kill
import ru.kontur.kinfra.logging.Logger
import ru.kontur.kinfra.logging.LoggingContext
import java.io.IOException

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
internal class PipeChromeConnection private constructor(
    private val process: Process,
    private val codec: CdpMessageStream.Codec
) : ChromeConnection {

    private val subscriber = CompletableDeferred<ChromeConnection.Subscriber>()
    private val outgoing = Channel<ObjectNode>()

    private lateinit var job: Job

    private fun open() {
        val messageStream = codec.createMessageStream(process.inputStream, process.outputStream)

        @Suppress("BlockingMethodInNonBlockingContext")
        this.job = GlobalScope.launch(Dispatchers.IO + LoggingContext.EMPTY.with("pid", process.pid())) {
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
                            val receiveResult = outgoing.receiveCatching()
                            receiveResult.exceptionOrNull()?.let { throw it }
                            val message = receiveResult.getOrNull() ?: break
                            messageStream.writeMessage(message)
                            yield()
                        }
                    }

                    launch(CoroutineName("Chrome stderr logger")) {
                        val stderr = process.errorStream
                        try {
                            val reader = stderr.bufferedReader()
                            while (true) {
                                val line = reader.readLine() ?: break
                                stderrLogger.info { line }
                            }
                            logger.debug { "End of stderr" }
                        } catch (e: IOException) {
                            logger.error(e) { "Failed to read stderr" }
                        }
                    }

                    val exitValue = process.onExit().await().exitValue()
                    logger.info { "Process finished (exit value: $exitValue)" }
                    outgoing.close()
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
        val subscriberDeferred = this.subscriber
        val success = subscriberDeferred.complete(subscriber)
        if (!success) {
            check(subscriberDeferred.isCompleted) { "Could not put subscriber into $subscriberDeferred" }
            if (subscriberDeferred.isCancelled) {
                subscriber.onConnectionClosed()
            } else {
                val existing = subscriberDeferred.getCompleted()
                error("Already subscribed: $existing")
            }
        }
    }

    override fun toString(): String {
        return "PipeChromeConnection(pid: ${process.pid()})"
    }

    override suspend fun close() {
        logger.debug { "Closing connection to PID ${process.pid()}" }
        process.toHandle().kill()
        job.join()
    }

    companion object {

        private val logger = Logger.currentClass()
        private val stderrLogger = Logger.forName("ru.kontur.cdp4k.connection.stderr")

        fun open(process: Process, codec: CdpMessageStream.Codec): PipeChromeConnection {
            return PipeChromeConnection(process, codec).also {
                try {
                    it.open()
                } catch (e: Throwable) {
                    // clean up failed process
                    process.toHandle().destroyForcibly()
                    throw e
                }
            }
        }

    }

}
