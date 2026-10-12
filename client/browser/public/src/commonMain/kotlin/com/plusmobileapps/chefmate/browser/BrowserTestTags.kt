package com.plusmobileapps.chefmate.browser

object BrowserTestTags {
    const val LANDING_SCREEN = "BrowserLandingScreen"
    const val SELECT_ENGINE_SCREEN = "BrowserSelectEngineScreen"

    /** Row for a single engine in the picker; suffix with [SearchEngine.id]. */
    const val ENGINE_OPTION_PREFIX = "BrowserEngineOption_"

    /** The engine dropdown control on the landing screen. */
    const val LANDING_ENGINE_DROPDOWN = "BrowserLandingEngineDropdown"

    /** Landing button that opens the selected engine's own front page. */
    const val LANDING_OPEN_ENGINE_HOME = "BrowserLandingOpenEngineHome"

    /** Address-bar action that hands the current page to the device's default browser. */
    const val OPEN_IN_DEFAULT_BROWSER = "BrowserOpenInDefaultBrowser"

    fun engineOption(engine: SearchEngine): String = "$ENGINE_OPTION_PREFIX${engine.id}"
}
