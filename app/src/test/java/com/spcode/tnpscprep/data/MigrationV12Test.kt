package com.spcode.tnpscprep.data

import com.spcode.tnpscprep.data.db.Migration11To12
import java.io.File
import java.sql.DriverManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runs the real v11 -> v12 SQL on a real SQLite (test-only sqlite-jdbc), with existing diary entries in place. */
class MigrationV12Test {
    @Test fun existingEntriesSurviveAndNewPartsWork() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { c ->
            c.createStatement().use { st ->
                st.execute("CREATE TABLE diary_entry (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, localDate TEXT NOT NULL, text TEXT NOT NULL, moodEmojiId TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, pinned INTEGER NOT NULL DEFAULT 0)")
                st.execute("INSERT INTO diary_entry (localDate, text, moodEmojiId, createdAt, updatedAt, pinned) VALUES ('2026-11-17', 'இன்று', 'PROUD', 1, 2, 1)")
                Migration11To12.statements.forEach { st.execute(it) }
            }
            c.createStatement().use { st ->
                st.executeQuery("SELECT localDate, text, moodEmojiId, createdAt, updatedAt, pinned, wrapup FROM diary_entry").use { r ->
                    assertTrue(r.next())
                    assertEquals("2026-11-17", r.getString(1)); assertEquals("இன்று", r.getString(2)); assertEquals("PROUD", r.getString(3))
                    assertEquals(1L, r.getLong(4)); assertEquals(2L, r.getLong(5)); assertEquals(1, r.getInt(6)); assertNull(r.getString(7))
                }
                st.execute("INSERT INTO planned_task (localDate, title, refType, refId, done, createdAt, sortOrder) VALUES ('2026-11-18', 'Read Preamble', 'TOPIC', 'x', 0, 1, 0)")
                st.executeQuery("SELECT COUNT(*) FROM planned_task WHERE localDate = '2026-11-18'").use { r -> r.next(); assertEquals(1, r.getInt(1)) }
            }
        }
    }

    /** Guard: our SQL must create exactly what Room's exported schema says, or Room refuses to open the database. */
    @Test fun matchesTheExportedSchema() {
        val f = File("schemas/com.spcode.tnpscprep.data.db.AppDatabase/12.json")
        assertTrue("12.json missing: build the app once so Room exports it", f.exists())
        fun norm(s: String) = s.replace("\\u0060", "`").replace("\\s+".toRegex(), " ").trim()
        val json = norm(f.readText())
        val create = norm(Migration11To12.statements[0]).replace("`planned_task`", "`\${TABLE_NAME}`")
        assertTrue("planned_task table differs from 12.json", json.contains(create))
        val index = norm(Migration11To12.statements[1]).replace("ON `planned_task`", "ON `\${TABLE_NAME}`")
        assertTrue("planned_task index differs from 12.json", json.contains(index))
        assertTrue("wrapup column missing from 12.json", json.contains("`pinned` INTEGER NOT NULL DEFAULT 0, `wrapup` TEXT)"))
    }
}
