import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kontur.cdp4k.launch.ChromeCommandLine
import ru.kontur.cdp4k.launch.ChromeSwitches
import ru.kontur.cdp4k.launch.pipe.PipeChromeLauncher
import ru.kontur.cdp4k.launch.useHeadlessDefaults
import ru.kontur.cdp4k.protocol.CdpExperimental
import ru.kontur.cdp4k.protocol.browser.BrowserDomain
import ru.kontur.cdp4k.protocol.io.CdpInputStream
import ru.kontur.cdp4k.protocol.io.IoDomain
import ru.kontur.cdp4k.protocol.page.ImageFormat
import ru.kontur.cdp4k.protocol.page.LoadEventFired
import ru.kontur.cdp4k.protocol.page.PageDomain
import ru.kontur.cdp4k.protocol.subscribeFirst
import ru.kontur.cdp4k.protocol.target.TargetDomain
import ru.kontur.cdp4k.rpc.RpcConnection
import ru.kontur.cdp4k.rpc.RpcSession
import ru.kontur.cdp4k.rpc.impl.DefaultRpcConnection
import ru.kontur.cdp4k.rpc.impl.HealthCheckingRpcConnection
import ru.kontur.kinfra.commons.time.MonotonicInstant
import ru.kontur.kinfra.io.OutputByteStream
import ru.kontur.kinfra.io.use
import ru.kontur.kinfra.logging.Logger
import ru.kontur.kinfra.logging.LoggingContext
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

private val logger = Logger.currentClass()

private val counters = ConcurrentHashMap<String, Pair<Int, Long>>()

suspend fun main() {
    val dataDir = Path.of("./build/chrome")
    dataDir.toFile().deleteRecursively()
    Files.createDirectories(dataDir)

    val chromePath = "/usr/lib/chromium/chromium"
    val commandLine = ChromeCommandLine.build(chromePath, dataDir) {
        useHeadlessDefaults()
        if (ProcessHandle.current().pid() == 1L) {
            // Use https://github.com/krallin/tini as a replacement for init process
            addPrefix("tini", "-s", "-g", "--")
        }
        // Run Chromium in Docker
        addPrefix(
            "docker",
            "run",
            "--rm",
            "--init",
            "--interactive",
            "registry.kontur.host/realty/base-images/openjdk-17-slim-chromium:latest"
        )
        add(ChromeSwitches.noSandbox)
        add(ChromeSwitches.disableDevShmUsage)
    }

    val executorsCount = 2
    val cyclesCount = 100

    val connection = PipeChromeLauncher.launchChrome(commandLine)
    val rpcConnection = DefaultRpcConnection.open(connection)
        .let { HealthCheckingRpcConnection(it) }
    rpcConnection.use {
        rpcConnection.useBrowserSession { browserSession ->
            val browserDomain = BrowserDomain(browserSession)

            coroutineScope {
                repeat(executorsCount) { executorIndex ->
                    launch(LoggingContext.with("executor", executorIndex.toString())) {
                        repeat(cyclesCount) {
                            logExecutionTime("total") {
                                printPdf(rpcConnection, executorIndex)
                            }
                        }
                    }
                }
                captureScreenshot(rpcConnection)
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

@OptIn(CdpExperimental::class)
private suspend fun printPdf(rpcConnection: RpcConnection, executorIndex: Int) {
    newPage(rpcConnection) { pageSession ->
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
            CdpInputStream(pdfResponse.stream!!, IoDomain(pageSession)).use { input ->
                val destPath = inputFile.resolveSibling("result${executorIndex}.pdf")
                // input.transferTo(OutputByteStream.nullStream())
                OutputByteStream.intoFile(destPath).use { output ->
                    input.transferTo(output)
                }
            }
        }
    }
}

private suspend fun captureScreenshot(rpcConnection: RpcConnection) {
    newPage(rpcConnection) { pageSession ->
        val pageDomain = PageDomain(pageSession)
        pageDomain.stopLoading()
        val pageLoaded = pageDomain.subscribeFirst(LoadEventFired)

        val url = "https://www.uuidgenerator.net/"
        val navigateResult = pageDomain.navigate(url, referrer = "https://pkk.rosreestr.ru/")
        navigateResult.errorText?.let { err ->
            throw RuntimeException("Failed to navigate page $url: $err")
        }

        pageLoaded.join()

        val data = pageDomain.captureScreenshot(ImageFormat.Jpeg(100))
        val image = ByteArray(data.remaining())
        data.get(image)

        withContext(Dispatchers.IO) {
            Files.write(Path.of("screenShoot.jpg"), image)
        }
    }
}

private suspend fun <R> newPage(rpcConnection: RpcConnection, block: suspend (RpcSession) -> R): R {
    return rpcConnection.useBrowserSession { browserSession ->
        val targetDomain = TargetDomain(browserSession)
        val pageTargetId = targetDomain.createTarget("about:blank").targetId

        try {
            val pageSessionId = targetDomain.attachToTarget(pageTargetId)
            try {
                rpcConnection.useSession(pageSessionId.value) { pageSession ->
                    block(pageSession)
                }
            } finally {
                targetDomain.detachFromTarget(pageSessionId)
            }
        } finally {
            targetDomain.closeTarget(pageTargetId)
        }
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
