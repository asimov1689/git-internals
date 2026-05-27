package gitinternals

class TreeParser {
    fun parse(body: ByteArray): GitObject.Tree {
        val entries = mutableListOf<TreeEntry>()
        var cursor = 0

        while (cursor < body.size) {
            val spaceIndex = body.indexOf(0x20.toByte(), cursor)
            require(spaceIndex != -1) { "Malformed tree entry: missing space at cursor $cursor" }

            val permissions = String(body, cursor, spaceIndex - cursor, Charsets.UTF_8)
            cursor = spaceIndex + 1

            val nullIndex = body.indexOf(0x00.toByte(), cursor)
            require(nullIndex != -1) { "Malformed tree entry: missing null terminator after name" }

            val name = String(body, cursor, nullIndex - cursor, Charsets.UTF_8)
            cursor = nullIndex + 1

            require(cursor + SHA1_BINARY_LENGTH <= body.size) {
                "Malformed tree entry: not enough bytes for SHA-1 at cursor $cursor"
            }

            val sha1 = buildSha1Hex(body, cursor)
            cursor += SHA1_BINARY_LENGTH

            entries += TreeEntry(permissions = permissions, sha1 = sha1, name = name)
        }

        return GitObject.Tree(entries = entries)
    }

    private fun buildSha1Hex(
        body: ByteArray,
        offset: Int,
    ): String {
        val sb = StringBuilder(40)
        repeat(SHA1_BINARY_LENGTH) { i ->
            val byte = body[offset + i].toInt() and 0xFF
            if (byte < 0x10) sb.append('0')
            sb.append(byte.toString(16))
        }
        return sb.toString()
    }

    private fun ByteArray.indexOf(
        target: Byte,
        from: Int,
    ): Int {
        for (i in from until size) if (this[i] == target) return i
        return -1
    }

    companion object {
        private const val SHA1_BINARY_LENGTH = 20
    }
}
