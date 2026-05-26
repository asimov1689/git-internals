package gitinternals

import java.time.ZoneOffset

class CommitParser {
    fun parse(body: String): GitObject.Commit {
        val lines = body.lines()

        var tree = ""
        val parents = mutableListOf<String>()
        var authorRaw = ""
        var committerRaw = ""
        var blankIndex = lines.size

        for ((index, line) in lines.withIndex()) {
            if (line.isBlank()) {
                blankIndex = index
                break
            }
            when {
                line.startsWith("tree ") -> tree = line.removePrefix("tree ")
                line.startsWith("parent ") -> parents += line.removePrefix("parent ")
                line.startsWith("author ") -> authorRaw = line.removePrefix("author ")
                line.startsWith("committer ") -> committerRaw = line.removePrefix("committer ")
            }
        }

        require(tree.isNotEmpty()) { "Missing 'tree' field" }
        require(authorRaw.isNotEmpty()) { "Missing 'author' field" }
        require(committerRaw.isNotEmpty()) { "Missing 'committer' field" }

        val message = lines.drop(blankIndex + 1).joinToString("\n").trimEnd('\n')

        return GitObject.Commit(
            tree = tree,
            parents = parents,
            author = parseContributor(authorRaw),
            committer = parseContributor(committerRaw),
            message = message,
        )
    }

    private fun parseContributor(raw: String): Contributor {
        val tokens = raw.trim().split(" ")
        require(tokens.size >= 4) { "Malformed contributor line: '$raw'" }

        val epoch =
            tokens[tokens.size - 2].toLongOrNull()
                ?: throw IllegalArgumentException("Non-numeric epoch in: '$raw'")

        return Contributor(
            name = tokens.dropLast(3).joinToString(" "),
            email = tokens[tokens.size - 3].removeSurrounding("<", ">"),
            epochSeconds = epoch,
            zoneOffset = parseZoneOffset(tokens.last()),
        )
    }

    private fun parseZoneOffset(raw: String): ZoneOffset {
        require(raw.length == 5 && (raw[0] == '+' || raw[0] == '-')) {
            "Invalid timezone offset: '$raw'"
        }
        return ZoneOffset.of("${raw.substring(0, 3)}:${raw.substring(3)}")
    }
}
