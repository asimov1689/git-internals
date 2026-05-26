package gitinternals.integration

import gitinternals.main
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.zip.DeflaterOutputStream
import kotlin.io.path.createTempDirectory

class CliIntegrationTest {
    @Test
    fun `list branches command prints sorted branches and marks current branch`() {
        val gitDir =
            createGitDir(
                head = "ref: refs/heads/master",
                branches = listOf("feature2", "master", "feature1"),
            )

        val output = runCli("${gitDir.absolutePath}\nlist-branches\n")

        assertEquals(
            "Enter .git directory location:\n" +
                "Enter command:\n" +
                "  feature1\n" +
                "  feature2\n" +
                "* master",
            output,
        )
    }

    @Test
    fun `log command prompts for branch and prints commit log`() {
        val gitDir = createGitDir(head = "ref: refs/heads/main", branches = emptyList())
        val base = "0000000000000000000000000000000000000001"
        val head = "0000000000000000000000000000000000000002"
        writeBranch(gitDir, "main", head)
        writeCommit(gitDir, base, parents = emptyList(), committerEpoch = "1639272696", message = "simple hello")
        writeCommit(gitDir, head, parents = listOf(base), committerEpoch = "1639273588", message = "maybe hello")

        val output = runCli("${gitDir.absolutePath}\nlog\nmain\n")

        assertEquals(
            "Enter .git directory location:\n" +
                "Enter command:\n" +
                "Enter branch name:\n" +
                "Commit: $head\n" +
                "Kalinka Kali.k4@email.com commit timestamp: 2021-12-11 22:46:28 -03:00\n" +
                "maybe hello\n" +
                "\n" +
                "Commit: $base\n" +
                "Kalinka Kali.k4@email.com commit timestamp: 2021-12-11 22:31:36 -03:00\n" +
                "simple hello",
            output,
        )
    }

    private fun runCli(input: String): String {
        val originalIn = System.`in`
        val originalOut = System.out
        val output = ByteArrayOutputStream()

        try {
            System.setIn(ByteArrayInputStream(input.toByteArray()))
            System.setOut(PrintStream(output))
            main()
        } finally {
            System.setIn(originalIn)
            System.setOut(originalOut)
        }

        return output.toString().replace("\r\n", "\n")
    }

    private fun createGitDir(
        head: String,
        branches: List<String>,
    ): File {
        val gitDir = createTempDirectory().toFile()
        File(gitDir, "HEAD").writeText(head)

        branches.forEach { branch ->
            File(gitDir, "refs/heads/$branch").apply {
                parentFile.mkdirs()
                writeText("0000000000000000000000000000000000000000")
            }
        }

        return gitDir
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
