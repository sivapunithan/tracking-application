package com.spcode.tnpscprep.data

import com.spcode.tnpscprep.data.db.Migration12To13
import java.io.File
import java.sql.DriverManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationV13Test {
    @Test fun existingEntriesSurviveAndPhotoRowsWork() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { c ->
            c.createStatement().use { st ->
                st.execute("CREATE TABLE diary_entry (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, localDate TEXT NOT NULL, text TEXT NOT NULL, moodEmojiId TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, pinned INTEGER NOT NULL DEFAULT 0, wrapup TEXT)")
                st.execute("INSERT INTO diary_entry (localDate, text, createdAt, updatedAt) VALUES ('2026-11-17', 'இன்று', 1, 2)")
                Migration12To13.statements.forEach { st.execute(it) }
                st.executeQuery("SELECT text FROM diary_entry").use { r -> assertTrue(r.next()); assertEquals("இன்று", r.getString(1)) }
                st.execute("INSERT INTO diary_photo VALUES ('0123456789ab', 1, 'ROUNDED', 16, 800, 600, 120000, 5)")
                st.executeQuery("SELECT COUNT(*) FROM diary_photo WHERE entryId = 1").use { r -> r.next(); assertEquals(1, r.getInt(1)) }
            }
        }
    }

    @Test fun matchesTheExportedSchema() {
        val f = File("schemas/com.spcode.tnpscprep.data.db.AppDatabase/13.json")
        assertTrue("13.json missing: build the app once so Room exports it", f.exists())
        fun norm(s: String) = s.replace("\\u0060", "`").replace("\\s+".toRegex(), " ").trim()
        val json = norm(f.readText())
        assertTrue(json.contains(norm(Migration12To13.statements[0]).replace("`diary_photo`", "`\${TABLE_NAME}`")))
        assertTrue(json.contains(norm(Migration12To13.statements[1]).replace("ON `diary_photo`", "ON `\${TABLE_NAME}`")))
    }
}
