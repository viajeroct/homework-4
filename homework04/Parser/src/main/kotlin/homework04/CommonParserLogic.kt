package homework04

data class Location(val input: Any, val offset: Int = 0)
data class ParseError(val stack: Pair<Location, String>)

fun Location.toError(msg: String) = ParseError(this to msg)

sealed class Result<A : Any>
data class Success<A : Any>(val data: A, val consumed: Int) : Result<A>()
data class Failure<A : Any>(val errorInfo: ParseError) : Result<A>()

fun <A : Any, B : Any> move(a: Failure<A>): Failure<B> = Failure(a.errorInfo)

typealias CommonParser<T, A> = (A) -> Result<T>
typealias ByteParser<T> = CommonParser<T, List<UByte>>
typealias StringParser<T> = CommonParser<T, String>

interface Parsers {
    fun <T : Any, A> run(p: CommonParser<T, A>, input: A): Result<T>
}

fun convertToLong(list: List<UByte>): Long {
    var ans = 0L
    var shift = 0
    for (i in list.reversed()) {
        ans = ans or (i.toLong() shl shift)
        shift += 8
    }
    return ans
}

fun mergeLists(a: List<*>, b: List<*>): List<*> = arrayListOf<Any>().apply {
    addAll(listOf(a))
    addAll(listOf(b))
}

interface Combinators {
    fun <A : Any, B : Any> flatMap(
        pa: ByteParser<A>, f: (A) -> ByteParser<B>,
        merge: (A, B) -> B
    ): ByteParser<B> = { input: List<UByte> ->
        when (val mp = pa(input)) {
            is Success -> {
                when (val cp = f(mp.data)(input.drop(mp.consumed))) {
                    is Success -> Success(merge(mp.data, cp.data), mp.consumed + cp.consumed)
                    is Failure -> cp
                }
            }

            is Failure -> move(mp)
        }
    }

    fun parseArray(sz: ByteParser<List<UByte>>, todo: ByteParser<*>, shift: Int = 0): ByteParser<List<*>> =
        marker@{ input: List<UByte> ->
            val szParsed = sz(input)
            if (szParsed is Failure) {
                return@marker Failure(szParsed.errorInfo)
            }
            szParsed as Success
            val arraySize = convertToLong(szParsed.data) - shift
            val actions = List(arraySize.toInt()) { todo }
            val x = seqLaunch(*actions.toTypedArray())(input.drop(szParsed.consumed))
            if (x is Failure) {
                return@marker x
            }
            x as Success
            Success(mergeLists(listOf(arraySize), x.data), x.consumed + szParsed.consumed)
        }

    fun <A> seqLaunch(vararg parsers: CommonParser<*, List<A>>): CommonParser<List<*>, List<A>> =
        marker@{ input: List<A> ->
            var total = 0
            val res = arrayListOf<Any>()
            for (p in parsers) {
                val cur = p(input.drop(total))
                if (cur is Failure) {
                    return@marker Failure(cur.errorInfo)
                }
                total += (cur as Success).consumed
                res.add(cur.data)
            }
            Success(res, total)
        }

    fun <T : Any, A> or(parsers: List<CommonParser<T, A>>): CommonParser<T, A> = { input: A ->
        var result = parsers.first()(input)
        for (cur in parsers.drop(1)) {
            if (result is Failure) result = cur(input)
            else break
        }
        result
    }
}

interface SimpleParsers {
    fun char(c: Char): StringParser<Char> = { input: String ->
        if (input.startsWith(c)) Success(c, 1)
        else Failure(Location(input).toError("Expected: $c"))
    }

    fun allExcept(from: Char, to: Char): StringParser<Char> = { input: String ->
        when (val c = input.first()) {
            in from..to -> Failure(Location(input).toError("Expected all except $from..$to."))
            else -> Success(c, 1)
        }
    }

    fun charRange(from: Char, to: Char): StringParser<Char> = { input: String ->
        when {
            input.isEmpty() -> Failure(Location(input).toError("Empty string"))
            input[0] in from..to -> Success(input[0], 1)
            else -> Failure(Location(input).toError("Expected: $from - $to"))
        }
    }

    fun readSeq(charParser: StringParser<Char>): StringParser<String> = { input: String ->
        when (val cur = charParser(input)) {
            is Failure -> Success("", 0)
            is Success -> {
                val a = cur.data
                val b = (readSeq(charParser)(input.drop(1)) as Success)
                Success(a + b.data, cur.consumed + b.consumed)
            }
        }
    }

    fun expectString(s: String): StringParser<String> = { input: String ->
        if (input.startsWith(s)) Success(s, s.length)
        else Failure(Location(input).toError("Expected string $s"))
    }

    fun expectBytes(s: List<UByte>): ByteParser<List<UByte>> = { input: List<UByte> ->
        if (input.subList(0, s.size) == s) Success(s, s.size)
        else Failure(Location(input).toError("Expected: $s"))
    }

    fun readBytes(n: Int): ByteParser<List<UByte>> = { input: List<UByte> ->
        if (input.size >= n) Success(input.subList(0, n), n)
        else Failure(Location(input).toError("Size must be at least $n."))
    }

    fun readBytesEraseType(n: Int): ByteParser<List<*>> = { input: List<UByte> ->
        if (input.size >= n) Success(input.subList(0, n), n)
        else Failure(Location(input).toError("Size must be at least $n."))
    }

    fun expectEnd(): ByteParser<List<UByte>> = { input: List<UByte> ->
        if (input.isNotEmpty()) Failure(Location(input).toError("End must be empty: $input."))
        else Success(emptyList(), 0)
    }
}
