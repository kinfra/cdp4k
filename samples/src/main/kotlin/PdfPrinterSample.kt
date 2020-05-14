import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.kontur.cdp4k.impl.ChromeLauncher
import ru.kontur.cdp4k.impl.rpc.DefaultRpcConnection
import ru.kontur.cdp4k.impl.rpc.HealthCheckingRpcConnection
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.protocol.browser.BrowserDomain
import ru.kontur.cdp4k.protocol.io.IoDomain
import ru.kontur.cdp4k.protocol.io.RemoteInputStream
import ru.kontur.cdp4k.protocol.page.LoadEventFired
import ru.kontur.cdp4k.protocol.page.PageDomain
import ru.kontur.cdp4k.protocol.subscribeFirst
import ru.kontur.cdp4k.protocol.target.TargetDomain
import ru.kontur.cdp4k.rpc.RpcConnection
import ru.kontur.jinfra.logging.Logger
import ru.kontur.jinfra.logging.LoggingContext
import ru.kontur.kinfra.commons.time.MonotonicInstant
import ru.kontur.kinfra.io.OutputByteStream
import ru.kontur.kinfra.io.use
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

private val logger = Logger.currentClass().withoutContext()

private val counters = ConcurrentHashMap<String, Pair<Int, Long>>()

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

    val executorsCount = 3
    val cyclesCount = 2

    val connection = ChromeLauncher.launchWithPipe("/usr/lib/chromium-browser/chromium-browser", chromeArgs)
    val rpcConnection = DefaultRpcConnection.open(connection)
        .let { HealthCheckingRpcConnection(it) }
    rpcConnection.use {
        rpcConnection.useBrowserSession { browserSession ->
            val browserDomain = BrowserDomain(browserSession)
            val targetDomain = TargetDomain(browserSession)

            coroutineScope {
                repeat(executorsCount) { executorIndex ->
                    launch(LoggingContext.with("executor", executorIndex.toString())) {
                        repeat(cyclesCount) {
                            logExecutionTime("total") {
                                printPdf(targetDomain, rpcConnection, executorIndex)
                            }
                        }
                    }
                }
            }

            browserDomain.close()
        }
    }

    logger.info { "Avg counters:" }
    counters.forEach { (operation, counter) ->
        val (invocations, totalTime) = counter
        logger.info { "$operation: ${totalTime.toDouble() / invocations.toDouble()}" }
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
            rpcConnection.useSession(pageSessionId.value) { pageSession ->
                val pageDomain = PageDomain(pageSession)

                val pdfIndex = (executorIndex % 3) + 1
                val inputFile = Path.of(System.getProperty("user.home") + "/Desktop/extracts/small$pdfIndex.html")
                val url = inputFile.toUri()
                // val url = URI("http://localhost:8080")

                logExecutionTime("page load") {
                    pageDomain.stopLoading()
                    val pageLoaded = pageDomain.subscribeFirst(LoadEventFired)

                    val navigateResult = pageDomain.navigate(url.toString())
                    navigateResult.errorText?.let { err ->
                        throw RuntimeException("Failed to navigate page $url: $err")
                    }

                    pageLoaded.join()
                }

                val pdfResponse = logExecutionTime("PDF print") {
                    pageDomain.printToPdf(transferMode = PageDomain.PdfTransferMode.STREAM)
                }

                logExecutionTime("PDF transfer") {
                    RemoteInputStream(pdfResponse.stream!!, IoDomain(pageSession)).use { input ->
                        val destPath = inputFile.resolveSibling("result${executorIndex}.pdf")
                        // input.transferTo(OutputByteStream.nullStream())
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
    return block().also {
        val elapsed = MonotonicInstant.now() - startTime
        updateCounter(operationName, elapsed)
    }
}

private fun updateCounter(operationName: String, time: Duration) {
    val timeMillis = time.toMillis()
    counters.compute(operationName) { _, counter ->
        val (invocations, totalTime) = counter ?: (0 to 0L)
        invocations + 1 to totalTime + timeMillis
    }
    logger.info { "Executed $operationName in $timeMillis ms" }
}
