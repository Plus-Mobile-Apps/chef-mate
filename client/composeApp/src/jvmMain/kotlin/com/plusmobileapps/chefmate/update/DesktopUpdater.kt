package com.plusmobileapps.chefmate.update

import co.touchlab.kermit.Logger
import com.plusmobileapps.chefmate.buildconfig.BuildConfig
import com.plusmobileapps.chefmate.platform.isRunningInFlatpak
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import java.awt.Desktop
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json

private const val DEFAULT_FEED_URL =
    "https://github.com/Plus-Mobile-Apps/chef-mate/releases/latest/download/latest.json"

/**
 * Desktop-only self-update driver. Polls a `latest.json` feed published by CI, and when a newer
 * version exists, downloads the platform installer and opens it. Signed-installer model — we never
 * patch jars in place, so the installer's signature stays intact.
 *
 * In-app updates run on **Linux only**. macOS ships through the Mac App Store and Windows through
 * the Microsoft Store; both stores forbid self-updating (certification policy) and sandbox the
 * install so it can't replace itself anyway. Updates on those platforms are the store's job.
 */
class DesktopUpdater(
    private val currentVersion: String = BuildConfig.VERSION_NAME,
    private val feedUrl: String = DEFAULT_FEED_URL,
) {
    private val log = Logger.withTag("DesktopUpdater")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = HttpClient(CIO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var available: UpdateState.Available? = null

    /**
     * In-app updates run on Linux only; macOS and Windows defer to their respective app stores. The
     * Flathub build is excluded for the same reason as the stores: Flatpak owns the install and the
     * sandbox is read-only, so the app can neither replace itself nor run a downloaded .deb.
     */
    private val isSupported: Boolean = platformKey() == "linux" && !isRunningInFlatpak

    fun checkForUpdates() {
        if (!isSupported) return
        scope.launch {
            runCatching {
                val feed = json.decodeFromString<UpdateFeed>(http.get(feedUrl).bodyAsText())
                if (!isNewer(feed.version, currentVersion)) return@runCatching null
                val url = feed.downloads[platformKey()] ?: return@runCatching null
                UpdateState.Available(feed.version, url, feed.notesUrl).also { available = it }
            }
                .onSuccess { result -> result?.let { _state.value = it } }
                .onFailure { log.w(it) { "Update check failed" } }
        }
    }

    fun startDownload() {
        val target = available ?: return
        scope.launch {
            runCatching { downloadToTemp(target) }
                .onSuccess { _state.value = UpdateState.Ready(it.absolutePath) }
                .onFailure {
                    log.e(it) { "Update download failed" }
                    _state.value = target // revert to the Available banner so the user can retry
                }
        }
    }

    /** Opens the downloaded installer with the OS handler (mounts the dmg / runs the msi). */
    fun install() {
        val ready = _state.value as? UpdateState.Ready ?: return
        runCatching { Desktop.getDesktop().open(File(ready.installerPath)) }
            .onFailure { log.e(it) { "Failed to launch installer" } }
    }

    fun dismiss() {
        _state.value = UpdateState.Idle
    }

    fun close() {
        http.close()
        scope.cancel()
    }

    private suspend fun downloadToTemp(target: UpdateState.Available): File {
        val fileName = target.downloadUrl.substringAfterLast('/')
        val dest = File(System.getProperty("java.io.tmpdir"), fileName)
        http.prepareGet(target.downloadUrl).execute { response ->
            // A stale/misbuilt feed can point at a URL that 404s. Without this guard we'd stream
            // the error page into the .deb and hand the user a corrupt-package dialog on install.
            // Fail loudly so startDownload() reverts to the retryable Available banner instead.
            if (!response.status.isSuccess()) {
                error("Download failed: HTTP ${response.status.value} for ${target.downloadUrl}")
            }
            val total = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
            val channel: ByteReadChannel = response.bodyAsChannel()
            var read = 0L
            dest.outputStream().use { out ->
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (!packet.exhausted()) {
                        val bytes = packet.readByteArray()
                        out.write(bytes)
                        read += bytes.size
                        _state.value =
                            UpdateState.Downloading(
                                if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else -1f
                            )
                    }
                }
            }
        }
        return dest
    }
}

private fun platformKey(): String {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("mac") || os.contains("darwin") -> "macos"
        os.contains("win") -> "windows"
        else -> "linux"
    }
}

/** Compares dotted numeric versions (e.g. `1.8.0` > `1.7.1`). Non-numeric parts compare as 0. */
internal fun isNewer(candidate: String, current: String): Boolean {
    val c = candidate.split('.').map { it.toIntOrNull() ?: 0 }
    val n = current.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(c.size, n.size)) {
        val a = c.getOrElse(i) { 0 }
        val b = n.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}
