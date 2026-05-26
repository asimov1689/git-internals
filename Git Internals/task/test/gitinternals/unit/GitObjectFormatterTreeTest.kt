package gitinternals.unit

import gitinternals.GitObject
import gitinternals.GitObjectFormatter
import gitinternals.TreeEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitObjectFormatterTreeTest {
    @Test
    fun `should prefix output with TREE marker`() {
        val tree =
            GitObject.Tree(
                entries =
                    listOf(
                        TreeEntry("100644", "2b26c15c04375d90203783fb4c2a45ff04b571a6", "main.kt"),
                    ),
            )
        assertTrue(GitObjectFormatter.format(tree).startsWith("*TREE*"))
    }

    @Test
    fun `should format single entry as permissions space sha1 space name`() {
        val sha = "2b26c15c04375d90203783fb4c2a45ff04b571a6"
        val tree = GitObject.Tree(entries = listOf(TreeEntry("100644", sha, "main.kt")))
        val lines = GitObjectFormatter.format(tree).lines()

        assertEquals("100644 $sha main.kt", lines[1])
    }

    @Test
    fun `should match spec example output exactly`() {
        val sha1 = "2b26c15c04375d90203783fb4c2a45ff04b571a6"
        val sha2 = "f674b5d3a4c6cef5815b4e72ef2ea1bbe46b786b"
        val sha3 = "74198c849dbbcd51d060c59253a4757eedb9bd12"
        val tree =
            GitObject.Tree(
                entries =
                    listOf(
                        TreeEntry("100644", sha1, "main.kt"),
                        TreeEntry("100644", sha2, "readme.txt"),
                        TreeEntry("40000", sha3, "some-folder"),
                    ),
            )
        val expected = "*TREE*\n100644 $sha1 main.kt\n100644 $sha2 readme.txt\n40000 $sha3 some-folder"

        assertEquals(expected, GitObjectFormatter.format(tree))
    }

    @Test
    fun `should not end with trailing newline`() {
        val tree =
            GitObject.Tree(
                entries =
                    listOf(
                        TreeEntry("40000", "74198c849dbbcd51d060c59253a4757eedb9bd12", "src"),
                    ),
            )
        assertFalse(GitObjectFormatter.format(tree).endsWith("\n"))
    }

    @Test
    fun `should format empty tree with only marker line`() {
        assertEquals("*TREE*", GitObjectFormatter.format(GitObject.Tree(entries = emptyList())))
    }

    @Test
    fun `should preserve directory entry 40000 permissions`() {
        val sha = "74198c849dbbcd51d060c59253a4757eedb9bd12"
        val tree = GitObject.Tree(entries = listOf(TreeEntry("40000", sha, "some-folder")))
        assertTrue(GitObjectFormatter.format(tree).contains("40000 $sha some-folder"))
    }

    @Test
    fun `blob format should be unaffected by tree addition`() {
        val blob = GitObject.Blob(size = 13, content = "Hello world!")
        assertEquals("*BLOB*\nHello world!", GitObjectFormatter.format(blob))
    }
}
