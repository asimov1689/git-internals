package gitinternals

fun main() {
    println("Enter git object location:")
    val path = readln()
    val gitObject = GitObjectReader().read(path)
    println("${gitObject.type} ${gitObject.size}")
    println(gitObject.content)
}