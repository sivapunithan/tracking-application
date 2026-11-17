package com.spcode.tnpscprep.data.news

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FeedParserTest {
    private fun fixture(name: String) = javaClass.getResource("/feeds/$name.xml")!!.readText()

    @Test fun real_the_hindu_sample_parses_cdata_titles_links_and_offset_dates() {
        val items = FeedParser.parse(fixture("hindu-national"))
        assertEquals(4, items.size)
        val first = items.first()
        assertTrue(first.title.isNotBlank())
        assertTrue(first.link.startsWith("https://www.thehindu.com/"))
        assertNotNull(first.published)
        assertTrue(items.all { it.published!!.isAfter(Instant.parse("2026-01-01T00:00:00Z")) })
    }

    @Test fun real_indian_express_sample_parses() {
        val items = FeedParser.parse(fixture("indian-express"))
        assertEquals(4, items.size)
        assertTrue(items.all { it.link.startsWith("https://indianexpress.com/") && it.published != null })
    }

    @Test fun real_bbc_tamil_sample_keeps_tamil_text() {
        val items = FeedParser.parse(fixture("bbc-tamil"))
        assertEquals(4, items.size)
        assertTrue(items.any { t -> t.title.any { it in '஀'..'௿' } }) // Tamil block
    }

    @Test fun the_hindu_tamil_nadu_sample_parses() = assertEquals(4, FeedParser.parse(fixture("hindu-tn")).size)

    @Test fun atom_feeds_use_the_href_attribute_and_iso_dates() {
        val xml = """<?xml version="1.0"?><feed xmlns="http://www.w3.org/2005/Atom"><title>x</title>
            <entry><title>Atom headline</title><link rel="alternate" href="https://example.org/a1"/>
            <updated>2026-11-17T10:00:00+05:30</updated></entry></feed>"""
        val i = FeedParser.parse(xml).single()
        assertEquals("Atom headline", i.title)
        assertEquals("https://example.org/a1", i.link)
        assertEquals(Instant.parse("2026-11-17T04:30:00Z"), i.published)
    }

    @Test fun items_without_title_or_link_are_skipped() {
        val xml = """<rss><channel><item><title>No link</title></item><item><link>https://e.org/1</link></item>
            <item><title>Good</title><link>https://e.org/2</link></item></channel></rss>"""
        assertEquals(listOf("Good"), FeedParser.parse(xml).map { it.title })
    }

    @Test fun missing_date_is_null_not_a_crash() {
        val xml = """<rss><channel><item><title>T</title><link>https://e.org/1</link></item></channel></rss>"""
        assertNull(FeedParser.parse(xml).single().published)
    }

    @Test fun byte_order_mark_and_unknown_html_entities_do_not_break_parsing() {
        val xml = "﻿<rss><channel><item><title>Fish&nbsp;&amp;&nbsp;chips &#8217;ok</title><link>https://e.org/1</link></item></channel></rss>"
        val t = FeedParser.parse(xml).single().title
        assertTrue(t.contains("& ") || t.contains("&")); assertTrue(t.contains("chips"))
    }

    @Test fun doctype_and_entity_declarations_are_rejected() {
        val bomb = """<?xml version="1.0"?><!DOCTYPE lolz [<!ENTITY a "aaaa">]><rss><channel><item><title>&a;</title><link>https://e.org</link></item></channel></rss>"""
        try { FeedParser.parse(bomb); fail("expected FeedParseException") } catch (_: FeedParseException) { }
    }

    @Test fun garbage_is_a_parse_exception_not_a_crash() {
        try { FeedParser.parse("<html><body>blocked</body></html> <<<"); fail() } catch (_: FeedParseException) { }
    }

    @Test fun an_html_error_page_yields_no_items() {
        assertTrue(FeedParser.parse("<html><body><p>Access denied</p></body></html>").isEmpty())
    }

    @Test fun date_formats() {
        assertEquals(Instant.parse("2026-11-17T10:12:10Z"), FeedParser.parseDate("Tue, 17 Nov 2026 15:42:10 +0530"))
        assertEquals(Instant.parse("2026-11-16T09:17:49Z"), FeedParser.parseDate(" Mon, 16 Nov 2026 09:17:49 GMT "))
        assertNull(FeedParser.parseDate("yesterday"))
    }
}
