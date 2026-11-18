package com.spcode.tnpscprep.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.media.ExifInterface
import com.spcode.tnpscprep.domain.PhotoSizing
import java.io.File

/**
 * Diary pictures as files in the app's PRIVATE storage (not the phone gallery, not readable by other apps, not in any cloud backup:
 * allowBackup=false). Each photo is two small WebP files: the display copy (long edge <= 1600 px) and a thumbnail (<= 320 px) for the timeline.
 * A 12 MB camera photo therefore costs about 0.2 MB on disk and never gets decoded at full size.
 */
class PhotoStore(context: Context) {
    private val dir = File(context.applicationContext.filesDir, "diary_photos").apply { mkdirs() }
    private val resolver = context.applicationContext.contentResolver

    fun file(name: String, thumb: Boolean = false) = File(dir, if (thumb) "${name}_t.webp" else "$name.webp")
    fun exists(name: String) = file(name).exists()

    /** Decodes [uri] already shrunk (never the full camera bitmap), upright by its EXIF turn. Null if it is not a readable picture. */
    fun decode(uri: Uri, maxEdge: Int = PhotoSizing.MAX_FULL): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply { inSampleSize = PhotoSizing.sampleSize(bounds.outWidth, bounds.outHeight, maxEdge) }
        val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val turn = resolver.openInputStream(uri)?.use { s ->
            when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f; ExifInterface.ORIENTATION_ROTATE_180 -> 180f; ExifInterface.ORIENTATION_ROTATE_270 -> 270f; else -> 0f
            }
        } ?: 0f
        val upright = if (turn != 0f) Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(turn) }, true).also { if (it !== raw) raw.recycle() } else raw
        scaleTo(upright, maxEdge)
    }.getOrNull()

    /** Writes the display copy and the thumbnail of [bitmap] (already cropped by the caller, if she cropped). Returns the saved size, or null on failure. */
    fun write(name: String, bitmap: Bitmap): Saved? = runCatching {
        val full = scaleTo(bitmap, PhotoSizing.MAX_FULL)
        val thumb = scaleTo(bitmap, PhotoSizing.MAX_THUMB)
        compress(full, file(name), 80); compress(thumb, file(name, true), 75)
        Saved(full.width, full.height, file(name).length() + file(name, true).length())
    }.getOrNull()

    /** Brings a photo back from a backup: [bytes] must decode as a picture (long edge <= 8000); the file is kept as it is and a thumbnail is rebuilt. */
    fun importBytes(name: String, bytes: ByteArray): Saved? = runCatching {
        if (!com.spcode.tnpscprep.domain.DiaryPhotos.isName(name)) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || maxOf(bounds.outWidth, bounds.outHeight) > 8000) return null
        val small = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = PhotoSizing.sampleSize(bounds.outWidth, bounds.outHeight, PhotoSizing.MAX_THUMB) }) ?: return null
        file(name).writeBytes(bytes)
        compress(scaleTo(small, PhotoSizing.MAX_THUMB), file(name, true), 75)
        Saved(bounds.outWidth, bounds.outHeight, file(name).length() + file(name, true).length())
    }.getOrNull()

    fun delete(name: String) { file(name).delete(); file(name, true).delete() }

    /** Removes every file that no entry refers to any more (called after deletes / restores). Returns how many photos were removed. */
    fun deleteExcept(keep: Set<String>): Int {
        var n = 0
        dir.listFiles()?.forEach { f ->
            val base = f.name.removeSuffix("_t.webp").removeSuffix(".webp")
            if (base !in keep) { f.delete(); if (!f.name.endsWith("_t.webp")) n++ }
        }
        return n
    }

    data class Saved(val width: Int, val height: Int, val bytes: Long)

    private fun scaleTo(b: Bitmap, max: Int): Bitmap {
        val (w, h) = PhotoSizing.fit(b.width, b.height, max)
        return if (w == b.width && h == b.height) b else Bitmap.createScaledBitmap(b, w, h, true)
    }

    @Suppress("DEPRECATION")
    private fun compress(b: Bitmap, f: File, q: Int) {
        val fmt = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        f.outputStream().use { b.compress(fmt, q, it) }
    }
}
