package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropMathTest {
    @Test fun squareFrameOnLandscapePictureStartsCentred() {
        val s = CropMath.minScale(2000, 1000, 500f, 500f)
        assertEquals(0.5f, s, 0.0001f)
        assertEquals(CropMath.Rect(500, 0, 1000, 1000), CropMath.sourceRect(2000, 1000, 500f, 500f, s, 0f, 0f))
    }
    @Test fun draggingMovesTheWindowButNeverOutsideThePicture() {
        val s = 0.5f
        val off = CropMath.clampOffset(10_000f, 2000, s, 500f)   // far too far right
        assertEquals(250f, off, 0.001f)
        val r = CropMath.sourceRect(2000, 1000, 500f, 500f, s, off, 0f)
        assertEquals(0, r.x); assertTrue(r.x + r.w <= 2000)
    }
    @Test fun zoomingInShrinksTheWindow() {
        val r = CropMath.sourceRect(2000, 1000, 500f, 500f, 1f, 0f, 0f)
        assertEquals(500, r.w); assertEquals(500, r.h)
    }
}
