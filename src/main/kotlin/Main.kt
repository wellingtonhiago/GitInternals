import java.io.File
import java.io.FileInputStream
import java.util.zip.InflaterInputStream

fun main() {
    println("Enter git object location:")
    val path = readln()
    val file = File(path)
    val byteArray2 = InflaterInputStream(FileInputStream(file)).readAllBytes()
    byteArray2.forEach { print(if (it.toInt() == 0) "\n" else Char(it.toInt())) }
}