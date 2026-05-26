package gitinternals

import java.io.File

class GitLogReader(
    private val pathResolver: GitPathResolver = GitPathResolver(),
    private val objectReader: GitObjectReader = GitObjectReader(),
) {
    fun read(
        gitDir: String,
        branch: String,
    ): String {
        val entries = mutableListOf<LogEntry>()
        var currentHash: String? = branchHead(gitDir, branch)

        while (currentHash != null) {
            val commit = readCommit(gitDir, currentHash)
            entries += LogEntry(currentHash, commit, merged = false)

            if (commit.parents.size >= 2) {
                val mergedHash = commit.parents[1]
                entries += LogEntry(mergedHash, readCommit(gitDir, mergedHash), merged = true)
            }

            currentHash = commit.parents.firstOrNull()
        }

        return entries.joinToString("\n\n") { it.format() }
    }

    private fun branchHead(
        gitDir: String,
        branch: String,
    ): String = File(gitDir, "refs/heads/$branch").readText().trim()

    private fun readCommit(
        gitDir: String,
        hash: String,
    ): GitObject.Commit {
        val obj = objectReader.read(pathResolver.resolve(gitDir, hash))
        require(obj is GitObject.Commit) { "Object $hash is not a commit" }
        return obj
    }

    private data class LogEntry(
        val hash: String,
        val commit: GitObject.Commit,
        val merged: Boolean,
    ) {
        fun format(): String =
            buildString {
                append("Commit: $hash")
                if (merged) append(" (merged)")
                appendLine()
                appendLine("${commit.committer.name} ${commit.committer.email} commit timestamp: ${commit.committer.formattedTimestamp()}")
                append(commit.message)
            }
    }
}
