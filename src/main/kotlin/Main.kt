import java.io.FileInputStream
import java.util.zip.InflaterInputStream

fun main() {
    println("Enter .git directory location:")
    val directory = readln()
    println("Enter git object hash:")
    val hash = readln()

    val path = "$directory/objects/${hash.substring(0, 2)}/${hash.substring(2)}"
    val byteArray = InflaterInputStream(FileInputStream(path)).readAllBytes()

    val headerEnd = byteArray.indexOfFirst { it.toInt() == 0 }
    val fullHeader = byteArray
        .filterIndexed { index, _ -> index in 0 until headerEnd }
        .map { it.toInt().toChar() }
        .joinToString("")

    val (type, length) = fullHeader.split(" ")
    println("type:$type length:$length")
}