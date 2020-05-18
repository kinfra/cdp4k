package ru.kontur.cdp4k.launch

import kotlin.math.sin

object ChromeSwitches {

    val disableBackgroundNetworking = binary("disable-background-networking")
    val disableComponentExtensionsWithBackgroundPages = binary("disable-component-extensions-with-background-pages")
    val disableCrashReporter = binary("disable-crash-reporter")
    val disableDefaultApps = binary("disable-default-apps")
    val disableExtensions = binary("disable-extensions")
    val disableFeatures = multiple("disable-features")
    val disableGpu = binary("disable-gpu")
    val disablePluginPowerSaver = binary("disable-plugin-power-saver")
    val disablePopupBlocking = binary("disable-popup-blocking")
    val disableSync = binary("disable-sync")
    val enableAutomation = binary("enable-automation")
    val headless = binary("headless")
    val inProcessGpu = binary("in-process-gpu")
    val metricsRecordingOnly = binary("metrics-recording-only")
    val muteAudio = binary("mute-audio")
    val noDefaultBrowserCheck = binary("no-default-browser-check")
    val noFirstRun = binary("no-first-run")
    val noSandbox = binary("no-sandbox")
    val remoteDebuggingPipe = single("remote-debugging-pipe")
    val safebrowsingDisableAutoUpdate = binary("safebrowsing-disable-auto-update")
    val userDataDir = single("user-data-dir")

    internal fun binary(name: String) = ChromeSwitch.Binary(name)
    internal fun single(name: String) = ChromeSwitch.SingleValue(name)
    internal fun multiple(name: String) = ChromeSwitch.MultiValue(name)

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
        add(safebrowsingDisableAutoUpdate)
        add(disablePluginPowerSaver)
        add(disablePopupBlocking)
        add(disableCrashReporter)
        add(metricsRecordingOnly)
    }

}
