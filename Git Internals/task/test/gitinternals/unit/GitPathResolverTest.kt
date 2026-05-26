package gitinternals.unit

import gitinternals.GitPathResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GitPathResolverTest {
    private val resolver = GitPathResolver()

    @Test
    fun `should split hash into two-char dir and remaining filename`() {
        // Arrange
        val gitDir = "/home/project/.git"
        val hash = "490f96725348e92770d3c6bab9ec532564b7ebe0"

        // Act
        val result = resolver.resolve(gitDir, hash)

        // Assert
        assertEquals("/home/project/.git/objects/49/0f96725348e92770d3c6bab9ec532564b7ebe0", result)
    }

    @Test
    fun `should work with relative git directory`() {
        // Arrange
        val gitDir = "task/test/gitone"
        val hash = "0eee6a98471a350b2c2316313114185ecaf82f0e"

        // Act
        val result = resolver.resolve(gitDir, hash)

        // Assert
        assertEquals("task/test/gitone/objects/0e/ee6a98471a350b2c2316313114185ecaf82f0e", result)
    }

    @Test
    fun `should throw when hash is shorter than 40 chars`() {
        // Arrange
        val hash = "abc123"

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            resolver.resolve(".git", hash)
        }
    }

    @Test
    fun `should throw when hash is longer than 40 chars`() {
        // Arrange
        val hash = "490f96725348e92770d3c6bab9ec532564b7ebe0EXTRA"

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            resolver.resolve(".git", hash)
        }
    }

    @Test
    fun `should throw when hash is empty`() {
        // Arrange
        val hash = ""

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            resolver.resolve(".git", hash)
        }
    }
}
