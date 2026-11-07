package com.plusmobileapps.chefmate.deeplink

import co.touchlab.kermit.Logger
import com.plusmobileapps.chefmate.ChefMateUrls
import com.plusmobileapps.chefmate.platform.isRunningInFlatpak
import java.io.File

/**
 * Registers the `chefmate://` custom URL scheme with the host OS so that opening `chefmate://…`
 * launches this app. macOS registers the scheme declaratively via `CFBundleURLTypes` in the
 * packaged Info.plist (see `client/composeApp/build.gradle.kts`), so nothing is needed at runtime
 * there. Windows and Linux have no jpackage equivalent, so the app self-registers on each launch —
 * writing a per-user handler (no admin required) that is idempotent and self-heals if the app is
 * moved or reinstalled, since the launcher path is re-derived every run.
 *
 * Registration is best-effort: any failure is logged and swallowed so it can never break launch.
 */
object SchemeRegistrar {
    private val log = Logger.withTag("SchemeRegistrar")
    private const val SCHEME = ChefMateUrls.SCHEME
    private const val LINUX_DESKTOP_FILE = "chefmate-url-handler.desktop"

    /**
     * Registers the scheme when running from a packaged app image; a no-op under `./gradlew run`
     * (there is no installed launcher to point the OS at, and the dev flow passes the link via
     * `--args` instead).
     */
    fun registerIfPackaged() {
        // The Flathub build declares x-scheme-handler/chefmate in its exported .desktop file, so
        // the sandbox already routes the scheme. Self-registering there would only write a handler
        // inside the sandbox pointing at the sandbox-internal launcher path, which the host session
        // can neither see nor run.
        if (isRunningInFlatpak) return
        val launcher = launcherPath() ?: return
        runCatching {
            when (desktopOs()) {
                DesktopOs.WINDOWS -> registerWindows(launcher)
                DesktopOs.LINUX -> registerLinux(launcher)
                // macOS registration is declarative (CFBundleURLTypes); LaunchServices handles it.
                DesktopOs.MACOS -> Unit
            }
        }
            .onFailure { log.w(it) { "Failed to register $SCHEME:// scheme" } }
    }

    /**
     * The installed launcher executable, or null when not running from a jpackage image. jpackage
     * launchers (JDK 18+) expose their own path via the `jpackage.app-path` system property; its
     * absence is the reliable "not packaged" signal that also skips `./gradlew run`.
     */
    private fun launcherPath(): String? =
        System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }

    // ---- Windows -----------------------------------------------------------------------------

    private fun registerWindows(launcher: String) {
        // Import a .reg file rather than shelling out to `reg add`: the command value contains
        // embedded quotes and a `%1`, and Java's Windows argument quoting mangles those. A .reg
        // file's escaping rules are explicit and predictable.
        val regFile = File.createTempFile("chefmate-scheme", ".reg")
        try {
            regFile.writeText(windowsRegFileContent(launcher))
            val exit =
                ProcessBuilder("reg", "import", regFile.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor()
            if (exit != 0) log.w { "reg import exited with $exit" }
        } finally {
            regFile.delete()
        }
    }

    /**
     * The `.reg` file that maps `HKCU\Software\Classes\chefmate` to this launcher.
     * Pure/deterministic so it can be unit-tested. In a `.reg` value, backslashes and quotes are
     * escaped (`\\`, `\"`), so a launcher path like `C:\Program Files\Chef Mate\Chef Mate.exe` and
     * the `"%1"` argument are doubled up accordingly.
     */
    internal fun windowsRegFileContent(launcherPath: String): String {
        val escaped = launcherPath.replace("\\", "\\\\").replace("\"", "\\\"")
        return buildString {
            append("Windows Registry Editor Version 5.00\r\n\r\n")
            append("[HKEY_CURRENT_USER\\Software\\Classes\\$SCHEME]\r\n")
            append("@=\"URL:Chef Mate\"\r\n")
            append("\"URL Protocol\"=\"\"\r\n\r\n")
            append("[HKEY_CURRENT_USER\\Software\\Classes\\$SCHEME\\shell\\open\\command]\r\n")
            append("@=\"\\\"$escaped\\\" \\\"%1\\\"\"\r\n")
        }
    }

    // ---- Linux -------------------------------------------------------------------------------

    private fun registerLinux(launcher: String) {
        val appsDir = File(System.getProperty("user.home"), ".local/share/applications")
        appsDir.mkdirs()
        File(appsDir, LINUX_DESKTOP_FILE).writeText(linuxDesktopFileContent(launcher))
        // Point the scheme at our handler, then refresh the desktop database so it takes effect.
        // update-desktop-database is absent on some minimal distros — non-fatal.
        runProcess("xdg-mime", "default", LINUX_DESKTOP_FILE, "x-scheme-handler/$SCHEME")
        runProcess("update-desktop-database", appsDir.absolutePath)
    }

    /**
     * The `.desktop` handler entry declaring the `x-scheme-handler/chefmate` MIME type. Pure so it
     * can be unit-tested. The launcher path is quoted per the Desktop Entry spec (it may contain
     * spaces); `%u` passes the opened URL through as the single argument.
     */
    internal fun linuxDesktopFileContent(launcherPath: String): String = buildString {
        append("[Desktop Entry]\n")
        append("Type=Application\n")
        append("Name=Chef Mate\n")
        append("Exec=\"$launcherPath\" %u\n")
        append("NoDisplay=true\n")
        append("MimeType=x-scheme-handler/$SCHEME;\n")
    }

    private fun runProcess(vararg command: String) {
        runCatching {
            ProcessBuilder(*command).redirectErrorStream(true).start().waitFor()
        }
            .onFailure { log.w(it) { "command failed: ${command.joinToString(" ")}" } }
    }
}
