import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import ru.kontur.cdp4k.impl.ChromeLauncher
import ru.kontur.cdp4k.impl.rpc.DefaultRpcConnection
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.protocol.browser.BrowserDomain
import ru.kontur.cdp4k.protocol.io.IoDomain
import ru.kontur.cdp4k.protocol.io.RemoteInputStream
import ru.kontur.cdp4k.protocol.page.LoadEventFired
import ru.kontur.cdp4k.protocol.page.PageDomain
import ru.kontur.cdp4k.protocol.subscribeOnce
import ru.kontur.cdp4k.protocol.target.TargetDomain
import ru.kontur.cdp4k.rpc.RpcConnection
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.jinfra.logging.Logger
import ru.kontur.jinfra.logging.LoggingContext
import ru.kontur.kinfra.commons.time.MonotonicInstant
import ru.kontur.kinfra.io.OutputByteStream
import ru.kontur.kinfra.io.use
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private val logger = Logger.currentClass().withoutContext()

private val counters = ConcurrentHashMap<String, AtomicLong>()

@OptIn(CdpExperimental::class)
@Suppress("BlockingMethodInNonBlockingContext")
fun main() = runBlocking {

    val dataDir = Path.of("./build/chrome").toAbsolutePath()
    dataDir.toFile().deleteRecursively()
    Files.createDirectories(dataDir)

    val chromeArgs = listOf(
        "--user-data-dir=$dataDir",

        // PDF printing works in Headless Chrome only
        "--headless",
        "--no-sandbox",
        "--disable-gpu", "--in-process-gpu",
        // "--single-process", "--no-zygote",
        "--enable-automation",

        // Well-known options, suitable for headless use
        "--disable-features=TranslateUI",
        "--disable-extensions",
        "--disable-component-extensions-with-background-pages",
        "--disable-background-networking",
        "--safebrowsing-disable-auto-update",
        "--disable-sync",
        "--metrics-recording-only",
        "--disable-default-apps",
        "--mute-audio",
        "--no-first-run",
        "--no-default-browser-check",
        "--disable-plugin-power-saver",
        "--disable-popup-blocking"
    )

    val executions = AtomicInteger()

    val connection = ChromeLauncher.launchWithPipe("/usr/lib/chromium-browser/chromium-browser", chromeArgs)
    DefaultRpcConnection.open(connection).use { rpcConnection ->
        val browserSession = rpcConnection.browserSession
        val browserDomain = BrowserDomain(browserSession)
        val targetDomain = TargetDomain(browserSession)

        coroutineScope {
            repeat(Runtime.getRuntime().availableProcessors()) { executorIndex ->
                launch {
                    withContext(LoggingContext.with("executor", executorIndex.toString())) {
                        repeat(2) {
                            executions.incrementAndGet()
                            logExecutionTime("total") {
                                printPdf(targetDomain, rpcConnection, executorIndex)
                            }
                        }
                    }
                }
            }
        }

        browserDomain.close()
    }

    logger.info { "Counters:" }
    counters.forEach { (operation, counter) ->
        logger.info { "$operation: ${counter.toDouble() / executions.toDouble()}" }
    }
}

private suspend fun printPdf(
    targetDomain: TargetDomain,
    rpcConnection: RpcConnection,
    executorIndex: Int
) {

    val pageTargetId = targetDomain.createTarget("about:blank").targetId

    try {
        val pageSessionId = targetDomain.attachToTarget(pageTargetId)
        try {
            rpcConnection.openSession(pageSessionId.value).use { pageSession ->
                val pageDomain = PageDomain(pageSession)

                val pdfIndex = (executorIndex % 3) + 1
                val inputFile = Path.of("/home/frostbit/Desktop/extracts/small$pdfIndex.html")

                logExecutionTime("page load") {
                    val pageLoaded = CompletableDeferred<Unit>()
                    pageDomain.stopLoading()
                    pageDomain.subscribeOnce(LoadEventFired) {
                        pageLoaded.complete(Unit)
                    }

                    val url = inputFile.toUri()
                    val navigateResult = pageDomain.navigate(url.toString())
                    navigateResult.errorText?.let { err ->
                        throw RuntimeException("Failed to load page $url: $err")
                    }
                    pageLoaded.join()
                }

                val pdfResponse = logExecutionTime("PDF print") {
                    pageDomain.printToPdf(transferMode = PageDomain.PdfTransferMode.STREAM)
                }

                logExecutionTime("PDF transfer") {
                    RemoteInputStream(pdfResponse.stream!!, IoDomain(pageSession)).use { input ->
                        // input.transferTo(OutputByteStream.nullStream())
                        val destPath = inputFile.resolveSibling("result${executorIndex}.pdf")
                        OutputByteStream.intoFile(destPath).use { output ->
                            input.transferTo(output)
                        }
                    }
                }
            }
        } finally {
            targetDomain.detachFromTarget(pageSessionId)
        }
    } finally {
        targetDomain.closeTarget(pageTargetId)
    }
}

private inline fun <R> logExecutionTime(operationName: String, block: () -> R): R {
    val startTime = MonotonicInstant.now()
    return try {
        block()
    } finally {
        val elapsed = MonotonicInstant.now() - startTime
        val counter = counters.computeIfAbsent(operationName) { AtomicLong() }
        counter.addAndGet(elapsed.toMillis())
        logger.info { "Executed $operationName in ${elapsed.toMillis()} ms" }
    }
}

private suspend fun RpcSession.executeRequest(method: String, paramsBuilder: ObjectNode.() -> Unit = {}): ObjectNode {
    val params = JsonNodeFactory.instance.objectNode().apply(paramsBuilder)
    return executeRequest(method, params)
}
