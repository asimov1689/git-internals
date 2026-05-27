package gitinternals

import java.io.File

class BranchLister {
    fun list(gitDir: String): List<String> {
        val headsDir = File(gitDir, "refs/heads")
        val currentBranch = currentBranch(gitDir)

        return headsDir
            .walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(headsDir).path.replace(File.separatorChar, '/') }
            .sorted()
            .map { branch ->
                if (branch == currentBranch) {
                    "* $branch"
                } else {
                    "  $branch"
                }
            }.toList()
    }

    private fun currentBranch(gitDir: String): String? {
        val head = File(gitDir, "HEAD")
        if (!head.isFile) return null

        val refPrefix = "ref: refs/heads/"
        val content = head.readText().trim()
        return content.takeIf { it.startsWith(refPrefix) }?.removePrefix(refPrefix)
    }
}
