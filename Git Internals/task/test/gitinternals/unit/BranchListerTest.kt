package gitinternals.unit

import gitinternals.BranchLister
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class BranchListerTest {
    private val lister = BranchLister()

    @Test
    fun `should list branches alphabetically and mark current branch`() {
        val gitDir =
            createGitDir(
                head = "ref: refs/heads/master",
                branches = listOf("feature2", "master", "feature1"),
            )

        assertEquals(
            listOf(
                "  feature1",
                "  feature2",
                "* master",
            ),
            lister.list(gitDir.absolutePath),
        )
    }

    @Test
    fun `should keep nested branch names relative to refs heads`() {
        val gitDir =
            createGitDir(
                head = "ref: refs/heads/feature/api",
                branches = listOf("main", "feature/api"),
            )

        assertEquals(
            listOf(
                "* feature/api",
                "  main",
            ),
            lister.list(gitDir.absolutePath),
        )
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
