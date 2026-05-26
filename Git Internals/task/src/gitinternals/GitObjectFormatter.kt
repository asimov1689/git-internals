package gitinternals

object GitObjectFormatter {
    fun format(obj: GitObject): String =
        when (obj) {
            is GitObject.Blob -> formatBlob(obj)
            is GitObject.Commit -> formatCommit(obj)
            is GitObject.Tree -> formatTree(obj)
        }

    private fun formatBlob(blob: GitObject.Blob): String = "*BLOB*\n${blob.content}"

    private fun formatCommit(commit: GitObject.Commit): String =
        buildString {
            appendLine("*COMMIT*")
            appendLine("tree: ${commit.tree}")
            when (commit.parents.size) {
                0 -> Unit
                1 -> appendLine("parents: ${commit.parents[0]}")
                else -> appendLine("parents: ${commit.parents.joinToString(" | ")}")
            }
            appendLine("author: ${commit.author.name} ${commit.author.email} original timestamp: ${commit.author.formattedTimestamp()}")
            appendLine(
                "committer: ${commit.committer.name} ${commit.committer.email} commit timestamp: ${commit.committer.formattedTimestamp()}",
            )
            appendLine("commit message:")
            append(commit.message)
        }

    private fun formatTree(tree: GitObject.Tree): String =
        buildString {
            append("*TREE*")
            tree.entries.forEach { entry ->
                append("\n${entry.permissions} ${entry.sha1} ${entry.name}")
            }
        }
}
