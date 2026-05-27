fun sumRecursive(n: Int): Int {
    if (n == 1) return 1
    return n + sumRecursive(n - 1)
}

fun main() {
    val n = readLine()!!.toInt()
    print(sumRecursive(n))
}