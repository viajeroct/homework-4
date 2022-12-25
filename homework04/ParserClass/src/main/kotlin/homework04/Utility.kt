package homework04

import java.io.File

@Suppress("UNCHECKED_CAST")
fun Any?.uba(): List<UByte> = this as List<UByte>

@Suppress("UNCHECKED_CAST")
fun Any?.la(): List<Long> = this as List<Long>

operator fun Any?.get(pos: Int): Any? = this.lt()[pos]
fun Any?.len(): Long = this.lt()[0].la()[0]
fun Any?.asArray(): List<*> = this.lt()[1].lt()
fun Any?.asUByteArray(): List<UByte> = this.lt()[1].uba()
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

fun readClassFile(filename: String): List<UByte> =
    File(filename).readBytes().map { it.toUByte() }

fun getAccessFlags(access_flags: Long, access_type: AccessType): String {
    val res = arrayListOf<String>()
    for ((code, mod) in AccessFlags.accesses[access_type]!!) {
        if ((code and access_flags.toInt()) != 0) {
            res.add(mod)
        }
    }
    return res.joinToString(separator = " ") { it }
}
