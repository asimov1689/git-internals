package gitinternals

data class GitObject(
    val type: String,
    val size: Int,
    val content: String
)