package gitinternals

fun main() {
    println("Enter .git directory location:")
    val gitDir = readln()

    println("Enter git object hash:")
    val hash = readln()

    val path      = GitPathResolver().resolve(gitDir, hash)
    val gitObject = GitObjectReader().read(path)

    println("type:${gitObject.type} length:${gitObject.size}")
}