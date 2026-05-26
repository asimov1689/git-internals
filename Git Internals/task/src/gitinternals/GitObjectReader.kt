package gitinternals

import java.io.File
import java.util.zip.InflaterInputStream

class GitObjectReader(
    private val commitParser: CommitParser = CommitParser(),
) {
    fun read(path: String): GitObject {
        val raw = InflaterInputStream(File(path).inputStream()).use { it.readBytes() }
        return parse(raw)
    }

    private fun parse(raw: ByteArray): GitObject {
        val nullIndex = raw.indexOfNullByte()
        require(nullIndex != -1) { "Invalid Git object: missing null byte separator" }

        val header = String(raw, 0, nullIndex, Charsets.UTF_8)
        val body = String(raw, nullIndex + 1, raw.size - nullIndex - 1, Charsets.UTF_8)
        val parts = header.split(" ")

        require(parts.size == 2) { "Invalid Git object header: '$header'" }

        val size =
            parts[1].toIntOrNull()
                ?: throw IllegalArgumentException("Non-numeric size in header: '${parts[1]}'")

        return when (parts[0]) {
            "blob" -> GitObject.Blob(size = size, content = body.trimEnd('\n'))
            "commit" -> commitParser.parse(body)
            else -> throw UnsupportedOperationException("Unsupported type: '${parts[0]}'")
        }
    }

    private fun ByteArray.indexOfNullByte(): Int {
        forEachIndexed { i, b -> if (b == 0.toByte()) return i }
        return -1
    }
}
