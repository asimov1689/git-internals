package gitinternals

fun main() {
    println("Enter .git directory location:")
    val gitDir = readln()

    println("Enter command:")
    when (readln()) {
        "cat-file" -> {
            println("Enter git object hash:")
            val hash = readln()

            val path = GitPathResolver().resolve(gitDir, hash)
            val obj = GitObjectReader().read(path)
            print(GitObjectFormatter.format(obj))
        }

        "list-branches" -> {
            print(BranchLister().list(gitDir).joinToString("\n"))
        }

        else -> {
            error("Unsupported command")
        }
    }
}
