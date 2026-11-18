package com.spcode.tnpscprep.domain

/** Crop maths (pure). The crop FRAME is fixed on screen; the picture is dragged and zoomed under it. All sizes in the same pixel unit. */
object CropMath {
    data class Rect(val x: Int, val y: Int, val w: Int, val h: Int)

    /** Smallest scale at which the picture still covers the whole frame. */
    fun minScale(imgW: Int, imgH: Int, frameW: Float, frameH: Float): Float = maxOf(frameW / imgW, frameH / imgH)

    /** Keeps the picture covering the frame: [off] is the picture centre's shift from the frame centre. */
    fun clampOffset(off: Float, imgSide: Int, scale: Float, frameSide: Float): Float {
        val limit = maxOf(0f, (imgSide * scale - frameSide) / 2f)
        return off.coerceIn(-limit, limit)
    }

    /** The part of the SOURCE picture under the frame, always inside the picture. */
    fun sourceRect(imgW: Int, imgH: Int, frameW: Float, frameH: Float, scale: Float, offX: Float, offY: Float): Rect {
        val w = (frameW / scale).coerceAtMost(imgW.toFloat())
        val h = (frameH / scale).coerceAtMost(imgH.toFloat())
        val x = (imgW / 2f - w / 2f - offX / scale).coerceIn(0f, imgW - w)
        val y = (imgH / 2f - h / 2f - offY / scale).coerceIn(0f, imgH - h)
        return Rect(Math.round(x), Math.round(y), maxOf(1, Math.round(w)), maxOf(1, Math.round(h)))
    }
}
