package homework04

import java.io.File

@Suppress("UNCHECKED_CAST")
fun Any?.uba(): List<UByte> = this as List<UByte>

@Suppress("UNCHECKED_CAST")
fun Any?.la(): List<Long> = this as List<Long>

fun Any?.lt(): List<*> = this as List<*>

fun convert(s: Char): String = when (s) {
    'B' -> "byte"
    'C' -> "char"
    'D' -> "double"
    'F' -> "float"
    'I' -> "int"
    'J' -> "long"
    'S' -> "short"
    'V' -> "void"
    else -> "boolean"
}

fun List<UByte>.toNumber(): Long = convertToLong(this)

fun readClassFile(filename: String): List<UByte> {
    return File(filename).readBytes().map { it.toUByte() }
}

fun getAccessFlags(access_flags: Long, access_pos: Int): String {
    val res = arrayListOf<String>()
    for ((code, mod) in AccessFlags.accesses[access_pos]) {
        if ((code and access_flags.toInt()) != 0) {
            res.add(mod)
        }
    }
    return res.joinToString(separator = " ") { it }
}
