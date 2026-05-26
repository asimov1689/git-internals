package gitinternals

class GitPathResolver {
    fun resolve(
        gitDir: String,
        hash: String,
    ): String {
        require(hash.length == 40) { "Git hash must be exactly 40 characters, got: ${hash.length}" }
        return "$gitDir/objects/${hash.substring(0, 2)}/${hash.substring(2)}"
    }
}
