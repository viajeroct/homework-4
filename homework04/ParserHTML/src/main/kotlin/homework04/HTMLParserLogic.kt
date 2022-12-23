package homework04

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

            ' ',
            in '0'..'9',
            in 'A'..'Z',
            in 'a'..'z' -> {
                var cur = ""
                if (endTags.lastOrNull()?.tag != "</p>") {
                    cur = "<p>"
                    endTags.add(Tag("</p>"))
                }
                val s = (readSeq(
                    or(listOf(charRange('a', 'z'), charRange('A', 'Z'), charRange('0', '9'), char(' ')))
                )(input) as Success).data
                val res = cur + s
                merge(Success(res, res.length), parseInner(tags, endTags)(input.drop(s.length)))
            }

            else -> when (val parsed = parseTag(tags)(input)) {
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
                    if ((foundTag == "<p>" || foundTag == "<div>") && endTags.lastOrNull()?.tag == "</p>") {
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

fun main() {
    val parsers = HTMLParser()
    println(
        parsers.run(
            parsers.parseHTML(),
            "abcd<div><p>efgh0</div>"
        )
    )
}
