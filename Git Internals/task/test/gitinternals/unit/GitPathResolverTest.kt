package gitinternals.unit

import gitinternals.GitPathResolver
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows

class GitPathResolverTest {

    private val resolver = GitPathResolver()

    @Test
    fun `should split hash into two-char dir and remaining filename`() {
        // Arrange
        val gitDir = "/home/project/.git"
        val hash   = "490f96725348e92770d3c6bab9ec532564b7ebe0"

        // Act
        val path = resolver.resolve(gitDir, hash)

        // Assert
        assertEquals("/home/project/.git/objects/49/0f96725348e92770d3c6bab9ec532564b7ebe0", path)
    }

    @Test
    fun `should work with relative git directory`() {
        // Arrange
        val gitDir = "task/test/gitone"
        val hash   = "0eee6a98471a350b2c2316313114185ecaf82f0e"

        // Act
        val path = resolver.resolve(gitDir, hash)

        // Assert
        assertEquals("task/test/gitone/objects/0e/ee6a98471a350b2c2316313114185ecaf82f0e", path)
    }

    @Test
    fun `should handle git directory with trailing slash`() {
        // Arrange
        val gitDir = "task/test/gitone/"
        val hash   = "490f96725348e92770d3c6bab9ec532564b7ebe0"

        // Act
        val path = resolver.resolve(gitDir, hash)

        // Assert
        assert(path.contains("/objects/49/0f96725348e92770d3c6bab9ec532564b7ebe0"))
    }

    @Test
    fun `should throw when hash is shorter than 40 chars`() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            resolver.resolve(".git", "abc123")
        }
    }

    @Test
    fun `should throw when hash is longer than 40 chars`() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            resolver.resolve(".git", "490f96725348e92770d3c6bab9ec532564b7ebe0EXTRA")
        }
    }

    @Test
    fun `should throw when hash is empty`() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            resolver.resolve(".git", "")
        }
    }
}