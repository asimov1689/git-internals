package gitinternals

import java.io.File
import java.util.zip.InflaterInputStream

class GitObjectReader(
    private val commitParser: CommitParser = CommitParser(),
    private val treeParser: TreeParser = TreeParser(),
) {
    fun read(path: String): GitObject {
        val raw = decompress(path)
        return parse(raw)
    }

    private fun decompress(path: String): ByteArray = InflaterInputStream(File(path).inputStream()).use { it.readBytes() }

    private fun parse(raw: ByteArray): GitObject {
        val nullIndex = raw.indexOfNullByte()
        require(nullIndex != -1) { "Invalid Git object: missing null byte separator" }

        val header = String(raw, 0, nullIndex, Charsets.UTF_8)
        val bodyBytes = raw.copyOfRange(nullIndex + 1, raw.size)

        val parts = header.split(" ")
        require(parts.size == 2) { "Invalid Git object header: '$header'" }

        val type = parts[0]
        val size =
            parts[1].toIntOrNull()
                ?: throw IllegalArgumentException("Non-numeric size in header: '${parts[1]}'")

        return when (type) {
            "blob" -> GitObject.Blob(size = size, content = String(bodyBytes, Charsets.UTF_8).trimEnd('\n'))

            "commit" -> commitParser.parse(String(bodyBytes, Charsets.UTF_8))

            "tree" -> treeParser.parse(bodyBytes)

            else -> throw UnsupportedOperationException(
                "Unsupported Git object type: '$type'. Stage-004 handles blob, commit, and tree.",
            )
        }
    }

    private fun ByteArray.indexOfNullByte(): Int {
        forEachIndexed { i, b -> if (b == 0.toByte()) return i }
        return -1
    }
}
