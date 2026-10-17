package com.plusmobileapps.chefmate

import com.arkivanov.decompose.ComponentContext
import com.plusmobileapps.chefmate.di.IosApplicationComponent
import com.plusmobileapps.chefmate.review.InAppReviewService
import com.plusmobileapps.chefmate.root.DeepLink
import com.plusmobileapps.chefmate.root.RootBloc
import com.plusmobileapps.chefmate.toast.ToastService
import dev.zacsweers.metro.createGraphFactory
import platform.UIKit.UIApplication

object RootBlocProvider {
    /**
     * The app-scoped [ToastService] from the same graph that built the [RootBloc]. Set by
     * [buildRootBloc] (always called before the UI is created) and read by [MainViewController] so
     * the toast host observes the very instance BLoCs enqueue onto. Threaded this way to avoid
     * widening the Swift glue signatures.
     */
    lateinit var toastService: ToastService
        private set

    /**
     * The app-scoped [InAppReviewService], threaded to [MainViewController] the same way as
     * [toastService] so the review host observes the instance ViewModels report milestones to.
     */
    lateinit var inAppReviewService: InAppReviewService
        private set

    /**
     * Bridge that serves grocery data to the watch companion and applies its actions over
     * WatchConnectivity — the phone is the source of truth; the watch holds no Kotlin/Supabase. Set
     * by [buildRootBloc] (always called before the UI is created) and read by the Swift
     * `WatchDataSender`.
     */
    lateinit var watchDataBridge: WatchDataBridge
        private set

    /**
     * Non-throwing view of [watchDataBridge] for callers that may run before the graph is built —
     * e.g. the Siri "add grocery item" App Intent, which can background-launch the app. Null until
     * [buildRootBloc] has run; reading [watchDataBridge] directly in that window would crash.
     */
    val watchDataBridgeOrNull: WatchDataBridge?
        get() = if (::watchDataBridge.isInitialized) watchDataBridge else null

    fun buildRootBloc(
        componentContext: ComponentContext,
        application: UIApplication,
        deepLinkUrl: String? = null,
    ): RootBloc {
        val applicationComponent =
            createGraphFactory<IosApplicationComponent.Factory>().create(application)
        toastService = applicationComponent.toastService
        inAppReviewService = applicationComponent.inAppReviewService
        watchDataBridge = applicationComponent.watchDataBridge
        return applicationComponent.rootBlocFactory.create(
            context = DefaultBlocContext(componentContext = componentContext),
            deepLink = DeepLink.parse(deepLinkUrl),
        )
    }
}
