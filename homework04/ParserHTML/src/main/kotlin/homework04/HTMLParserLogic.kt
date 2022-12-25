package homework04

import java.io.File

fun merge(a: Success<String>, b: Result<String>): Result<String> {
    return when (b) {
        is Failure -> b
        is Success -> Success(a.data + b.data, a.consumed + b.consumed)
    }
}

class HTMLParser : Parsers, SimpleParsers, Combinators {
    private fun parseTag(tags: List<String>): StringParser<String> = { input: String ->
        run(or(tags.map { expectString(it) }), input)
    }

    data class Tag(val tag: String, var isAlive: Boolean = true) {
        fun get() = if (isAlive) tag else ""
    }

    private fun parseInner(
        tags: List<String>,
        endTags: ArrayList<Tag> = arrayListOf()
    ): StringParser<String> = { input: String ->
        when (input.firstOrNull()) {
            null -> {
                endTags.reverse()
                val cur = endTags.joinToString(separator = "") { it.get() }
                endTags.clear()
                Success(cur, cur.length)
            }

            '<' -> when (val parsed = parseTag(tags)(input)) {
                is Failure -> {
                    val last = endTags.removeLastOrNull()
                    if (last == null) Failure(Location(input).toError("Unexpected tag ${parsed.errorInfo}"))
                    else {
                        val closeParsing = parseTag(listOf(last.tag))(input)
                        merge(
                            Success(last.get(), last.get().length),
                            if (closeParsing is Failure) parseInner(tags, endTags)(input)
                            else parseInner(tags, endTags)(input.drop(last.tag.length))
                        )
                    }
                }

                else -> {
                    var cur = ""
                    val foundTag = (parsed as Success).data
                    if ((foundTag == "<p>" || foundTag == "<div>") &&
                        endTags.lastOrNull() == Tag("</p>")
                    ) {
                        cur = "</p>"
                        endTags.last().isAlive = false
                    }
                    endTags.add(Tag("</${foundTag.drop(1)}"))
                    val res = cur + foundTag
                    merge(
                        Success(res, res.length),
                        parseInner(tags, endTags)(input.drop(foundTag.length))
                    )
                }
            }

            else -> {
                val s = (readSeq(or(listOf(allExcept('<', '<'))))(input) as Success).data
                var cur = ""
                if (endTags.lastOrNull() != Tag("</p>") && s.trim().isNotEmpty()) {
                    cur = "<p>"
                    endTags.add(Tag("</p>"))
                }
                val res = cur + s
                merge(Success(res, res.length), parseInner(tags, endTags)(input.drop(s.length)))
            }
        }
    }

    fun parseHTML(): StringParser<String> = { inp ->
        val input = inp.trim()
        val body = parseTag(listOf("<body>"))(input)
        var res = merge(
            Success("<body>", 6), parseInner(
                listOf("<p>", "<div>"),
                arrayListOf(Tag("</body>", true))
            )(if (body is Failure) input else input.drop(6))
        )
        if (res is Success) {
            if (!res.data.endsWith("</body>"))
                res = Failure(Location(input).toError("Body is body."))
        }
        res
    }

    override fun <T : Any, A> run(p: CommonParser<T, A>, input: A): Result<T> = p(input)
}

fun main(args: Array<String>) {
    val inputFile = args[0]
    val outputFile = args[1]

    val parser = HTMLParser()
    val data = File(inputFile).readText()
    val toWrite = when (val parsedData = parser.run(parser.parseHTML(), data)) {
        is Success -> parsedData.data
        is Failure -> parsedData.errorInfo.toString()
    }

    File(outputFile).bufferedWriter().use { out ->
        out.write(toWrite)
    }
}
