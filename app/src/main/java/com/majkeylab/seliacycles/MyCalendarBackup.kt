package com.majkeylab.seliacycles

import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

class MyCalendarFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class MyCalendarContainer(val database: ByteArray, val generation: String?)

object MyCalendarContainerReader {
    const val MAX_DATABASE_BYTES = 10 * 1024 * 1024
    const val MAX_FILE_BYTES = 20 * 1024 * 1024
    private const val MAX_ENTRIES = 32
    private const val MAX_GENERATION_BYTES = 64 * 1024
    private const val MAX_EXTRACTED_BYTES = 20 * 1024 * 1024
    private val sqliteHeader = "SQLite format 3\u0000".encodeToByteArray()

    fun read(input: InputStream): MyCalendarContainer = try {
        ObjectInputStream(LimitedInputStream(input, MAX_FILE_BYTES)).use { objectInput ->
            if (objectInput.readInt() != -1 || objectInput.readInt() != 1 || objectInput.readInt() != 0) {
                throw MyCalendarFormatException("Unsupported My Calendar metadata")
            }
            readZip(objectInput)
        }
    } catch (error: MyCalendarFormatException) {
        throw error
    } catch (error: IOException) {
        throw MyCalendarFormatException("Damaged My Calendar backup", error)
    }

    private fun readZip(input: InputStream): MyCalendarContainer {
        var database: ByteArray? = null
        var generation: String? = null
        var extractedBytes = 0
        val names = mutableSetOf<String>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (names.size == MAX_ENTRIES) throw MyCalendarFormatException("Too many backup entries")
                val name = entry.name.replace('\\', '/').substringAfterLast('/')
                if (name.isEmpty() || !names.add(name)) throw MyCalendarFormatException("Duplicate backup entry")
                val limit = when (name) {
                    "cloud.db" -> MAX_DATABASE_BYTES
                    "1.generation" -> MAX_GENERATION_BYTES
                    else -> MAX_DATABASE_BYTES
                }
                val bytes = zip.readBounded(limit)
                extractedBytes += bytes.size
                if (extractedBytes > MAX_EXTRACTED_BYTES) throw MyCalendarFormatException("Backup expands too large")
                when (name) {
                    "cloud.db" -> database = bytes
                    "1.generation" -> generation = bytes.toString(StandardCharsets.UTF_8).trim()
                }
                zip.closeEntry()
            }
        }
        val verifiedDatabase = database ?: throw MyCalendarFormatException("My Calendar database is missing")
        if (verifiedDatabase.size < sqliteHeader.size ||
            !verifiedDatabase.copyOfRange(0, sqliteHeader.size).contentEquals(sqliteHeader)
        ) {
            throw MyCalendarFormatException("Invalid My Calendar database")
        }
        return MyCalendarContainer(verifiedDatabase, generation)
    }

    private fun InputStream.readBounded(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val count = read(buffer)
            if (count == -1) break
            if (output.size() + count > limit) throw MyCalendarFormatException("Backup entry is too large")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private class LimitedInputStream(input: InputStream, private val limit: Int) : FilterInputStream(input) {
        private var count = 0

        override fun read(): Int = super.read().also { if (it != -1) add(1) }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            super.read(buffer, offset, length).also { if (it > 0) add(it) }

        private fun add(value: Int) {
            count += value
            if (count > limit) throw IOException("Backup file is too large")
        }
    }
}
