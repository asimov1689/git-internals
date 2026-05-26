package gitinternals

sealed class GitObject {
    data class Blob(
        val size: Int,
        val content: String,
    ) : GitObject()

    data class Commit(
        val tree: String,
        val parents: List<String>,
        val author: Contributor,
        val committer: Contributor,
        val message: String,
    ) : GitObject()

    data class Tree(
        val entries: List<TreeEntry>,
    ) : GitObject()
}
