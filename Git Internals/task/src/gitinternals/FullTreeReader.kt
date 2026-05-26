package gitinternals

class FullTreeReader(
    private val pathResolver: GitPathResolver = GitPathResolver(),
    private val objectReader: GitObjectReader = GitObjectReader(),
) {
    fun read(
        gitDir: String,
        commitHash: String,
    ): String {
        val commit = readCommit(gitDir, commitHash)
        return readTree(gitDir, commit.tree).joinToString("\n")
    }

    private fun readTree(
        gitDir: String,
        treeHash: String,
        prefix: String = "",
    ): List<String> {
        val tree = readTreeObject(gitDir, treeHash)
        val paths = mutableListOf<String>()

        tree.entries.forEach { entry ->
            val path = "$prefix${entry.name}"
            if (entry.permissions == DIRECTORY_PERMISSIONS) {
                paths += readTree(gitDir, entry.sha1, "$path/")
            } else {
                paths += path
            }
        }

        return paths
    }

    private fun readCommit(
        gitDir: String,
        hash: String,
    ): GitObject.Commit {
        val obj = objectReader.read(pathResolver.resolve(gitDir, hash))
        require(obj is GitObject.Commit) { "Object $hash is not a commit" }
        return obj
    }

    private fun readTreeObject(
        gitDir: String,
        hash: String,
    ): GitObject.Tree {
        val obj = objectReader.read(pathResolver.resolve(gitDir, hash))
        require(obj is GitObject.Tree) { "Object $hash is not a tree" }
        return obj
    }

    companion object {
        private const val DIRECTORY_PERMISSIONS = "40000"
    }
}
