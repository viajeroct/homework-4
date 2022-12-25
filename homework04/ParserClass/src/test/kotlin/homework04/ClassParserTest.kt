package homework04

import kotlin.test.Test
import kotlin.test.assertEquals

class ClassParserTest {
    @Test
    fun testClassFile() {
        val expect = "Version: 61.0\n" +
                "Class: public super var/Nikita\n" +
                "Super class: java/util/concurrent/atomic/AtomicInteger\n" +
                "Interfaces: java/lang/Comparable, java/lang/Runnable\n" +
                "Fields:\n" +
                "private final int field1\n" +
                "\tAttributes: ConstantValue\n" +
                "public final int field2\n" +
                "\tAttributes: ConstantValue\n" +
                "private int[][][] field4\n" +
                "\tAttributes: \n" +
                "protected java/math/BigInteger field3\n" +
                "\tAttributes: \n" +
                "Methods:\n" +
                "public void <init>()\n" +
                "\tAttributes: Code\n" +
                "public static java/lang/String method1(int param0, double param1, int[][][] param2, java/math/BigInteger param3)\n" +
                "\tAttributes: Code, Deprecated, RuntimeVisibleAnnotations\n" +
                "private void method2(int param0)\n" +
                "\tAttributes: Code\n" +
                "public int compareTo(var/Nikita param0)\n" +
                "\tAttributes: Code\n" +
                "public void run()\n" +
                "\tAttributes: Code\n" +
                "public bridge synthetic int compareTo(java/lang/Object param0)\n" +
                "\tAttributes: Code\n" +
                "Attributes: Signature, SourceFile, BootstrapMethods, InnerClasses\n"

        val pathToClassFile =
            "/home/nikita/kotlin-course/kotlin-hse-2022/homework04/ParserClass/src/main/kotlin/var/Nikita.class"

        val parsers = ClassParser()
        val bytes = readClassFile(pathToClassFile)
        val parsedData = parsers.parseClass()(bytes)
        val classFile = convertToClassFile((parsedData as Success).data)
        val s = convertToReadableFormat(classFile)
        assertEquals(expect, s)
    }
}
