package gitinternals.integration

import gitinternals.GitObjectFormatter
import gitinternals.GitObjectReader
import gitinternals.GitPathResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.DeflaterOutputStream

class TreeIntegrationTest {
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

    private fun plantObject(
        hash: String,
        bodyBytes: ByteArray,
    ): String {
        val gitRoot = Files.createTempDirectory("git-tree-test")
        val folder = gitRoot.resolve("objects/${hash.substring(0, 2)}").toFile().also { it.mkdirs() }
        DeflaterOutputStream(File(folder, hash.substring(2)).outputStream()).use {
            it.write(("tree ${bodyBytes.size}\u0000").toByteArray(Charsets.UTF_8))
            it.write(bodyBytes)
        }
        return gitRoot.toString()
    }

    @Test
    fun `full pipeline from gitDir and hash to formatted tree output`() {
        val hash = "109e8050b41bd10b81be0a51a5e67327f5609551"
        val sha1 = "2b26c15c04375d90203783fb4c2a45ff04b571a6"
        val sha2 = "f674b5d3a4c6cef5815b4e72ef2ea1bbe46b786b"
        val sha3 = "74198c849dbbcd51d060c59253a4757eedb9bd12"
        val gitDir =
            plantObject(
                hash,
                buildTreeBody(
                    Triple("100644", sha1, "main.kt"),
                    Triple("100644", sha2, "readme.txt"),
                    Triple("40000", sha3, "some-folder"),
                ),
            )

        val output =
            GitObjectFormatter.format(
                GitObjectReader().read(GitPathResolver().resolve(gitDir, hash)),
            )

        val expected = "*TREE*\n100644 $sha1 main.kt\n100644 $sha2 readme.txt\n40000 $sha3 some-folder"
        assertEquals(expected, output)
    }

    @Test
    fun `output starts with TREE marker on first line`() {
        val hash = "abcdef1234567890abcdef1234567890abcdef12"
        val sha = "2b26c15c04375d90203783fb4c2a45ff04b571a6"
        val gitDir = plantObject(hash, buildTreeBody(Triple("100644", sha, "file.kt")))

        val output =
            GitObjectFormatter.format(
                GitObjectReader().read(GitPathResolver().resolve(gitDir, hash)),
            )

        assertEquals("*TREE*", output.lines()[0])
    }

    @Test
    fun `each entry line contains permissions sha1 and name in that order`() {
        val hash = "abcdef1234567890abcdef1234567890abcd1234"
        val sha = "2b26c15c04375d90203783fb4c2a45ff04b571a6"
        val gitDir = plantObject(hash, buildTreeBody(Triple("100644", sha, "Main.kt")))

        val parts =
            GitObjectFormatter
                .format(
                    GitObjectReader().read(GitPathResolver().resolve(gitDir, hash)),
                ).lines()[1]
                .split(" ")

        assertEquals("100644", parts[0])
        assertEquals(sha, parts[1])
        assertEquals("Main.kt", parts[2])
    }

    @Test
    fun `output does not end with trailing newline`() {
        val hash = "abcdef1234567890abcdef1234567890abcd5678"
        val sha = "2b26c15c04375d90203783fb4c2a45ff04b571a6"
        val gitDir = plantObject(hash, buildTreeBody(Triple("40000", sha, "src")))

        val output =
            GitObjectFormatter.format(
                GitObjectReader().read(GitPathResolver().resolve(gitDir, hash)),
            )

        assertTrue(!output.endsWith("\n"))
    }

    @Test
    fun `blob pipeline still works after tree support added`() {
        val content = "Hello world!\n"
        val hash = "abcdef1234567890abcdef1234567890abcd9999"
        val gitRoot = Files.createTempDirectory("git-tree-test")
        val folder = gitRoot.resolve("objects/${hash.substring(0, 2)}").toFile().also { it.mkdirs() }
        DeflaterOutputStream(File(folder, hash.substring(2)).outputStream()).use {
            it.write(("blob ${content.length}\u0000$content").toByteArray())
        }

        val output =
            GitObjectFormatter.format(
                GitObjectReader().read(GitPathResolver().resolve(gitRoot.toString(), hash)),
            )

        assertEquals("*BLOB*\nHello world!", output)
    }
}
