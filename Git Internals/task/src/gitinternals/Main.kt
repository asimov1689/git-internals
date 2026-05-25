package gitinternals

fun main() {
    println("Enter .git directory location:")
    val dir = readln().trimEnd('/')
    println("Enter git object hash:")
    val hash = readln()
    val path = "$dir/objects/${hash.substring(0, 2)}/${hash.substring(2)}"
    val gitObject = GitObjectReader().read(path)
    println("type:${gitObject.type} length:${gitObject.size}")
}