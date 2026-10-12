package com.plusmobileapps.chefmate.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import java.awt.BorderLayout
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.web.WebView
import javax.swing.JPanel

@Composable
actual fun PlatformWebView(
    url: String,
    onUrlLoaded: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onCanNavigateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    goBackTrigger: Int,
    goForwardTrigger: Int,
    captureHtmlTrigger: Int,
    onHtmlCaptured: (String?) -> Unit,
    instanceKeeper: InstanceKeeper,
    modifier: Modifier,
) {
    val holder = remember { instanceKeeper.getOrCreate { WebViewHolder() } }

    DisposableEffect(Unit) {
        holder.ensureInitialized(onUrlLoaded, onLoadingChanged, onCanNavigateChanged, url)
        onDispose {}
    }

    LaunchedEffect(url) {
        if (url.isNotBlank() && url != holder.lastCommandedUrl) {
            holder.lastCommandedUrl = url
            Platform.runLater { holder.webView?.engine?.load(url) }
        }
    }

    LaunchedEffect(goBackTrigger) { if (goBackTrigger > 0) holder.goBack() }

    LaunchedEffect(goForwardTrigger) { if (goForwardTrigger > 0) holder.goForward() }

    LaunchedEffect(captureHtmlTrigger) {
        if (captureHtmlTrigger > 0) holder.captureHtml(onHtmlCaptured)
    }

    SwingPanel(
        modifier = modifier,
        factory = { JPanel(BorderLayout()).apply { add(holder.jfxPanel, BorderLayout.CENTER) } },
    )
}

private class WebViewHolder : InstanceKeeper.Instance {
    val jfxPanel: JFXPanel = JFXPanel().also { Platform.setImplicitExit(false) }

    @Volatile
    var webView: WebView? = null
        private set

    @Volatile var lastCommandedUrl: String = ""

    @Volatile private var onCanNavigateChanged: ((Boolean, Boolean) -> Unit)? = null

    private var initialized = false

    fun goBack() {
        Platform.runLater {
            webView?.engine?.history?.let { history ->
                if (history.currentIndex > 0) history.go(-1)
            }
        }
    }

    fun goForward() {
        Platform.runLater {
            webView?.engine?.history?.let { history ->
                if (history.currentIndex < history.entries.size - 1) history.go(1)
            }
        }
    }

    /**
     * Reads the loaded page's markup off the JavaFX application thread, where the engine requires
     * every call to happen. [onCaptured] therefore runs on that thread, not the caller's.
     */
    fun captureHtml(onCaptured: (String?) -> Unit) {
        Platform.runLater {
            onCaptured(
                runCatching { webView?.engine?.executeScript(CAPTURE_HTML_SCRIPT) as? String }
                    .getOrNull()
            )
        }
    }

    fun ensureInitialized(
        onUrlLoaded: (String) -> Unit,
        onLoadingChanged: (Boolean) -> Unit,
        onCanNavigateChanged: (Boolean, Boolean) -> Unit,
        initialUrl: String,
    ) {
        this.onCanNavigateChanged = onCanNavigateChanged
        if (initialized) return
        initialized = true

        Platform.runLater {
            val wv = WebView()
            webView = wv
            // JavaFX identifies itself with a `JavaFX/<n>` token where a browser puts its version,
            // which Google answers with a stream of CAPTCHAs. Present as the Safari build the
            // engine's WebKit corresponds to instead.
            wv.engine.userAgent =
                wv.engine.userAgent.replace(Regex("""JavaFX/\S+"""), "Version/18.0")

            wv.engine.loadWorker.stateProperty().addListener { _, _, newState ->
                when (newState) {
                    Worker.State.RUNNING -> onLoadingChanged(true)
                    Worker.State.SUCCEEDED -> {
                        onLoadingChanged(false)
                        wv.engine.location?.let {
                            lastCommandedUrl = it
                            onUrlLoaded(it)
                        }
                        val history = wv.engine.history
                        this.onCanNavigateChanged?.invoke(
                            history.currentIndex > 0,
                            history.currentIndex < history.entries.size - 1,
                        )
                    }
                    Worker.State.FAILED,
                    Worker.State.CANCELLED -> onLoadingChanged(false)
                    else -> {}
                }
            }

            wv.addEventFilter(MouseEvent.MOUSE_PRESSED) { event ->
                when (event.button) {
                    MouseButton.BACK -> {
                        goBack()
                        event.consume()
                    }
                    MouseButton.FORWARD -> {
                        goForward()
                        event.consume()
                    }
                    else -> {}
                }
            }

            jfxPanel.scene = Scene(wv)

            if (initialUrl.isNotBlank()) {
                wv.engine.load(initialUrl)
            }
        }
    }

    override fun onDestroy() {
        Platform.runLater {
            webView?.engine?.load("about:blank")
            webView = null
        }
    }
}
