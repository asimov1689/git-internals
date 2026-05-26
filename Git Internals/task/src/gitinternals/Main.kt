package gitinternals

fun main() {
    println("Enter .git directory location:")
    val gitDir = readln()

    println("Enter git object hash:")
    val hash = readln()

    val path = GitPathResolver().resolve(gitDir, hash)
    val obj = GitObjectReader().read(path)
    val output = GitObjectFormatter.format(obj)

    print(output)
}
