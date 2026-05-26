package gitinternals.unit

import gitinternals.Contributor
import gitinternals.GitObject
import gitinternals.GitObjectFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class GitObjectFormatterTest {
    // ── blob ──────────────────────────────────────────────────────────────────

    @Test
    fun `should format blob`() {
        assertEquals("*BLOB*\nHello world!", GitObjectFormatter.format(GitObject.Blob(13, "Hello world!")))
    }

    @Test
    fun `should format blob with multiline content`() {
        val content = "fun main() {\n    println(\"hello\")\n}"
        assertEquals("*BLOB*\n$content", GitObjectFormatter.format(GitObject.Blob(content.length, content)))
    }

    // ── commit: spec example 2 ────────────────────────────────────────────────

    @Test
    fun `should produce exact output — spec example 2`() {
        val commit =
            GitObject.Commit(
                tree = "79401ddb0e2c0fe0472c813754dd4a8873b66a84",
                parents = listOf("12a4717e84b5e414f93cc91ca50a6d5a6c3563a0"),
                author = Contributor("Smith", "mr.smith@matrix", 1585491500L, ZoneOffset.of("+03:00")),
                committer = Contributor("Cypher", "cypher@matrix", 1585491952L, ZoneOffset.of("+03:00")),
                message = "get docs from feature1",
            )
        val expected =
            "*COMMIT*\n" +
                "tree: 79401ddb0e2c0fe0472c813754dd4a8873b66a84\n" +
                "parents: 12a4717e84b5e414f93cc91ca50a6d5a6c3563a0\n" +
                "author: Smith mr.smith@matrix original timestamp: 2020-03-29 17:18:20 +03:00\n" +
                "committer: Cypher cypher@matrix commit timestamp: 2020-03-29 17:25:52 +03:00\n" +
                "commit message:\n" +
                "get docs from feature1"
        assertEquals(expected, GitObjectFormatter.format(commit))
    }

    // ── commit: spec example 3 — no parents ──────────────────────────────────

    @Test
    fun `should produce exact output — spec example 3 no parents`() {
        val k = Contributor("Kalinka", "Kali.k4@email.com", 1639272696L, ZoneOffset.of("-03:00"))
        val commit =
            GitObject.Commit(
                tree = "aaa96ced2d9a1c8e72c56b253a0e2fe78393feb7",
                parents = emptyList(),
                author = k,
                committer = k,
                message = "simple hello",
            )
        val expected =
            "*COMMIT*\n" +
                "tree: aaa96ced2d9a1c8e72c56b253a0e2fe78393feb7\n" +
                "author: Kalinka Kali.k4@email.com original timestamp: 2021-12-11 22:31:36 -03:00\n" +
                "committer: Kalinka Kali.k4@email.com commit timestamp: 2021-12-11 22:31:36 -03:00\n" +
                "commit message:\n" +
                "simple hello"
        assertEquals(expected, GitObjectFormatter.format(commit))
    }

    @Test
    fun `should not include parents line when zero parents`() {
        val k = Contributor("A", "a@b.com", 0L, ZoneOffset.UTC)
        val output = GitObjectFormatter.format(GitObject.Commit("abc", emptyList(), k, k, "msg"))
        assertTrue(!output.contains("parents:"))
    }

    // ── commit: spec example 4 — merge commit ────────────────────────────────

    @Test
    fun `should produce exact output — spec example 4 merge commit`() {
        val k = Contributor("Kalinka", "Kali.k4@email.com", 1639273742L, ZoneOffset.of("-03:00"))
        val commit =
            GitObject.Commit(
                tree = "d128f76a96c56ac4373717d3fbba4fa5875ca68f",
                parents =
                    listOf(
                        "5ad3239e54ba7c533d9f215a13ac82d14197cd8f",
                        "d2c5bedbb2c46945fd84f2ad209a7d4ee047f7f9",
                    ),
                author = k,
                committer = k,
                message = "awsome hello",
            )
        val expected =
            "*COMMIT*\n" +
                "tree: d128f76a96c56ac4373717d3fbba4fa5875ca68f\n" +
                "parents: 5ad3239e54ba7c533d9f215a13ac82d14197cd8f | d2c5bedbb2c46945fd84f2ad209a7d4ee047f7f9\n" +
                "author: Kalinka Kali.k4@email.com original timestamp: 2021-12-11 22:49:02 -03:00\n" +
                "committer: Kalinka Kali.k4@email.com commit timestamp: 2021-12-11 22:49:02 -03:00\n" +
                "commit message:\n" +
                "awsome hello"
        assertEquals(expected, GitObjectFormatter.format(commit))
    }

    @Test
    fun `should join two parent hashes with pipe separator`() {
        val k = Contributor("A", "a@b.com", 0L, ZoneOffset.UTC)
        val output = GitObjectFormatter.format(GitObject.Commit("tree", listOf("aaa", "bbb"), k, k, "m"))
        assertTrue(output.contains("parents: aaa | bbb"))
    }
}
