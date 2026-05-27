package gitinternals.unit

import gitinternals.CommitParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitParserTest {
    private val parser = CommitParser()

    // ── tree ──────────────────────────────────────────────────────────────────

    @Test
    fun `should parse tree hash`() {
        val body = body(tree = "79401ddb0e2c0fe0472c813754dd4a8873b66a84")
        val result = parser.parse(body)
        assertEquals("79401ddb0e2c0fe0472c813754dd4a8873b66a84", result.tree)
    }

    // ── parents ───────────────────────────────────────────────────────────────

    @Test
    fun `should produce empty parents list for initial commit`() {
        val body = body(parents = emptyList())
        val result = parser.parse(body)
        assertTrue(result.parents.isEmpty())
    }

    @Test
    fun `should parse single parent`() {
        val sha = "12a4717e84b5e414f93cc91ca50a6d5a6c3563a0"
        val body = body(parents = listOf(sha))
        val result = parser.parse(body)
        assertEquals(listOf(sha), result.parents)
    }

    @Test
    fun `should parse two parents and preserve order`() {
        val first = "5ad3239e54ba7c533d9f215a13ac82d14197cd8f"
        val second = "d2c5bedbb2c46945fd84f2ad209a7d4ee047f7f9"
        val body = body(parents = listOf(first, second))
        val result = parser.parse(body)
        assertEquals(listOf(first, second), result.parents)
    }

    // ── author ────────────────────────────────────────────────────────────────

    @Test
    fun `should parse author name`() {
        val body = body(authorName = "Smith")
        val result = parser.parse(body)
        assertEquals("Smith", result.author.name)
    }

    @Test
    fun `should parse author email`() {
        val body = body(authorEmail = "mr.smith@matrix")
        val result = parser.parse(body)
        assertEquals("mr.smith@matrix", result.author.email)
    }

    @Test
    fun `should strip angle brackets from email — real git object format`() {
        val body = body(authorEmail = "<mr.smith@matrix>")
        val result = parser.parse(body)
        assertEquals("mr.smith@matrix", result.author.email)
    }

    @Test
    fun `should parse author epoch`() {
        val body = body(authorEpoch = "1585491500")
        val result = parser.parse(body)
        assertEquals(1585491500L, result.author.epochSeconds)
    }

    @Test
    fun `should parse positive timezone offset`() {
        val body = body(authorZone = "+0300")
        val result = parser.parse(body)
        assertEquals("+03:00", result.author.zoneOffset.id)
    }

    @Test
    fun `should parse negative timezone offset`() {
        val body = body(authorZone = "-0300")
        val result = parser.parse(body)
        assertEquals("-03:00", result.author.zoneOffset.id)
    }

    // ── committer ─────────────────────────────────────────────────────────────

    @Test
    fun `should parse different committer from author`() {
        val body = body(committerName = "Cypher", committerEmail = "cypher@matrix")
        val result = parser.parse(body)
        assertEquals("Cypher", result.committer.name)
        assertEquals("cypher@matrix", result.committer.email)
    }

    @Test
    fun `should parse different epoch for committer than author`() {
        val body = body(authorEpoch = "1585491500", committerEpoch = "1585491952")
        val result = parser.parse(body)
        assertEquals(1585491500L, result.author.epochSeconds)
        assertEquals(1585491952L, result.committer.epochSeconds)
    }

    // ── timestamp formatting ──────────────────────────────────────────────────

    @Test
    fun `should format author timestamp — spec example 2`() {
        val body = body(authorEpoch = "1585491500", authorZone = "+0300")
        val result = parser.parse(body)
        assertEquals("2020-03-29 17:18:20 +03:00", result.author.formattedTimestamp())
    }

    @Test
    fun `should format committer timestamp — spec example 2`() {
        val body = body(committerEpoch = "1585491952", committerZone = "+0300")
        val result = parser.parse(body)
        assertEquals("2020-03-29 17:25:52 +03:00", result.committer.formattedTimestamp())
    }

    @Test
    fun `should format timestamp with negative offset — spec example 3`() {
        val body = body(authorEpoch = "1639272696", authorZone = "-0300")
        val result = parser.parse(body)
        assertEquals("2021-12-11 22:31:36 -03:00", result.author.formattedTimestamp())
    }

    @Test
    fun `should format timestamp — spec example 4`() {
        val body = body(authorEpoch = "1639273742", authorZone = "-0300")
        val result = parser.parse(body)
        assertEquals("2021-12-11 22:49:02 -03:00", result.author.formattedTimestamp())
    }

    // ── message ───────────────────────────────────────────────────────────────

    @Test
    fun `should parse single-line message`() {
        val body = body(message = "get docs from feature1")
        val result = parser.parse(body)
        assertEquals("get docs from feature1", result.message)
    }

    @Test
    fun `should parse multi-line message preserving internal newlines`() {
        val msg = "line one\nline two\nline three"
        val body = body(message = msg)
        val result = parser.parse(body)
        assertEquals(msg, result.message)
    }

    @Test
    fun `should trim trailing newline from message`() {
        val body = body(message = "simple hello\n")
        val result = parser.parse(body)
        assertEquals("simple hello", result.message)
    }

    // ── error cases ───────────────────────────────────────────────────────────

    @Test
    fun `should throw when tree field is missing`() {
        val body =
            buildString {
                appendLine("author Smith mr.smith@matrix 1585491500 +0300")
                appendLine("committer Smith mr.smith@matrix 1585491500 +0300")
                appendLine()
                append("msg")
            }
        assertThrows(IllegalArgumentException::class.java) { parser.parse(body) }
    }

    @Test
    fun `should throw when author field is missing`() {
        val body =
            buildString {
                appendLine("tree 79401ddb0e2c0fe0472c813754dd4a8873b66a84")
                appendLine("committer Smith mr.smith@matrix 1585491500 +0300")
                appendLine()
                append("msg")
            }
        assertThrows(IllegalArgumentException::class.java) { parser.parse(body) }
    }

    @Test
    fun `should throw for malformed timezone offset`() {
        val body = body(authorZone = "UTC")
        assertThrows(IllegalArgumentException::class.java) { parser.parse(body) }
    }

    @Test
    fun `should throw for non-numeric epoch`() {
        val body = body(authorEpoch = "notanumber")
        assertThrows(IllegalArgumentException::class.java) { parser.parse(body) }
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private fun body(
        tree: String = "79401ddb0e2c0fe0472c813754dd4a8873b66a84",
        parents: List<String> = listOf("12a4717e84b5e414f93cc91ca50a6d5a6c3563a0"),
        authorName: String = "Smith",
        authorEmail: String = "mr.smith@matrix",
        authorEpoch: String = "1585491500",
        authorZone: String = "+0300",
        committerName: String = "Smith",
        committerEmail: String = "mr.smith@matrix",
        committerEpoch: String = "1585491500",
        committerZone: String = "+0300",
        message: String = "test commit",
    ): String =
        buildString {
            appendLine("tree $tree")
            parents.forEach { appendLine("parent $it") }
            appendLine("author $authorName $authorEmail $authorEpoch $authorZone")
            appendLine("committer $committerName $committerEmail $committerEpoch $committerZone")
            appendLine()
            append(message)
        }
}
