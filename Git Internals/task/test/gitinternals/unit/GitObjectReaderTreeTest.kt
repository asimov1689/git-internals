package gitinternals.unit

import gitinternals.GitObject
import gitinternals.GitObjectReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.DeflaterOutputStream

class GitObjectReaderTreeTest {
    private val reader = GitObjectReader()

    private fun buildTreeBody(vararg entries: Triple<String, String, String>): ByteArray {
        val out = mutableListOf<Byte>()
        for ((permissions, sha1Hex, name) in entries) {
            permissions.toByteArray(Charsets.UTF_8).forEach { out += it }
            out += 0x20.toByte()
            name.toByteArray(Charsets.UTF_8).forEach { out += it }
            out += 0x00.toByte()
            for (i in 0 until 40 step 2) {
                out += sha1Hex.substring(i, i + 2).toInt(16).toByte()
            }
        }
        return out.toByteArray()
    }

    private fun writeCompressedObject(
        header: String,
        bodyBytes: ByteArray,
    ): String {
        val file = File.createTempFile("tree-object", null).also { it.deleteOnExit() }
        DeflaterOutputStream(file.outputStream()).use {
            it.write((header + "\u0000").toByteArray(Charsets.UTF_8))
            it.write(bodyBytes)
        }
        return file.absolutePath
    }

    @Test
    fun `should return Tree instance for tree type header`() {
        val sha = "2b26c15c04375d90203783fb4c2a45ff04b571a6"
        val bodyBytes = buildTreeBody(Triple("100644", sha, "main.kt"))
        val path = writeCompressedObject("tree ${bodyBytes.size}", bodyBytes)

        assertTrue(reader.read(path) is GitObject.Tree)
    }

    @Test
    fun `should parse tree entries from compressed file`() {
        val sha1 = "2b26c15c04375d90203783fb4c2a45ff04b571a6"
        val sha2 = "f674b5d3a4c6cef5815b4e72ef2ea1bbe46b786b"
        val bodyBytes =
            buildTreeBody(
                Triple("100644", sha1, "main.kt"),
                Triple("100644", sha2, "readme.txt"),
            )
        val path = writeCompressedObject("tree ${bodyBytes.size}", bodyBytes)
        val result = reader.read(path) as GitObject.Tree

        assertEquals(2, result.entries.size)
        assertEquals("main.kt", result.entries[0].name)
        assertEquals("readme.txt", result.entries[1].name)
    }

    @Test
    fun `should still parse blob objects after tree support added`() {
        val content = "Hello world!\n"
        val raw = "blob ${content.length}\u0000$content"
        val file = File.createTempFile("blob-object", null).also { it.deleteOnExit() }
        DeflaterOutputStream(file.outputStream()).use { it.write(raw.toByteArray()) }

        val result = reader.read(file.absolutePath) as GitObject.Blob
        assertEquals("Hello world!", result.content)
    }

    @Test
    fun `should throw for unsupported type tag`() {
        val raw = "unknowntype 5\u0000hello"
        val file = File.createTempFile("bad-object", null).also { it.deleteOnExit() }
        DeflaterOutputStream(file.outputStream()).use { it.write(raw.toByteArray()) }

        assertThrows(UnsupportedOperationException::class.java) { reader.read(file.absolutePath) }
    }
}
