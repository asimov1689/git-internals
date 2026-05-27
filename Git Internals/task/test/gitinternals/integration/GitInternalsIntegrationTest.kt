package gitinternals.integration

import gitinternals.GitObject
import gitinternals.GitObjectFormatter
import gitinternals.GitObjectReader
import gitinternals.GitPathResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.DeflaterOutputStream
import kotlin.io.path.createTempDirectory

class GitInternalsIntegrationTest {
    // ── blob pipeline ─────────────────────────────────────────────────────────

    @Test
    fun `full pipeline reads and formats a blob`() {
        val content = "Hello world!"
        val (gitDir, hash) = fakeRepo("blob", content)
        val result = GitObjectReader().read(GitPathResolver().resolve(gitDir, hash))
        assertTrue(result is GitObject.Blob)
        assertEquals(content, (result as GitObject.Blob).content)
    }

    @Test
    fun `full pipeline formats blob output`() {
        val content = "fun main() {\nwhile(true) {\nprintln(\"Hello Hyperskill student!\")\n}\n}"
        val (gitDir, hash) = fakeRepo("blob", content)
        val output = GitObjectFormatter.format(GitObjectReader().read(GitPathResolver().resolve(gitDir, hash)))
        assertTrue(output.startsWith("*BLOB*\n"))
        assertTrue(output.contains("fun main()"))
    }

    // ── commit pipeline: spec example 2 ──────────────────────────────────────

    @Test
    fun `full pipeline formats commit — spec example 2`() {
        val (gitDir, hash) = fakeRepo("commit", specEx2Body())
        val output = GitObjectFormatter.format(GitObjectReader().read(GitPathResolver().resolve(gitDir, hash)))
        val expected =
            "*COMMIT*\n" +
                "tree: 79401ddb0e2c0fe0472c813754dd4a8873b66a84\n" +
                "parents: 12a4717e84b5e414f93cc91ca50a6d5a6c3563a0\n" +
                "author: Smith mr.smith@matrix original timestamp: 2020-03-29 17:18:20 +03:00\n" +
                "committer: Cypher cypher@matrix commit timestamp: 2020-03-29 17:25:52 +03:00\n" +
                "commit message:\n" +
                "get docs from feature1"
        assertEquals(expected, output)
    }

    // ── commit pipeline: spec example 3 — no parents ─────────────────────────

    @Test
    fun `full pipeline formats commit — spec example 3 no parents`() {
        val (gitDir, hash) = fakeRepo("commit", specEx3Body())
        val output = GitObjectFormatter.format(GitObjectReader().read(GitPathResolver().resolve(gitDir, hash)))
        val expected =
            "*COMMIT*\n" +
                "tree: aaa96ced2d9a1c8e72c56b253a0e2fe78393feb7\n" +
                "author: Kalinka Kali.k4@email.com original timestamp: 2021-12-11 22:31:36 -03:00\n" +
                "committer: Kalinka Kali.k4@email.com commit timestamp: 2021-12-11 22:31:36 -03:00\n" +
                "commit message:\n" +
                "simple hello"
        assertEquals(expected, output)
    }

    // ── commit pipeline: spec example 4 — merge commit ───────────────────────

    @Test
    fun `full pipeline formats commit — spec example 4 merge`() {
        val (gitDir, hash) = fakeRepo("commit", specEx4Body())
        val output = GitObjectFormatter.format(GitObjectReader().read(GitPathResolver().resolve(gitDir, hash)))
        val expected =
            "*COMMIT*\n" +
                "tree: d128f76a96c56ac4373717d3fbba4fa5875ca68f\n" +
                "parents: 5ad3239e54ba7c533d9f215a13ac82d14197cd8f | d2c5bedbb2c46945fd84f2ad209a7d4ee047f7f9\n" +
                "author: Kalinka Kali.k4@email.com original timestamp: 2021-12-11 22:49:02 -03:00\n" +
                "committer: Kalinka Kali.k4@email.com commit timestamp: 2021-12-11 22:49:02 -03:00\n" +
                "commit message:\n" +
                "awsome hello"
        assertEquals(expected, output)
    }

    // ── error propagation ─────────────────────────────────────────────────────

    @Test
    fun `should throw when object file does not exist`() {
        val gitDir = createTempDirectory().toFile().absolutePath
        val path = GitPathResolver().resolve(gitDir, "490f96725348e92770d3c6bab9ec532564b7ebe0")
        assertThrows(Exception::class.java) { GitObjectReader().read(path) }
    }

    // ── fake repo builder ─────────────────────────────────────────────────────

    private fun fakeRepo(
        type: String,
        content: String,
    ): Pair<String, String> {
        val hash = "490f96725348e92770d3c6bab9ec532564b7ebe0"
        val gitDir = createTempDirectory().toFile()
        val objectDir = File("${gitDir.absolutePath}/objects/${hash.substring(0, 2)}")
        objectDir.mkdirs()
        val contentBytes = content.toByteArray(Charsets.UTF_8)
        val raw = "$type ${contentBytes.size}".toByteArray() + byteArrayOf(0) + contentBytes
        DeflaterOutputStream(File(objectDir, hash.substring(2)).outputStream()).use { it.write(raw) }
        return Pair(gitDir.absolutePath, hash)
    }

    // ── spec body strings ─────────────────────────────────────────────────────

    private fun specEx2Body() =
        buildString {
            appendLine("tree 79401ddb0e2c0fe0472c813754dd4a8873b66a84")
            appendLine("parent 12a4717e84b5e414f93cc91ca50a6d5a6c3563a0")
            appendLine("author Smith mr.smith@matrix 1585491500 +0300")
            appendLine("committer Cypher cypher@matrix 1585491952 +0300")
            appendLine()
            append("get docs from feature1")
        }

    private fun specEx3Body() =
        buildString {
            appendLine("tree aaa96ced2d9a1c8e72c56b253a0e2fe78393feb7")
            appendLine("author Kalinka Kali.k4@email.com 1639272696 -0300")
            appendLine("committer Kalinka Kali.k4@email.com 1639272696 -0300")
            appendLine()
            append("simple hello")
        }

    private fun specEx4Body() =
        buildString {
            appendLine("tree d128f76a96c56ac4373717d3fbba4fa5875ca68f")
            appendLine("parent 5ad3239e54ba7c533d9f215a13ac82d14197cd8f")
            appendLine("parent d2c5bedbb2c46945fd84f2ad209a7d4ee047f7f9")
            appendLine("author Kalinka Kali.k4@email.com 1639273742 -0300")
            appendLine("committer Kalinka Kali.k4@email.com 1639273742 -0300")
            appendLine()
            append("awsome hello")
        }
}
