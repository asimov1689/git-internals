package gitinternals.unit

import gitinternals.GitObject
import gitinternals.GitObjectReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.DeflaterOutputStream

class GitObjectReaderTest {
    private val reader = GitObjectReader()

    @Test
    fun `should return Blob for blob type`() {
        // Arrange
        val path = objectFile("blob", "Hello world!")

        // Act
        val result = reader.read(path)

        // Assert
        assertTrue(result is GitObject.Blob)
    }

    @Test
    fun `should return blob with correct content`() {
        // Arrange
        val content = "fun main() {\n    println(\"hello\")\n}"
        val path = objectFile("blob", content)

        // Act
        val result = reader.read(path) as GitObject.Blob

        // Assert
        assertEquals(content, result.content)
    }

    @Test
    fun `should return blob with size from header`() {
        // Arrange
        val path = objectFile("blob", "Hello world!", sizeOverride = 12)

        // Act
        val result = reader.read(path) as GitObject.Blob

        // Assert
        assertEquals(12, result.size)
    }

    @Test
    fun `should trim trailing newline from blob content`() {
        // Arrange
        val path = objectFile("blob", "line one\nline two\n")

        // Act
        val result = reader.read(path) as GitObject.Blob

        // Assert
        assertEquals("line one\nline two", result.content)
    }

    @Test
    fun `should return Commit for commit type`() {
        // Arrange
        val path = objectFile("commit", validCommitBody())

        // Act
        val result = reader.read(path)

        // Assert
        assertTrue(result is GitObject.Commit)
    }

    @Test
    fun `should return commit with correct tree`() {
        // Arrange
        val path = objectFile("commit", validCommitBody())

        // Act
        val result = reader.read(path) as GitObject.Commit

        // Assert
        assertEquals("79401ddb0e2c0fe0472c813754dd4a8873b66a84", result.tree)
    }

    @Test
    fun `should throw UnsupportedOperationException for tree type`() {
        // Arrange
        val path = objectFile("tree", "binary-placeholder")

        // Act & Assert
        assertThrows(UnsupportedOperationException::class.java) { reader.read(path) }
    }

    @Test
    fun `should throw when file does not exist`() {
        // Arrange
        val path = "/nonexistent/abc123"

        // Act & Assert
        assertThrows(Exception::class.java) { reader.read(path) }
    }

    @Test
    fun `should throw when null byte separator is missing`() {
        // Arrange
        val file = File.createTempFile("bad", null).also { it.deleteOnExit() }
        DeflaterOutputStream(file.outputStream()).use {
            it.write("blob 5 hello".toByteArray())
        }

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) { reader.read(file.absolutePath) }
    }

    @Test
    fun `should throw when header is malformed`() {
        // Arrange
        val file = File.createTempFile("bad", null).also { it.deleteOnExit() }
        val raw = byteArrayOf(*"blobonly".toByteArray(), 0, *"content".toByteArray())
        DeflaterOutputStream(file.outputStream()).use { it.write(raw) }

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) { reader.read(file.absolutePath) }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun objectFile(
        type: String,
        content: String,
        sizeOverride: Int? = null,
    ): String {
        val contentBytes = content.toByteArray(Charsets.UTF_8)
        val raw = "$type ${sizeOverride ?: contentBytes.size}".toByteArray() + byteArrayOf(0) + contentBytes
        val file = File.createTempFile("git-obj-$type", null).also { it.deleteOnExit() }
        DeflaterOutputStream(file.outputStream()).use { it.write(raw) }
        return file.absolutePath
    }

    private fun validCommitBody(): String =
        buildString {
            appendLine("tree 79401ddb0e2c0fe0472c813754dd4a8873b66a84")
            appendLine("parent 12a4717e84b5e414f93cc91ca50a6d5a6c3563a0")
            appendLine("author Smith mr.smith@matrix 1585491500 +0300")
            appendLine("committer Smith mr.smith@matrix 1585491500 +0300")
            appendLine()
            append("initial commit")
        }
}
