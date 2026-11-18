package com.spcode.tnpscprep.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MathiBundleTest {
    private val a = "0123456789ab"; private val b = "ba9876543210"

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val o = ByteArrayOutputStream()
        ZipOutputStream(o).use { z -> entries.forEach { (n, d) -> z.putNextEntry(ZipEntry(n)); z.write(d); z.closeEntry() } }
        return o.toByteArray()
    }

    @Test fun roundTripJsonAndPhotos() {
        val out = ByteArrayOutputStream()
        val n = MathiBundle.write(out, """{"app":"TnpscPrep"}""", listOf(a, b, "ffffffffffff")) { name -> if (name == "ffffffffffff") null else ByteArrayInputStream(name.toByteArray()) }
        assertEquals(2, n)
        val bytes = out.toByteArray()
        assertTrue(MathiBundle.looksLikeZip(bytes))
        val r = MathiBundle.Reader(ByteArrayInputStream(bytes))
        assertEquals("""{"app":"TnpscPrep"}""", r.json())
        val got = generateSequence { r.nextPhoto() }.toList()
        assertEquals(listOf(a to a, b to b), got.map { it.first to String(it.second) })
    }

    @Test fun plainJsonIsNotAZip() { assertTrue(!MathiBundle.looksLikeZip("{\"app\"".toByteArray())) }

    @Test fun jsonMustBeFirst() {
        val r = MathiBundle.Reader(ByteArrayInputStream(zip("photos/$a.webp" to byteArrayOf(1), "backup.json" to "{}".toByteArray())))
        assertNull(r.json())
    }

    @Test fun badEntryNamesAreIgnoredAndNeverUsedAsPaths() {
        val r = MathiBundle.Reader(ByteArrayInputStream(zip(
            "backup.json" to "{}".toByteArray(),
            "photos/../../evil.webp" to byteArrayOf(9), "photos/ABCDEF123456.webp" to byteArrayOf(9), "other/$a.webp" to byteArrayOf(9),
            "photos/$a.png" to byteArrayOf(9), "photos/$b.webp" to byteArrayOf(7),
        )))
        assertEquals("{}", r.json())
        val p = r.nextPhoto()!!
        assertEquals(b, p.first); assertNull(r.nextPhoto())
    }

    @Test fun oversizePhotoIsSkippedNotFatal() {
        val big = ByteArray(MathiBundle.MAX_PHOTO_BYTES + 1)
        val r = MathiBundle.Reader(ByteArrayInputStream(zip("backup.json" to "{}".toByteArray(), "photos/$a.webp" to big, "photos/$b.webp" to byteArrayOf(1, 2))))
        r.json()
        assertEquals(b, r.nextPhoto()!!.first); assertNull(r.nextPhoto())
    }

    @Test fun whichPhotosComeBack() {
        val inc = listOf(PhotoDto(a, "HEART", 0, 800, 800, 1), PhotoDto(b, "ROUNDED", 16, 800, 600, 2), PhotoDto("111111111111", "STAR", 0, 5, 5, 3))
        val r = AppBackup.photosToRestore(localNames = setOf(b), zipNames = setOf(a, b), incoming = inc) { n -> if (n == a) 7L else null }
        assertEquals(listOf(a to 7L), r.map { it.first.name to it.second })   // b is already here, the third is not in the zip
    }

    @Test fun photoSectionIsValidated() {
        val ok = FullBackupFile(photos = listOf(PhotoDto(a, "HEART", 0, 800, 800, 1)))
        assertTrue(AppBackup.parse(AppBackup.encode(ok)) is AppBackup.Parsed.Ok)
        listOf(PhotoDto("../x", "HEART", 0, 8, 8, 1), PhotoDto(a, "HEART", 0, 0, 8, 1), PhotoDto(a, "HEART", 500, 8, 8, 1), PhotoDto(a, "X".repeat(40), 0, 8, 8, 1))
            .forEach { assertTrue(AppBackup.parse(AppBackup.encode(FullBackupFile(photos = listOf(it)))) is AppBackup.Parsed.Bad) }
    }
}
