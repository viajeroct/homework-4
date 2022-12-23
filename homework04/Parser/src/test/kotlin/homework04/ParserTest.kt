package homework04

import kotlin.test.Test
import kotlin.test.assertEquals

class TestParser : Parsers, SimpleParsers, Combinators {
    override fun <T : Any, A> run(p: CommonParser<T, A>, input: A): Result<T> = p(input)
}

class ParserTest {
    private fun launch(s: String, parser: StringParser<String>): String {
        val parsers = TestParser()
        val code = parsers.run(parser, s)
        if (code is Failure) {
            return "-1"
        }
        return (code as Success).data
    }

    @Test
    fun testOtherFunctions() {
        assertEquals(3405691582, convertToLong(listOf(202U, 254U, 186U, 190U)))
    }

    @Test
    fun testSimpleParsersAndCombinators() {
        val parser = TestParser()

        with(parser) {
            assertEquals(
                "AZaz0190190sjssjjsA",
                launch(
                    "AZaz0190190sjssjjsA",
                    readSeq(
                        or(listOf(charRange('a', 'z'), charRange('A', 'Z'), charRange('0', '9')))
                    )
                )
            )
        }

        with(parser) {
            assertEquals(
                "nikita",
                launch("nikita", expectString("nikita"))
            )
        }

        with(parser) {
            assertEquals(
                "-1",
                launch("ikita", expectString("nikita"))
            )
        }
    }
}
