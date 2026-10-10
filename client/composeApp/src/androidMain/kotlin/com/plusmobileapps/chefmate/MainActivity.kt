package com.plusmobileapps.chefmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.plusmobileapps.chefmate.root.DeepLink
import com.plusmobileapps.chefmate.root.RootBloc

class MainActivity : ComponentActivity() {
    private lateinit var rootBloc: RootBloc

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appComponent = (application as MyApplication).appComponent

        rootBloc =
            buildRootBloc(
                componentContext = defaultComponentContext(),
                applicationComponent = appComponent,
                deepLink = parseDeepLink(intent),
            )
        setContent {
            App(rootBloc, appComponent.toastService, appComponent.inAppReviewService)
        }

        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // A warm app (launchMode=singleTask) receives tapped deep links here rather than through a
        // fresh onCreate, so route them to the running RootBloc.
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.toString()?.let { rootBloc.handleDeepLink(it) }
        }
        handleShareIntent(intent)
    }

    private fun parseDeepLink(intent: Intent?): DeepLink {
        if (intent?.action != Intent.ACTION_VIEW) return DeepLink.None
        val data = intent.data?.toString() ?: return DeepLink.None
        return DeepLink.parse(data)
    }

    private fun handleShareIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim() ?: return
            val url = sharedText.lines().firstOrNull { it.startsWith("http") } ?: sharedText
            if (url.startsWith("http")) {
                rootBloc.handleSharedUrl(url)
            }
        }
    }
}
