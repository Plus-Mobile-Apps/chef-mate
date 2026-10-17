package com.plusmobileapps.chefmate.browser.impl

/**
 * The form a bookmarked URL is stored and matched in, so the browser's star reflects the page
 * whether or not the WebView reports it with an in-page anchor or stray whitespace.
 */
internal object BookmarkUrl {
    fun normalize(rawUrl: String): String? =
        rawUrl.trim().substringBefore('#').takeIf { it.isNotEmpty() }

    /** The URL's host without a leading `www.`, used as the fallback bookmark title. */
    fun host(url: String): String =
        url.substringAfter("://")
            .substringBefore('/')
            .substringBefore('?')
            .removePrefix("www.")
            .ifEmpty { url }
}
