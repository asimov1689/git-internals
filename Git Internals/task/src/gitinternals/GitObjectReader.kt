package gitinternals

import java.io.File
import java.util.zip.InflaterInputStream

class GitObjectReader {

    fun read(path: String): GitObject {
        val raw = decompress(path)
        return parse(raw)
    }

    private fun decompress(path: String): ByteArray =
        InflaterInputStream(File(path).inputStream()).use { it.readBytes() }

    private fun parse(raw: ByteArray): GitObject {
        val nullIndex = raw.indexOf(0)
        require(nullIndex != -1) { "Invalid Git object: missing null byte separator" }

        val header = String(raw, 0, nullIndex)
        val content = String(raw, nullIndex + 1, raw.size - nullIndex - 1)

        val parts = header.split(" ")
        require(parts.size == 2) { "Invalid Git object header: $header" }

        return GitObject(
            type = parts[0],
            size = parts[1].toInt(),
            content = content.trimEnd('\n')
        )
    }

    private fun ByteArray.indexOf(byte: Int): Int {
        forEachIndexed { i, b -> if (b == byte.toByte()) return i }
        return -1
    }
}