package homework04

data class AttributeInfo(
    val attribute_name_index: Long,
    val attribute_length: Long,
    val info: List<UByte>
)

data class ClassFile(
    val magic: Long,
    val minor_version: Long,
    val major_version: Long,
    val constant_pool_count: Long,
    val cpInfo: List<CpInfo>,
    val access_flags: Long,
    val this_class: Long,
    val super_class: Long,
    val interfaces_count: Long,
    val interfaces: List<*>,
    val fields_count: Long,
    val fields: List<FieldInfo>,
    val methods_count: Long,
    val methods: List<MethodInfo>,
    val attributes_count: Long,
    val attributes: List<AttributeInfo>
)

data class FieldInfo(
    val access_flags: Long,
    val name_index: Long,
    val descriptor_index: Long,
    val attributes_count: Long,
    val attributes: List<AttributeInfo>
)

data class MethodInfo(
    val access_flags: Long,
    val name_index: Long,
    val descriptor_index: Long,
    val attributes_count: Long,
    val attributes: List<AttributeInfo>
)

data class CpInfo(
    val tag: UByte,
    val info: List<*>
) {
    fun convertToName(): String {
        assert(tag == 1.toUByte())
        return String(info.lt()[1].lt().map { it.uba()[0].toByte() }.toByteArray())
    }
}

enum class AccessType {
    CLASS, FIELD, METHOD
}

object AccessFlags {
    private val class_accesses = listOf(0x0001, 0x0010, 0x0020, 0x0200, 0x0400, 0x1000, 0x2000, 0x4000, 0x8000).zip(
        listOf("public", "final", "super", "interface", "abstract", "synthetic", "annotation", "enum", "module")
    )
    private val field_accesses = listOf(0x0001, 0x0002, 0x0004, 0x0008, 0x0010, 0x0040, 0x0080, 0x1000, 0x4000).zip(
        listOf("public", "private", "protected", "static", "final", "volatile", "transient", "synthetic", "enum")
    )
    private val method_accesses = listOf(
        0x0001, 0x0002, 0x0004, 0x0008, 0x0010, 0x0020, 0x0040, 0x0080, 0x0100,
        0x0400, 0x0800, 0x1000
    ).zip(
        listOf(
            "public", "private", "protected", "static", "final", "synchronized",
            "bridge", "varargs", "native", "abstract", "strict", "synthetic"
        )
    )
    val accesses = mapOf(
        AccessType.CLASS to class_accesses,
        AccessType.FIELD to field_accesses,
        AccessType.METHOD to method_accesses
    )
}
