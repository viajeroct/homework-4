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
            readBytes(2), /* minor */
            readBytes(2), /* major */
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
            readBytes(2), /* access */
            readBytes(2), /* this */
            readBytes(2), /* super */
            parseArray(
                readBytes(2), /* interfaces */
                readBytes(2)
            ),
            parseArray(
                readBytes(2), /* fields */
                seqLaunch(
                    readBytes(2), /* access */
                    readBytes(2), /* name */
                    readBytes(2), /* descriptor */
                    parseArray(
                        readBytes(2), /* attributes */
                        seqLaunch(
                            readBytes(2), /* attribute_name_index */
                            parseArray(readBytes(4), readBytes(1))
                        )
                    )
                )
            ),
            parseArray(
                readBytes(2), /* methods_count */
                seqLaunch(
                    readBytes(2), /* access */
                    readBytes(2), /* name */
                    readBytes(2), /* descriptor */
                    parseArray(
                        readBytes(2), /* attributes */
                        seqLaunch(
                            readBytes(2), /* attribute_name_index */
                            parseArray(readBytes(4), readBytes(1))
                        )
                    )
                )
            ),
            parseArray(
                readBytes(2), /* attributes_count */
                seqLaunch(
                    readBytes(2),
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
    data[1].uba().toNumber(),
    data[2].uba().toNumber(),
    data[3].lt()[0].la()[0],
    data[3].lt()[1].lt().map { CpInfo(it.lt()[0].uba()[0], it.lt()[1].lt()) },
    data[4].uba().toNumber(),
    data[5].uba().toNumber(),
    data[6].uba().toNumber(),
    data[7].lt()[0].la()[0],
    data[7].lt()[1].lt(),
    data[8].lt()[0].la()[0],
    data[8].lt()[1].lt().map { field ->
        FieldInfo(
            field.lt()[0].uba().toNumber(),
            field.lt()[1].uba().toNumber(),
            field.lt()[2].uba().toNumber(),
            field.lt()[3].lt()[0].la()[0],
            field.lt()[3].lt()[1].lt().map {
                AttributeInfo(
                    it.lt()[0].uba().toNumber(),
                    it.lt()[1].lt()[0].la()[0],
                    it.lt()[1].lt()[1].uba()
                )
            }
        )
    },
    data[9].lt()[0].la()[0],
    data[9].lt()[1].lt().map { method ->
        MethodInfo(
            method.lt()[0].uba().toNumber(),
            method.lt()[1].uba().toNumber(),
            method.lt()[2].uba().toNumber(),
            method.lt()[3].lt()[0].la()[0],
            method.lt()[3].lt()[1].lt().map {
                AttributeInfo(
                    it.lt()[0].uba().toNumber(),
                    it.lt()[1].lt()[0].la()[0],
                    it.lt()[1].lt()[1].uba()
                )
            }
        )
    },
    data[10].lt()[0].la()[0],
    data[10].lt()[1].lt().map {
        AttributeInfo(
            it.lt()[0].uba().toNumber(),
            it.lt()[1].lt()[0].la()[0],
            it.lt()[1].lt()[1].uba()
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
    answer.append("Class: ${getAccessFlags(res.access_flags, 0)} ${getCpInfoObject(res.this_class)}").append("\n")
    answer.append("Super class: ${getCpInfoObject(res.super_class)}").append("\n")

    val interfacesNames = res.interfaces.map {
        getCpInfoObject(convertToLong(it.uba()))
    }
    answer.append("Interfaces: ${interfacesNames.joinToString(", ")}").append("\n")

    val parsers = ClassParser()

    answer.append("Fields:").append("\n")
    res.fields.forEach {
        val signature = parsers.parseArgument()(getCpInfoObjectDirect(it.descriptor_index)) as Success
        answer.append(
            "${
                getAccessFlags(
                    it.access_flags, 1
                )
            } ${signature.data} ${getCpInfoObjectDirect(it.name_index)}"
        ).append("\n")
    }

    answer.append("Methods:").append("\n")
    res.methods.forEach { method ->
        val signature = (parsers.parseMethodSignature()(getCpInfoObjectDirect(method.descriptor_index)) as Success).data
        val params = signature.withIndex().map { (i, it) -> "$it param$i" }
        val fields = params.dropLast(1).joinToString(separator = ", ") { it }
        answer.append(
            "${
                getAccessFlags(
                    method.access_flags,
                    2
                )
            } ${signature.last()} ${getCpInfoObjectDirect(method.name_index)}($fields)"
        ).append("\n")
    }
    return answer.toString()
}

fun main() {
    val pathToClassFile =
        "/home/nikita/kotlin-course/kotlin-hse-2022/homework04/ParserClass/src/main/kotlin/corrupted/D.class"

    val parsers = ClassParser()
    val bytes = readClassFile(pathToClassFile)
    val parsedData = parsers.parseClass()(bytes)
    if (parsedData is Failure) {
        println(parsedData)
        return
    }
    val classFile = convertToClassFile((parsedData as Success).data)
    val s = convertToReadableFormat(classFile)
    println(s)
}
