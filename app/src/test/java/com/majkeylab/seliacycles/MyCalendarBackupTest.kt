package com.majkeylab.seliacycles

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MyCalendarBackupTest {
    @Test
    fun readsVerifiedCloudDatabase() {
        val database = sqliteBytes("period rows")

        val result = MyCalendarContainerReader.read(ByteArrayInputStream(fixture("cloud.db" to database)))

        assertContentEquals(database, result.database)
        assertEquals("24", result.generation)
    }

    @Test
    fun rejectsMissingCloudDatabase() {
        assertFailsWith<MyCalendarFormatException> {
            MyCalendarContainerReader.read(ByteArrayInputStream(fixture("1.info" to "metadata".encodeToByteArray())))
        }
    }

    @Test
    fun rejectsDuplicateNormalizedEntry() {
        assertFailsWith<MyCalendarFormatException> {
            MyCalendarContainerReader.read(ByteArrayInputStream(fixture(
                "first/cloud.db" to sqliteBytes("one"),
                "second/cloud.db" to sqliteBytes("two"),
            )))
        }
    }

    @Test
    fun rejectsOversizedDatabase() {
        assertFailsWith<MyCalendarFormatException> {
            MyCalendarContainerReader.read(ByteArrayInputStream(fixture(
                "cloud.db" to ByteArray(MyCalendarContainerReader.MAX_DATABASE_BYTES + 1),
            )))
        }
    }

    @Test
    fun rejectsInvalidDatabaseHeader() {
        assertFailsWith<MyCalendarFormatException> {
            MyCalendarContainerReader.read(ByteArrayInputStream(fixture("cloud.db" to "not sqlite".encodeToByteArray())))
        }
    }

    private fun fixture(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { output ->
            output.writeInt(-1)
            output.writeInt(1)
            output.writeInt(0)
            ZipOutputStream(output).use { zip ->
                val allEntries = listOf("1.generation" to "24".encodeToByteArray()) + entries
                allEntries.forEach { (name, value) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(value)
                    zip.closeEntry()
                }
            }
        }
        return bytes.toByteArray()
    }

    private fun sqliteBytes(body: String): ByteArray = "SQLite format 3\u0000$body".encodeToByteArray()
}
