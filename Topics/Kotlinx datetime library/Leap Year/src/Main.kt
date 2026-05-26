import kotlinx.datetime.*

fun isLeapYear(year: String): Boolean {
    return try {
        LocalDate(year.toInt(), 2, 29)
        true
    } catch (e: Exception) {
        false
    }
}

fun main() {
    val year = readln()
    println(isLeapYear(year))
}
