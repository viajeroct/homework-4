package homework04

import kotlin.test.Test
import kotlin.test.assertEquals

class HTMLParserTest {
    private fun launch(s: String): String {
        val parsers = HTMLParser()
        val code = parsers.run(parsers.parseHTML(), s)
        if (code is Failure) {
            return "-1"
        }
        return (code as Success).data
    }

    @Test
    fun testHTML() {
        assertEquals(
            "<body><p>nikita</p></body>",
            launch("nikita")
        )
        assertEquals(
            "<body><p>nikita</p><p>go</p></body>",
            launch("<p>nikita<p>go</p></p>")
        )
        assertEquals(
            "<body><p>nikita</p><div><p>go</p></div></body>",
            launch("<p>nikita<div>go</div></p>")
        )
        assertEquals(
            "<body><div><p>nikita</p></div></body>",
            launch("<body><div>nikita</body>")
        )
        assertEquals(
            "<body><div><p>go</p><div><p>nikita030</p></div></div></body>",
            launch("<body><div>go<div>nikita030</body>")
        )

        assertEquals(
            "-1",
            launch("<body><div>go<div>nikita</body><body>go</body>")
        )
        assertEquals(
            "-1",
            launch("<body><div>nikita</div></body>nikita")
        )
        assertEquals(
            "<body><div><p></p></div></body>",
            launch("<div><p></div>")
        )
        assertEquals(
            "<body><div><div><p>nikiTa</p><p>go</p><div></div></div></div></body>",
            launch("<body><div><div><p>nikiTa</p>go<div>")
        )
        assertEquals(
            "<body></body>",
            launch("")
        )
        assertEquals(
            "<body><p>  </p></body>",
            launch("    <body>  </body>   ")
        )
        assertEquals(
            "<body><div><div></div><p>nikita</p><p>nikita</p><div></div></div></body>",
            launch("<body><div><div></div><p>nikita</p><p>nikita<div></div></div></body>")
        )
    }
}
