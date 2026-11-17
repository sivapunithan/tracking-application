package com.spcode.tnpscprep.data.news

import java.io.StringReader
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class RawFeedItem(val title: String, val link: String, val published: Instant?)

class FeedParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Reads RSS 2.0 and Atom. Deliberately small and defensive: the XML comes from the internet.
 *  - A DOCTYPE/ENTITY declaration is rejected outright (no external entities, no "billion laughs").
 *  - Unknown named entities (e.g. &nbsp;) are neutralised instead of crashing the whole feed.
 *  - Elements are read by name; anything unexpected is skipped.
 */
object FeedParser {
    private const val MAX_ITEMS = 500
    private val unknownEntity = Regex("&(?!amp;|lt;|gt;|quot;|apos;|#)[A-Za-z][A-Za-z0-9]*;")

    fun parse(xml: String): List<RawFeedItem> {
        val text = xml.trimStart('﻿', ' ', '\n', '\r', '\t')
        if (text.contains("<!DOCTYPE", ignoreCase = true) || text.contains("<!ENTITY", ignoreCase = true)) {
            throw FeedParseException("DOCTYPE/ENTITY not allowed")
        }
        val safe = unknownEntity.replace(text, " ")
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(StringReader(safe))
            val items = ArrayList<RawFeedItem>()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT && items.size < MAX_ITEMS) {
                if (event == XmlPullParser.START_TAG && (parser.name == "item" || parser.name == "entry")) {
                    readItem(parser)?.let { items += it }
                }
                event = parser.next()
            }
            return items
        } catch (e: FeedParseException) {
            throw e
        } catch (e: Exception) { // XmlPullParserException, IOException, ...
            throw FeedParseException("Malformed feed: ${e.message}", e)
        }
    }

    private fun readItem(p: XmlPullParser): RawFeedItem? {
        val endName = p.name
        var title = ""
        var link = ""
        var date: String? = null
        while (true) {
            val ev = p.next()
            if (ev == XmlPullParser.END_DOCUMENT) break
            if (ev == XmlPullParser.END_TAG && p.name == endName) break
            if (ev != XmlPullParser.START_TAG) continue
            when (p.name) {
                "title" -> title = readText(p)
                "link" -> {
                    // Atom: <link href="..."/>   RSS: <link>url</link>
                    val href = p.getAttributeValue(null, "href")
                    val rel = p.getAttributeValue(null, "rel")
                    val body = readText(p)
                    if (!href.isNullOrBlank() && (rel == null || rel == "alternate")) link = href.trim()
                    else if (body.isNotBlank() && link.isEmpty()) link = body.trim()
                }
                "pubDate", "published", "updated", "date" -> if (date == null) date = readText(p).trim()
                else -> skip(p)
            }
        }
        if (title.isBlank() || link.isBlank()) return null
        return RawFeedItem(title.trim(), link.trim(), date?.let(::parseDate))
    }

    /** Text (incl. CDATA) of the current element; nested elements are skipped. Leaves the parser on the END_TAG. */
    private fun readText(p: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (p.next()) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (depth == 1) sb.append(p.text)
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return sb.toString()
            }
        }
        return sb.toString()
    }

    private fun skip(p: XmlPullParser) {
        var depth = 1
        while (depth > 0) {
            when (p.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }

    /** RFC 1123 ("Tue, 17 Nov 2026 15:42:10 +0530" / "... GMT") or ISO 8601; null if it is neither. */
    fun parseDate(text: String): Instant? {
        val t = text.trim()
        return try { ZonedDateTime.parse(t, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() } catch (_: Exception) {
            try { OffsetDateTime.parse(t).toInstant() } catch (_: Exception) {
                try { Instant.parse(t) } catch (_: Exception) { null }
            }
        }
    }
}
