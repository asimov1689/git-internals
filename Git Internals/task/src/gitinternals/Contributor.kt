package gitinternals

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class Contributor(
    val name: String,
    val email: String,
    val epochSeconds: Long,
    val zoneOffset: ZoneOffset,
) {
    fun formattedTimestamp(): String = TIMESTAMP_FORMATTER.format(Instant.ofEpochSecond(epochSeconds).atZone(zoneOffset))

    companion object {
        private val TIMESTAMP_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxx")
    }
}
