package gitinternals.unit

import gitinternals.GitLogReader
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.util.zip.DeflaterOutputStream
import kotlin.io.path.createTempDirectory

class GitLogReaderTest {
    @Test
    fun `should print branch log from newest commit to initial commit`() {
        val gitDir = createTempDirectory().toFile()
        val initial = "0000000000000000000000000000000000000001"
        val second = "0000000000000000000000000000000000000002"

        writeBranch(gitDir, "main", second)
        writeCommit(gitDir, initial, parents = emptyList(), committerEpoch = "1639272696", message = "initial commit")
        writeCommit(gitDir, second, parents = listOf(initial), committerEpoch = "1639273588", message = "maybe hello")

        assertEquals(
            "Commit: $second\n" +
                "Kalinka Kali.k4@email.com commit timestamp: 2021-12-11 22:46:28 -03:00\n" +
                "maybe hello\n" +
                "\n" +
                "Commit: $initial\n" +
                "Kalinka Kali.k4@email.com commit timestamp: 2021-12-11 22:31:36 -03:00\n" +
                "initial commit",
            GitLogReader().read(gitDir.absolutePath, "main"),
        )
    }

    @Test
    fun `should print second merge parent first and mark it as merged`() {
        val gitDir = createTempDirectory().toFile()
        val base = "0000000000000000000000000000000000000001"
        val mainParent = "0000000000000000000000000000000000000002"
        val mergedParent = "0000000000000000000000000000000000000003"
        val merge = "0000000000000000000000000000000000000004"

        writeBranch(gitDir, "main", merge)
        writeCommit(gitDir, base, parents = emptyList(), committerEpoch = "1639272696", message = "simple hello")
        writeCommit(gitDir, mainParent, parents = listOf(base), committerEpoch = "1639273588", message = "maybe hello")
        writeCommit(gitDir, mergedParent, parents = listOf(base), committerEpoch = "1639273434", message = "hello of the champions")
        writeCommit(gitDir, merge, parents = listOf(mainParent, mergedParent), committerEpoch = "1639273742", message = "awsome hello")

        assertEquals(
            "Commit: $merge\n" +
                "Kalinka Kali.k4@email.com commit timestamp: 2021-12-11 22:49:02 -03:00\n" +
                "awsome hello\n" +
                "\n" +
                "Commit: $mergedParent (merged)\n" +
                "Kalinka Kali.k4@email.com commit timestamp: 2021-12-11 22:43:54 -03:00\n" +
                "hello of the champions\n" +
                "\n" +
                "Commit: $mainParent\n" +
                "Kalinka Kali.k4@email.com commit timestamp: 2021-12-11 22:46:28 -03:00\n" +
                "maybe hello\n" +
                "\n" +
                "Commit: $base\n" +
                "Kalinka Kali.k4@email.com commit timestamp: 2021-12-11 22:31:36 -03:00\n" +
                "simple hello",
            GitLogReader().read(gitDir.absolutePath, "main"),
        )
    }

    private fun writeBranch(
        gitDir: File,
        branch: String,
        hash: String,
    ) {
        File(gitDir, "refs/heads/$branch").apply {
            parentFile.mkdirs()
            writeText(hash)
        }
    }

    private fun writeCommit(
        gitDir: File,
        hash: String,
        parents: List<String>,
        committerEpoch: String,
        message: String,
    ) {
        val body =
            buildString {
                appendLine("tree 1111111111111111111111111111111111111111")
                parents.forEach { appendLine("parent $it") }
                appendLine("author Kalinka Kali.k4@email.com $committerEpoch -0300")
                appendLine("committer Kalinka Kali.k4@email.com $committerEpoch -0300")
                appendLine()
                append(message)
            }
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val raw = "commit ${bodyBytes.size}".toByteArray() + byteArrayOf(0) + bodyBytes
        File(gitDir, "objects/${hash.substring(0, 2)}/${hash.substring(2)}").apply {
            parentFile.mkdirs()
            DeflaterOutputStream(outputStream()).use { it.write(raw) }
        }
    }
}
