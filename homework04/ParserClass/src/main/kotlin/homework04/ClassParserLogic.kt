package homework04

import java.lang.StringBuilder

class ClassParser : Parsers, SimpleParsers, Combinators {
    fun parseArgument(): StringParser<String> = marker@{ input: String ->
        when (val s = input.first()) {
            'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z', 'V' -> Success(convert(input.first()), 1)
            '[' -> {
                val pars = readSeq(char('['))(input) as Success
                val type = parseArgument()(input.drop(pars.consumed))
                if (type is Failure) {
                    return@marker type
                }
                type as Success
                Success(
                    type.data + "[]".repeat(pars.consumed),
                    pars.consumed + type.consumed
                )
            }

            'L' -> {
                val objectType = readSeq(allExcept(';', ';'))(input) as Success
                Success(
                    input.substring(1, objectType.consumed),
                    objectType.consumed + 1
                )
            }

            else -> Failure(Location(input).toError("Unexpected symbol $s."))
        }
    }

    fun parseMethodSignature(): StringParser<List<String>> = marker@{ input: String ->
        if (input.first() != '(') {
            return@marker Failure(Location(input).toError("Input must start with (, not with ${input.first()}"))
        }
        var total = 1
        val res: ArrayList<String> = arrayListOf()
        while (input[total] != ')') {
            val cur = parseArgument()(input.drop(total))
            if (cur is Failure) {
                return@marker Failure(cur.errorInfo)
            }
            cur as Success
            total += cur.consumed
            res.add(cur.data)
        }
        val ret = parseArgument()(input.drop(total + 1))
        if (ret is Failure) {
            return@marker Failure(ret.errorInfo)
        }
        ret as Success
        Success(res + ret.data, total + ret.consumed + 1)
    }

    fun parseClass() = { input: List<UByte> ->
        seqLaunch(
            expectBytes(listOf(202U, 254U, 186U, 190U)), /* magic */
            readBytesAndConvert(2), /* minor */
            readBytesAndConvert(2), /* major */
            parseArray(
                readBytes(2), /* pool count */
                flatMap(
                    readBytes(1),
                    { tag ->
                        when (val x = tag[0].toInt()) {
                            7, 8, 16, 19, 20 -> readBytesEraseType(2)
                            15 -> readBytesEraseType(3)
                            9, 10, 11, 3, 4, 12, 17, 18 -> readBytesEraseType(4)
                            5, 6 -> readBytesEraseType(8)
                            else -> {
                                assert(x == 1)
                                parseArray(readBytes(2), readBytes(1))
                            }
                        }
                    },
                    { x, y -> mergeLists(x, y) }
                ),
                1
            ),
            readBytesAndConvert(2), /* access */
            readBytesAndConvert(2), /* this */
            readBytesAndConvert(2), /* super */
            parseArray(
                readBytes(2), /* interfaces */
                readBytes(2)
            ),
            parseArray(
                readBytes(2), /* fields */
                seqLaunch(
                    readBytesAndConvert(2), /* access */
                    readBytesAndConvert(2), /* name */
                    readBytesAndConvert(2), /* descriptor */
                    parseArray(
                        readBytes(2), /* attributes */
                        seqLaunch(
                            readBytesAndConvert(2), /* attribute_name_index */
                            parseArray(readBytes(4), readBytes(1))
                        )
                    )
                )
            ),
            parseArray(
                readBytes(2), /* methods_count */
                seqLaunch(
                    readBytesAndConvert(2), /* access */
                    readBytesAndConvert(2), /* name */
                    readBytesAndConvert(2), /* descriptor */
                    parseArray(
                        readBytes(2), /* attributes */
                        seqLaunch(
                            readBytesAndConvert(2), /* attribute_name_index */
                            parseArray(readBytes(4), readBytes(1))
                        )
                    )
                )
            ),
            parseArray(
                readBytes(2), /* attributes_count */
                seqLaunch(
                    readBytesAndConvert(2),
                    parseArray(readBytes(4), readBytes(1))
                )
            ),
            expectEnd()
        )(input)
    }

    override fun <T : Any, A> run(p: CommonParser<T, A>, input: A): Result<T> = p(input)
}

