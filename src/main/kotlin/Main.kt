import java.io.File
import java.io.FileInputStream
import java.util.zip.InflaterInputStream

fun main() {
    val dir = println("Enter .git directory location:").run { readln() }
    val hash = println("Enter git object hash:").run { readln() }

    val fileDir = dir.plus("/objects/${hash.take(2)}/${hash.drop(2)}")

    InflaterInputStream(FileInputStream(File(fileDir)))
        .readBytes()
        .decodeToString()
        .split('\u0000')[0]
        .split(' ')
        .let {
            print("type:${it[0]} length:${it[1]}")
        }
}