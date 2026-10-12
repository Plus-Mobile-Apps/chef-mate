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

    /** Address-bar overflow button that opens the page actions menu. */
    const val OVERFLOW_MENU = "BrowserOverflowMenu"

    /** Overflow menu item that stars or un-stars the current page. */
    const val TOGGLE_BOOKMARK = "BrowserToggleBookmark"

    /** Address-bar button that returns to the landing screen. */
    const val HOME = "BrowserHome"

    /** Overflow menu item that goes forward in the page history. */
    const val FORWARD = "BrowserForward"

    /** The bookmarks section on the landing screen. */
    const val LANDING_BOOKMARKS = "BrowserLandingBookmarks"

    /** A single bookmark tile on the landing screen; suffix with the bookmark's id. */
    const val BOOKMARK_TILE_PREFIX = "BrowserBookmarkTile_"

    /** A bookmark tile's options button; suffix with the bookmark's id. */
    const val BOOKMARK_OPTIONS_PREFIX = "BrowserBookmarkOptions_"

    /** The delete item in a bookmark tile's options menu. */
    const val BOOKMARK_DELETE = "BrowserBookmarkDelete"

    /** The settings screen that manages bookmarks. */
    const val BOOKMARKS_SCREEN = "BrowserBookmarksScreen"

    /** A row on the bookmarks settings screen; suffix with the bookmark's id. */
    const val BOOKMARK_ROW_PREFIX = "BrowserBookmarkRow_"

    /** Selection-mode header action that selects (or deselects) every bookmark. */
    const val BOOKMARKS_SELECT_ALL = "BrowserBookmarksSelectAll"

    /** Selection-mode header action that deletes the selected bookmarks. */
    const val BOOKMARKS_DELETE_SELECTED = "BrowserBookmarksDeleteSelected"

    fun engineOption(engine: SearchEngine): String = "$ENGINE_OPTION_PREFIX${engine.id}"

    fun bookmarkTile(id: Long): String = "$BOOKMARK_TILE_PREFIX$id"

    fun bookmarkOptions(id: Long): String = "$BOOKMARK_OPTIONS_PREFIX$id"

    fun bookmarkRow(id: Long): String = "$BOOKMARK_ROW_PREFIX$id"
}
