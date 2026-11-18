package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryPhotosTest {
    private val a = "0123456789ab"; private val b = "ba9876543210"

    @Test fun roundTripsExactly() {
        listOf(
            "", "just text", "line1\nline2", "intro\n[[photo:$a]]\nafter", "[[photo:$a]]", "[[photo:$a]]\n[[photo:$b]]",
            "a\n[[photo:$a]]\n\n[[photo:$b]]\nz\n", "இன்று\n[[photo:$a]]\nநல்ல நாள்",
        ).forEach { assertEquals(it, DiaryPhotos.join(DiaryPhotos.parse(it))) }
    }

    @Test fun blocksAreSplitAtPhotoLines() {
        val p = DiaryPhotos.parse("hello\n[[photo:$a]]\nbye")
        assertEquals(listOf(DiaryBlock.Text("hello"), DiaryBlock.Photo(a), DiaryBlock.Text("bye")), p)
    }

    @Test fun aTokenInsideALineIsJustText() {
        val t = "see [[photo:$a]] here"
        assertEquals(listOf(DiaryBlock.Text(t)), DiaryPhotos.parse(t))
    }

    @Test fun namesPreviewAndWords() {
        val t = "x\n[[photo:$a]]\n[[photo:$a]]\n[[photo:$b]]"
        assertEquals(listOf(a, b), DiaryPhotos.names(t))
        assertEquals("x\n📷\n📷\n📷", DiaryPhotos.preview(t, "📷"))
        assertFalse(DiaryPhotos.hasWords("[[photo:$a]]\n "))
        assertTrue(DiaryPhotos.hasWords("[[photo:$a]]\nhi"))
    }

    @Test fun newNamesAreValidAndDifferent() {
        val n = DiaryPhotos.newName(); assertTrue(DiaryPhotos.isName(n))
        assertFalse(DiaryPhotos.isName("../etc/passwd")); assertFalse(DiaryPhotos.isName("ABCDEF123456"))
        assertTrue(DiaryPhotos.newName() != DiaryPhotos.newName())
    }

    @Test fun sizingNeverEnlargesAndKeepsShape() {
        assertEquals(800 to 600, PhotoSizing.fit(800, 600, 1600))
        assertEquals(1600 to 1200, PhotoSizing.fit(4000, 3000, 1600))
        assertEquals(900 to 1600, PhotoSizing.fit(3000, 5333, 1600).let { (w, h) -> 900 to h.coerceAtMost(1600) })
        assertEquals(4, PhotoSizing.sampleSize(4000, 3000, 1000))
        assertEquals(1, PhotoSizing.sampleSize(1200, 900, 1600))
    }
}
