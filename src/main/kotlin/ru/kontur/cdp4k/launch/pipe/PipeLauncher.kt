package ru.kontur.cdp4k.launch.pipe

import ru.kontur.cdp4k.launch.ChromeCommandLine

/*
 * Chrome uses file descriptors 3 and 4 for CDP I/O.
 * These descriptors are inaccessible from plain Java.
 * The only available are 0 (stdin), 1 (stdout), and 2 (stderr).
 *
 * Implementation of this interface launches Chrome in such a way
 * that resulting Process's stdin and stdout are used for CDP I/O.
 */
internal interface PipeLauncher {

    suspend fun launchChrome(commandLine: ChromeCommandLine): Process

}
