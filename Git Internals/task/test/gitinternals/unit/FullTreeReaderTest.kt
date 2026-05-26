package gitinternals.unit

import gitinternals.FullTreeReader
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.util.zip.DeflaterOutputStream
import kotlin.io.path.createTempDirectory

class FullTreeReaderTest {
    @Test
    fun `should print all files from commit root tree`() {
        val gitDir = createTempDirectory().toFile()
        val commitHash = "0000000000000000000000000000000000000001"
        val rootTree = "0000000000000000000000000000000000000002"
        val mainFile = "0000000000000000000000000000000000000003"
        val readmeFile = "0000000000000000000000000000000000000004"

        writeCommit(gitDir, commitHash, rootTree)
        writeTree(
            gitDir,
            rootTree,
            Entry("100644", mainFile, "main.kt"),
            Entry("100644", readmeFile, "readme.txt"),
        )

        assertEquals(
            "main.kt\nreadme.txt",
            FullTreeReader().read(gitDir.absolutePath, commitHash),
        )
    }

    @Test
    fun `should recursively print nested tree files with directory prefixes`() {
        val gitDir = createTempDirectory().toFile()
        val commitHash = "0000000000000000000000000000000000000001"
        val rootTree = "0000000000000000000000000000000000000002"
        val mainFile = "0000000000000000000000000000000000000003"
        val readmeFile = "0000000000000000000000000000000000000004"
        val subTree = "0000000000000000000000000000000000000005"
        val nestedFile = "0000000000000000000000000000000000000006"

        writeCommit(gitDir, commitHash, rootTree)
        writeTree(
            gitDir,
            rootTree,
            Entry("100644", mainFile, "main.kt"),
            Entry("100644", readmeFile, "readme.txt"),
            Entry("40000", subTree, "some-folder"),
        )
        writeTree(gitDir, subTree, Entry("100644", nestedFile, "qq.txt"))

        assertEquals(
            "main.kt\nreadme.txt\nsome-folder/qq.txt",
            FullTreeReader().read(gitDir.absolutePath, commitHash),
        )
    }

    @Test
    fun `should support multiple nested directories`() {
        val gitDir = createTempDirectory().toFile()
        val commitHash = "0000000000000000000000000000000000000001"
        val rootTree = "0000000000000000000000000000000000000002"
        val srcTree = "0000000000000000000000000000000000000003"
        val testTree = "0000000000000000000000000000000000000004"
        val fileHash = "0000000000000000000000000000000000000005"

        writeCommit(gitDir, commitHash, rootTree)
        writeTree(gitDir, rootTree, Entry("40000", srcTree, "src"))
        writeTree(gitDir, srcTree, Entry("40000", testTree, "test"))
        writeTree(gitDir, testTree, Entry("100644", fileHash, "MainTest.kt"))

        assertEquals(
            "src/test/MainTest.kt",
            FullTreeReader().read(gitDir.absolutePath, commitHash),
        )
    }

    private fun writeCommit(
        gitDir: File,
        hash: String,
        treeHash: String,
    ) {
        val body =
            buildString {
                appendLine("tree $treeHash")
                appendLine("author Neo mr.anderson@matrix 1585491052 +0300")
                appendLine("committer Neo mr.anderson@matrix 1585491052 +0300")
                appendLine()
                append("initial commit")
            }
        writeObject(gitDir, hash, "commit", body.toByteArray(Charsets.UTF_8))
    }

    private fun writeTree(
        gitDir: File,
        hash: String,
        vararg entries: Entry,
    ) {
        val body = mutableListOf<Byte>()
        entries.forEach { entry ->
            entry.permissions.toByteArray(Charsets.UTF_8).forEach { body += it }
            body += 0x20.toByte()
            entry.name.toByteArray(Charsets.UTF_8).forEach { body += it }
            body += 0x00.toByte()
            for (i in 0 until 40 step 2) {
                body += entry.sha1.substring(i, i + 2).toInt(16).toByte()
            }
        }
        writeObject(gitDir, hash, "tree", body.toByteArray())
    }

    private fun writeObject(
        gitDir: File,
        hash: String,
        type: String,
        body: ByteArray,
    ) {
        val raw = "$type ${body.size}".toByteArray() + byteArrayOf(0) + body
        File(gitDir, "objects/${hash.substring(0, 2)}/${hash.substring(2)}").apply {
            parentFile.mkdirs()
            DeflaterOutputStream(outputStream()).use { it.write(raw) }
        }
    }

    private data class Entry(
        val permissions: String,
        val sha1: String,
        val name: String,
    )
}
