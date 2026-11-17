package com.spcode.tnpscprep.data.news

import java.net.URI

/**
 * The only network rule-book in the app. Everything fetched is untrusted, so:
 *  - https only (also enforced by the manifest's network security config),
 *  - a feed URL (and any redirect target) must be on a host listed in feeds.json,
 *  - article links we STORE only need to be https (they are merely opened in her browser when she taps them).
 */
object FeedPolicy {
    fun hostOf(url: String): String? = try {
        URI(url.trim()).takeIf { it.scheme.equals("https", ignoreCase = true) }?.host?.lowercase()
    } catch (_: Exception) { null }

    /** Exact host or a real subdomain of an allowed host. "evilthehindu.com" and "thehindu.com.evil.org" do NOT match. */
    fun isAllowed(url: String, allowedHosts: Set<String>): Boolean {
        val host = hostOf(url) ?: return false
        return allowedHosts.any { a -> host == a || host.endsWith(".$a") }
    }

    fun isHttps(url: String): Boolean = hostOf(url) != null
}
