package ru.kontur.cdp4k.launch

object ChromeSwitches {

    /**
     * Disable several subsystems which run network requests in the background.
     * This is for use when doing network performance testing to avoid noise in the measurements.
     */
    val disableBackgroundNetworking = binary("disable-background-networking")

    /**
     * Disable default component extensions with background pages - useful for performance tests
     * where these pages may interfere with perf results.
     */
    val disableComponentExtensionsWithBackgroundPages = binary("disable-component-extensions-with-background-pages")

    /**
     * Disable crash reporter for headless. It is enabled by default in official builds.
     */
    val disableCrashReporter = binary("disable-crash-reporter")

    /**
     * Disables installation of default apps on first run. This is used during automated testing.
     */
    val disableDefaultApps = binary("disable-default-apps")

    /**
     * The `/dev/shm` partition is too small in certain VM environments,
     * causing Chrome to fail or crash (see http://crbug.com/715363).
     *
     * Use this flag to work-around this issue
     * (a temporary directory will always be used to create anonymous shared memory files).
     */
    val disableDevShmUsage = binary("disable-dev-shm-usage")

    /**
     * Disable extensions.
     */
    val disableExtensions = binary("disable-extensions")

    /**
     * List of feature names to disable.
     */
    val disableFeatures = multiple("disable-features")

    /**
     * Disables GPU hardware acceleration. If software renderer is not in place, then the GPU process won't launch.
     */
    val disableGpu = binary("disable-gpu")

    /**
     * Disables the GPU process sandbox.
     */
    val disableGpuSandbox = binary("disable-gpu-sandbox")

    /**
     * Disable pop-up blocking.
     */
    val disablePopupBlocking = binary("disable-popup-blocking")

    /**
     * Disables the use of a 3D software rasterizer.
     */
    val disableSoftwareRasterizer = binary("disable-software-rasterizer")

    /**
     * Disables syncing browser data to a Google Account.
     */
    val disableSync = binary("disable-sync")

    /**
     * Enable indication that browser is controlled by automation.
     */
    val enableAutomation = binary("enable-automation")

    /**
     * Run in headless mode, i.e., without a UI or display server dependencies.
     */
    val headless = binary("headless")

    /**
     * Ignore certificate errors
     */
    val ignoreCertificateErrors = binary("ignore-certificate-errors")

    /**
     * Ignores GPU blocklist.
     */
    val ignoreGpuBlocklist = binary("ignore-gpu-blocklist")

    /**
     * Run the GPU process as a thread in the browser process.
     */
    val inProcessGpu = binary("in-process-gpu")

    /**
     * Mutes audio sent to the audio device so it is not audible during automated testing.
     */
    val muteAudio = binary("mute-audio")

    /**
     * Disables the default browser check.
     * Useful for UI/browser tests where we want to avoid having the default browser info-bar displayed.
     */
    val noDefaultBrowserCheck = binary("no-default-browser-check")

    /**
     * Skip First Run tasks, whether or not it's actually the First Run. Overridden by `--force-first-run`.
     * This does not drop the First Run sentinel and thus doesn't prevent first run from occuring
     * the next time chrome is launched without this flag.
     */
    val noFirstRun = binary("no-first-run")

    /**
     * Disables the sandbox for all process types that are normally sandboxed.
     */
    val noSandbox = binary("no-sandbox")

    /**
     * Use the given address instead of the default loopback for accepting remote debugging connections.
     * Should be used together with [--remote-debugging-port][remoteDebuggingPort].
     * Note that the remote debugging protocol does not perform any authentication,
     * so exposing it too widely can be a security risk.
     */
    val remoteDebuggingAddress = single("remote-debugging-address")

    /**
     * Enables remote debug over stdio pipes [in=3, out=4].
     * Optionally, specifies the format for the protocol messages, can be either "JSON" (the default) or "CBOR".
     */
    val remoteDebuggingPipe = single("remote-debugging-pipe")

    /**
     * Enables remote debug over HTTP on the specified port.
     */
    val remoteDebuggingPort = single("remote-debugging-port")

    /**
     * Runs the renderer and plugins in the same process as the browser.
     */
    val singleProcess = binary("single-process")

    /**
     * Select which implementation of GL the GPU process should use. Options are:
     *  * `desktop`: whatever desktop OpenGL the user has installed (Linux and Mac default).
     *  * `egl`: whatever EGL / GLES2 the user has installed (Windows default - actually ANGLE).
     *  * `swiftshader`: The SwiftShader software renderer.
     */
    val useGl = single("use-gl")

    /**
     * Directory where the browser stores the user profile.
     */
    val userDataDir = single("user-data-dir")

    /**
     * Override the default user agent with a custom one.
     */
    val userAgent = single("user-agent")

    /**
     * Uses a specified proxy server, overrides system settings. This switch only affects HTTP and HTTPS requests.
     */
    val proxyServer = single("proxy-server")

    private fun binary(name: String) = ChromeSwitch.Binary(name)
    private fun single(name: String) = ChromeSwitch.SingleValue(name)
    private fun multiple(name: String) = ChromeSwitch.MultiValue(name)
}

fun ChromeCommandLine.Builder.useHeadlessDefaults() {
    with(ChromeSwitches) {
        add(headless)
        add(enableAutomation)
        add(muteAudio)

        add(disableGpu)
        add(inProcessGpu)

        add(noFirstRun)
        add(noDefaultBrowserCheck)

        add(disableFeatures, "TranslateUI")
        add(disableExtensions)
        add(disableComponentExtensionsWithBackgroundPages)
        add(disableDefaultApps)

        add(disableBackgroundNetworking)
        add(disableSync)
        add(disablePopupBlocking)
        add(disableCrashReporter)
    }

}
