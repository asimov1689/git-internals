package gitinternals

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import java.io.File
import java.util.zip.DeflaterOutputStream

class GitObjectReaderTest {

    private val reader = GitObjectReader()

    private fun createGitObject(header: String, content: String): String {
        val raw = (header + "\u0000" + content).toByteArray()
        val file = File.createTempFile("git-object", null)
        file.deleteOnExit()
        DeflaterOutputStream(file.outputStream()).use { it.write(raw) }
        return file.absolutePath
    }

    @Test
    fun `should parse blob with single line content`() {
        // Arrange
        val path = createGitObject("blob 13", "Hello world!")

        // Act
        val result = reader.read(path)

        // Assert
        assertEquals("blob", result.type)
        assertEquals(13, result.size)
        assertEquals("Hello world!", result.content)
    }

    @Test
    fun `should parse blob with multiline content`() {
        // Arrange
        val path = createGitObject("blob 23", "first line\nsecond line")

        // Act
        val result = reader.read(path)

        // Assert
        assertEquals("blob", result.type)
        assertEquals(23, result.size)
        assertEquals("first line\nsecond line", result.content)
    }

    @Test
    fun `should throw when file does not exist`() {
        // Arrange
        val path = "/non/existent/path/object"

        // Act & Assert
        assertThrows(Exception::class.java) { reader.read(path) }
    }

    @Test
    fun `should throw when null byte separator is missing`() {
        // Arrange
        val file = File.createTempFile("bad-object", null)
        file.deleteOnExit()
        DeflaterOutputStream(file.outputStream()).use {
            it.write("blob 5 hello".toByteArray())
        }

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) { reader.read(file.absolutePath) }
    }

    @Test
    fun `should throw when header is malformed`() {
        // Arrange
        val path = createGitObject("blobonly", "some content")

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) { reader.read(path) }
    }
}