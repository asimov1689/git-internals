package gitinternals.integration

import gitinternals.main
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
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
}
