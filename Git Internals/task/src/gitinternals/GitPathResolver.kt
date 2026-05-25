package gitinternals

class GitPathResolver {

    fun resolve(gitDir: String, hash: String): String {
        require(hash.length == 40) { "Git hash must be exactly 40 characters, got: ${hash.length}" }
        val dir    = hash.substring(0, 2)
        val file   = hash.substring(2)
        return "$gitDir/objects/$dir/$file"
    }
}