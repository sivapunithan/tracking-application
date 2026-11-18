package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiaryLinksTest {
    @Test fun findsLinks() {
        val t = "see [my site](https://a.com/x?y=1) and [b](http://b.org) ok"
        val l = DiaryLinks.find(t)
        assertEquals(listOf("my site" to "https://a.com/x?y=1", "b" to "http://b.org"), l.map { it.label to it.url })
        assertEquals("[my site](https://a.com/x?y=1)", t.substring(l[0].start, l[0].end))
    }
    @Test fun onlyWebAddressesAreAccepted() {
        assertEquals("https://example.com", DiaryLinks.normalizeUrl("example.com"))
        assertEquals("https://x.in/a", DiaryLinks.normalizeUrl("  https://x.in/a "))
        assertEquals("http://x.in", DiaryLinks.normalizeUrl("http://x.in"))
        listOf("javascript:alert(1)", "intent://x#Intent;end", "file:///etc/passwd", "data:text/html,hi", "ftp://x.com", "hello", "a b.com", "", "tel:123").forEach { assertNull(it, DiaryLinks.normalizeUrl(it)) }
    }
    @Test fun makeCleansTheLabelAndEscapesParens() {
        assertEquals("[a(b)c](https://x.com/p%29)", DiaryLinks.make("a[b]c", "x.com/p)"))
        assertNull(DiaryLinks.make("   ", "x.com")); assertNull(DiaryLinks.make("ok", "javascript:1"))
    }
    @Test fun displayShowsOnlyTheLabelAndMapsOffsets() {
        val t = "go [web](https://w.com) now"
        val d = DiaryLinks.display(t)
        assertEquals("go web now", d.text)
        assertEquals(1, d.links.size); assertEquals(3, d.links[0].tStart); assertEquals(6, d.links[0].tEnd)
        assertEquals(0, d.storedOffset(0)); assertEquals(3, d.storedOffset(3))          // before the link
        assertEquals(5, d.storedOffset(4))                                              // inside the label (between w and e)
        assertEquals(t.indexOf("]"), d.storedOffset(6))                                 // end of label = before "]": typing extends the label
        assertEquals(t.length, d.storedOffset(d.text.length))
        assertEquals(d.text.length, d.shownOffset(t.length)); assertEquals(6, d.shownOffset(t.indexOf("]") + 3)) // inside the hidden part = end of label
        assertEquals(7, d.shownOffset(d.storedOffset(7)))                              // the char after the link round-trips
    }
    @Test fun noLinksIsIdentity() {
        val d = DiaryLinks.display("plain text")
        assertEquals("plain text", d.text); (0..10).forEach { assertEquals(it, d.storedOffset(it)); assertEquals(it, d.shownOffset(it)) }
    }
    @Test fun previewKeepsOnlyLabels() { assertEquals("a b c", DiaryLinks.preview("a [b](https://x.com) c")) }
}
