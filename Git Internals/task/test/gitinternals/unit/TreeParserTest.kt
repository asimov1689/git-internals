package gitinternals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TreeParserTest {
    private val parser = TreeParser()

    private fun buildTreeBody(vararg entries: Triple<String, String, String>): ByteArray {
        val out = mutableListOf<Byte>()
        for ((permissions, sha1Hex, name) in entries) {
            permissions.toByteArray(Charsets.UTF_8).forEach { out += it }
            out += 0x20.toByte()
            name.toByteArray(Charsets.UTF_8).forEach { out += it }
            out += 0x00.toByte()
            require(sha1Hex.length == 40) { "SHA-1 hex must be 40 chars" }
            for (i in 0 until 40 step 2) {
                out += sha1Hex.substring(i, i + 2).toInt(16).toByte()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun `should parse single file entry`() {
        val sha = "2b26c15c04375d90203783fb4c2a45ff04b571a6"
        val body = buildTreeBody(Triple("100644", sha, "main.kt"))
        val result = parser.parse(body)

        assertEquals(1, result.entries.size)
        assertEquals("100644", result.entries[0].permissions)
        assertEquals(sha, result.entries[0].sha1)
        assertEquals("main.kt", result.entries[0].name)
    }

    @Test
    fun `should parse multiple entries preserving order`() {
        val sha1 = "2b26c15c04375d90203783fb4c2a45ff04b571a6"
        val sha2 = "f674b5d3a4c6cef5815b4e72ef2ea1bbe46b786b"
        val sha3 = "74198c849dbbcd51d060c59253a4757eedb9bd12"
        val body =
            buildTreeBody(
                Triple("100644", sha1, "main.kt"),
                Triple("100644", sha2, "readme.txt"),
                Triple("40000", sha3, "some-folder"),
            )
        val result = parser.parse(body)

        assertEquals(3, result.entries.size)
        assertEquals("main.kt", result.entries[0].name)
        assertEquals("readme.txt", result.entries[1].name)
        assertEquals("some-folder", result.entries[2].name)
    }

    @Test
    fun `should zero-pad SHA bytes with value less than 16`() {
        val sha = "0a0b0c0d0e0f101112131415161718191a1b1c1d"
        val body = buildTreeBody(Triple("100644", sha, "file.txt"))
        assertEquals(sha, parser.parse(body).entries[0].sha1)
    }

    @Test
    fun `should represent zero byte as 00 in SHA hex`() {
        val sha = "00" + "0".repeat(38)
        val body = buildTreeBody(Triple("100644", sha, "file.txt"))
        assertEquals(sha, parser.parse(body).entries[0].sha1)
    }

    @Test
    fun `should represent 200 decimal byte as c8 in SHA hex`() {
        val sha = "c8" + "0".repeat(38)
        val body = buildTreeBody(Triple("100644", sha, "file.txt"))
        assertEquals(sha, parser.parse(body).entries[0].sha1)
    }

    @Test
    fun `should represent 10 decimal byte as 0a in SHA hex`() {
        val sha = "0a" + "0".repeat(38)
        val body = buildTreeBody(Triple("100644", sha, "file.txt"))
        assertEquals(sha, parser.parse(body).entries[0].sha1)
    }

    @Test
    fun `should parse directory entry with 40000 permissions`() {
        val sha = "74198c849dbbcd51d060c59253a4757eedb9bd12"
        val body = buildTreeBody(Triple("40000", sha, "src"))
        val result = parser.parse(body)

        assertEquals("40000", result.entries[0].permissions)
        assertEquals("src", result.entries[0].name)
    }

    @Test
    fun `should return empty list for empty body`() {
        assertEquals(0, parser.parse(ByteArray(0)).entries.size)
    }

    @Test
    fun `should throw on body missing null terminator after filename`() {
        val partial = "100644 badname".toByteArray(Charsets.UTF_8)
        assertThrows(IllegalArgumentException::class.java) { parser.parse(partial) }
    }

    @Test
    fun `should throw on body with truncated SHA-1 binary`() {
        val out = mutableListOf<Byte>()
        "100644".toByteArray().forEach { out += it }
        out += 0x20.toByte()
        "file.txt".toByteArray().forEach { out += it }
        out += 0x00.toByte()
        repeat(10) { out += 0x00.toByte() }
        assertThrows(IllegalArgumentException::class.java) { parser.parse(out.toByteArray()) }
    }

    @Test
    fun `should parse entry name with dots and hyphens`() {
        val sha = "abcdef1234567890abcdef1234567890abcdef12"
        val body = buildTreeBody(Triple("100644", sha, "my-file.config.json"))
        assertEquals("my-file.config.json", parser.parse(body).entries[0].name)
    }

    @Test
    fun `should parse the exact spec example from stage-004 requirements`() {
        val sha1 = "2b26c15c04375d90203783fb4c2a45ff04b571a6"
        val sha2 = "f674b5d3a4c6cef5815b4e72ef2ea1bbe46b786b"
        val sha3 = "74198c849dbbcd51d060c59253a4757eedb9bd12"
        val body =
            buildTreeBody(
                Triple("100644", sha1, "main.kt"),
                Triple("100644", sha2, "readme.txt"),
                Triple("40000", sha3, "some-folder"),
            )
        val result = parser.parse(body)

        assertEquals(TreeEntry("100644", sha1, "main.kt"), result.entries[0])
        assertEquals(TreeEntry("100644", sha2, "readme.txt"), result.entries[1])
        assertEquals(TreeEntry("40000", sha3, "some-folder"), result.entries[2])
    }
}
