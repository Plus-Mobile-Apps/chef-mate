package com.plusmobileapps.chefmate.client.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.plusmobileapps.chefmate.database.Database
import java.io.File
import java.util.Properties

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        useBundledSqliteNativeLibIfPresent()

        val dbPath = getAppDataDirectory()
        dbPath.mkdirs()
        val dbFile = File(dbPath, "chefmate.db")

        return JdbcSqliteDriver(
                url = "jdbc:sqlite:${dbFile.absolutePath}",
                properties = Properties(),
                schema = Database.Schema,
            )
            .also { it.execute(null, "PRAGMA foreign_keys = ON", 0) }
    }

    /**
     * On the macOS App Store build, load sqlite-jdbc's native lib from the signed app bundle
     * instead of letting it extract an unsigned copy to a temp dir at runtime. The App Store
     * sandbox forbids loading code that isn't part of the signed bundle, so the default
     * extract-and-dlopen path is blocked ("could not verify … free of malware"). The build stages
     * the `.dylib` into Compose's app-resources dir (see composeApp/build.gradle.kts
     * `extractSqliteJdbcMacDylib`); point sqlite-jdbc at it via `org.sqlite.lib.path`/`name` so it
     * never extracts. No-op on other platforms and in `./gradlew run` (where the lib isn't staged
     * and the process isn't sandboxed).
     */
    private fun useBundledSqliteNativeLibIfPresent() {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        if (!os.contains("mac") && !os.contains("darwin")) return
        val resourcesDir = System.getProperty("compose.application.resources.dir") ?: return
        val nativeLib = File(resourcesDir, "libsqlitejdbc.dylib")
        if (nativeLib.exists()) {
            System.setProperty("org.sqlite.lib.path", resourcesDir)
            System.setProperty("org.sqlite.lib.name", nativeLib.name)
        }
    }

    private fun getAppDataDirectory(): File {
        val os = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")

        return when {
            os.contains("mac") -> File(userHome, "Library/Application Support/Chef Mate")
            os.contains("win") -> File(System.getenv("APPDATA") ?: userHome, "Chef Mate")
            // Linux: honor XDG_DATA_HOME, falling back to its spec default of ~/.local/share.
            // Outside a sandbox the two are the same path, but the Flatpak build has no access to
            // the real ~/.local/share — Flatpak points XDG_DATA_HOME at the app's private
            // ~/.var/app/<id>/data instead, and writing the hardcoded path there fails.
            else -> {
                val dataHome =
                    System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
                        ?: File(userHome, ".local/share").path
                File(dataHome, "chef-mate")
            }
        }
    }
}