fun convertToClassFile(data: List<*>): ClassFile = ClassFile(
    data[0].uba().toNumber(),
    data[1] as Long,
    data[2] as Long,
    data[3].len(),
    data[3].asArray().map { CpInfo(it[0].uba()[0], it.asArray()) },
    data[4] as Long,
    data[5] as Long,
    data[6] as Long,
    data[7].len(),
    data[7].asArray(),
    data[8].len(),
    data[8].asArray().map { field ->
        FieldInfo(
            field[0] as Long,
            field[1] as Long,
            field[2] as Long,
            field[3].len(),
            field[3].asArray().map {
                AttributeInfo(
                    it[0] as Long,
                    it[1].len(),
                    it[1].asUByteArray()
                )
            }
        )
    },
    data[9].len(),
    data[9].asArray().map { method ->
        MethodInfo(
            method[0] as Long,
            method[1] as Long,
            method[2] as Long,
            method[3].len(),
            method[3].asArray().map {
                AttributeInfo(
                    it[0] as Long,
                    it[1].len(),
                    it[1].asUByteArray()
                )
            }
        )
    },
    data[10].len(),
    data[10].asArray().map {
        AttributeInfo(
            it[0] as Long,
            it[1].len(),
            it[1].asUByteArray()
        )
    }
)

fun convertToReadableFormat(res: ClassFile): String {
    val answer = StringBuilder()

    val getCpInfoObject = { pos: Long ->
        val pos1 = res.cpInfo[pos.toInt() - 1]
        assert(pos1.tag == 7.toUByte())
        val obj = res.cpInfo[convertToLong(pos1.info.uba()).toInt() - 1]
        obj.convertToName()
    }

    val getCpInfoObjectDirect = { pos: Long ->
        val obj = res.cpInfo[pos.toInt() - 1]
        assert(obj.tag == 1.toUByte())
        obj.convertToName()
    }

    answer.append("Version: ${res.major_version}.${res.minor_version}").append("\n")
    answer.append("Class: ${getAccessFlags(res.access_flags, AccessType.CLASS)} ${getCpInfoObject(res.this_class)}")
        .append("\n")
    answer.append("Super class: ${getCpInfoObject(res.super_class)}").append("\n")

    val interfacesNames = res.interfaces.map {
        getCpInfoObject(convertToLong(it.uba()))
    }
    answer.append("Interfaces: ${interfacesNames.joinToString(", ")}").append("\n")

    val parsers = ClassParser()

    answer.append("Fields:").append("\n")
    res.fields.forEach { field ->
        val signature = parsers.parseArgument()(getCpInfoObjectDirect(field.descriptor_index))
        if (signature is Failure) {
            return signature.errorInfo.toString()
        }
        signature as Success
        answer.append(
            "${
                getAccessFlags(
                    field.access_flags, AccessType.FIELD
                )
            } ${signature.data} ${getCpInfoObjectDirect(field.name_index)}"
        ).append("\n")
        answer.append("\tAttributes: ")
        answer.append(field.attributes.joinToString(separator = ", ") { getCpInfoObjectDirect(it.attribute_name_index) })
            .append("\n")
    }

    answer.append("Methods:").append("\n")
    res.methods.forEach { method ->
        val signatureR = parsers.parseMethodSignature()(getCpInfoObjectDirect(method.descriptor_index))
        if (signatureR is Failure) {
            return signatureR.errorInfo.toString()
        }
        val signature = (signatureR as Success).data
        val params = signature.withIndex().map { (i, it) -> "$it param$i" }
        val fields = params.dropLast(1).joinToString(separator = ", ") { it }
        answer.append(
            "${
                getAccessFlags(
                    method.access_flags,
                    AccessType.METHOD
                )
            } ${signature.last()} ${getCpInfoObjectDirect(method.name_index)}($fields)"
        ).append("\n")
        answer.append("\tAttributes: ")
        answer.append(method.attributes.joinToString(separator = ", ") { getCpInfoObjectDirect(it.attribute_name_index) })
            .append("\n")
    }

    answer.append("Attributes: ")
    answer.append(res.attributes.joinToString(separator = ", ") { getCpInfoObjectDirect(it.attribute_name_index) })
        .append("\n")

    return answer.toString()
}

fun main() {
    val pathToClassFile =
        "/home/nikita/kotlin-course/kotlin-hse-2022/homework04/ParserClass/src/main/kotlin/var/Nikita.class"
//    val pathToClassFile =
//        "/home/nikita/kotlin-course/kotlin-hse-2022/homework04/ParserClass/src/main/kotlin/corrupted/C.class"

    val parsers = ClassParser()
    val bytes = readClassFile(pathToClassFile)
    val parsedData = parsers.parseClass()(bytes)
    if (parsedData is Failure) {
        println(parsedData)
        return
    }
    val classFile = convertToClassFile((parsedData as Success).data)
    val s = convertToReadableFormat(classFile)
    print(s)
}
