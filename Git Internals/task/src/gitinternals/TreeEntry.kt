package gitinternals

data class TreeEntry(
    val permissions: String,
    val sha1: String,
    val name: String,
)
