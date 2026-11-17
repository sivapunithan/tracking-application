package com.spcode.tnpscprep.data.news

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Where feed text comes from. An interface so the refresh logic can be tested without a network. */
interface FeedSource {
    suspend fun fetch(url: String): String
}

/**
 * Plain HttpsURLConnection (no extra library: keeps the APK small). Defensive on purpose:
 * https only, host allow-list (also for every redirect), short timeouts, 2 MB size cap, no cookies, no credentials.
 */
class HttpFeedSource(private val allowedHosts: Set<String>) : FeedSource {
    override suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        var current = url
        repeat(MAX_REDIRECTS + 1) {
            if (!FeedPolicy.isAllowed(current, allowedHosts)) throw IOException("Blocked URL (not https or host not allowed)")
            val conn = URL(current).openConnection() as HttpsURLConnection
            try {
                conn.instanceFollowRedirects = false // we follow manually so every hop is checked against the allow-list
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.requestMethod = "GET"
                conn.useCaches = false
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml;q=0.9, */*;q=0.5")
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location") ?: throw IOException("Redirect without Location")
                    current = URL(URL(current), loc).toString()
                    return@repeat
                }
                if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
                val charset = Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE)
                    .find(conn.contentType.orEmpty())?.groupValues?.get(1)
                    ?.let { runCatching { charset(it) }.getOrNull() } ?: Charsets.UTF_8
                return@withContext conn.inputStream.use { readLimited(it, MAX_BYTES) }.toString(charset)
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("Too many redirects")
    }

    private fun readLimited(input: java.io.InputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            if (out.size() + n > limit) throw IOException("Feed larger than $limit bytes")
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private companion object {
        const val MAX_REDIRECTS = 3
        const val MAX_BYTES = 2 * 1024 * 1024
        // Generic client identity: says nothing about the user, the device or this project.
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android) FeedReader/1.0"
    }
}
