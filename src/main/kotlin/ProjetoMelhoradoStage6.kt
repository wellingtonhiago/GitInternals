import java.io.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.zip.InflaterInputStream

fun main() {
    val directory = println("Enter .git directory location:").run { readln() }
    when (println("Enter command:").run { readln() }) {
        "cat-file" -> aboutCatFile(directory)
        "list-branches" -> getListBranches(directory)
        "log" -> printLog(directory)
        else -> print("Unknown command")
    }
}

fun printCommitData(data: List<String>) {
    var i = 0
    print("tree: ${data[i].substringAfter("tree ")}").also { i++ }
    if (data[i].contains("parent")) print("\nparents: ${data[i].substringAfter("parent ")}").also { i++ }
    if (data[i].contains("parent")) print(" | ${data[i].substringAfter("parent ")}").also { i++ }
    val author = data[i].split(" ")
    println("\nauthor: ${author[1]} ${author[2].trim('<', '>')} " +
            "original timestamp: ${getTimestamp(author[3].toLong(), author[4])}").also { i++ }
    val committer = data[i].split(" ")
    println("committer: ${committer[1]} ${committer[2].trim('<', '>')} " +
            "commit timestamp: ${getTimestamp(committer[3].toLong(), committer[4])}").also { i++ }
    println("commit message:").also { (i + 1 until data.size - 1).forEach { println(data[it]) } }
}

fun printTreeData(data: String) {
    val lines = data.split(0.toChar())
    var (number, name) = lines[1].split(" ")
    var hash: String
    (2 until lines.size - 1).forEach { i ->
        hash = lines[i].take(20).map { it.code.toByte() }.joinToString("") { "%02x".format(it) }
        println("$number $hash $name").also {
            lines[i].substring(20).split(" ").let { number = it[0]; name = it[1] }
        }
    }
    hash = lines.last().map { it.code.toByte() }.joinToString("") { "%02x".format(it) }
    println("$number $hash $name")
}

fun aboutCatFile(directory: String) {
    val hash = println("Enter git object hash:").run { readln() }
    val content = decompress("$directory/objects/${hash.take(2)}/${hash.drop(2)}").readBytes()
    val contentList = content.toString(Charsets.UTF_8).split("\n")
    when (true) {
        "blob" in contentList[0] -> println("*BLOB*\n${content.toString(Charsets.UTF_8).split(0.toChar())[1]}")
        "commit" in contentList[0] -> println("*COMMIT*").also { printCommitData(contentList) }
        "tree" in contentList[0] -> println("*TREE*").also {
            printTreeData(content.map { Char(it.toUShort()) }.joinToString(""))
        }

        else -> println("Erro na função aboutCatFile")
    }
}

fun getListBranches(directory: String) {
    val branches = File("$directory/refs/heads").listFiles()
    val current = File("$directory/HEAD").readText().substringAfterLast("/")
    branches.forEach { println(if (it.name in current) "* ${it.name}" else "  ${it.name}") }
}

fun readCommit(directory: String, hash: String, merged: String = "") {
    val data = decompress("$directory/objects/${hash.take(2)}/${hash.drop(2)}").readBytes()
        .toString(Charsets.UTF_8).split("\n")
    var i = 1
    var parent = ""
    var parentMerged = ""
    println("Commit: $hash$merged")
    if ("parent" in data[i]) run { parent = data[i].substringAfter("parent ") }.also { i++ }
    if ("parent" in data[i]) run { parentMerged = data[i].substringAfter("parent ") }.also { i++ }
    i++
    val committer = data[i].split(" ")
    val time = getTimestamp(committer[3].toLong(), committer[4])
    println("${committer[1]} ${committer[2].trim('<', '>')} commit timestamp: $time").also { i++ }
    (i + 1 until data.size - 1).forEach { println(data[it]) }
    println()
    if (merged.isEmpty()) {
        if (parentMerged.isNotEmpty()) readCommit(directory, parentMerged, " (merged)")
        if (parent.isNotEmpty()) readCommit(directory, parent)
    }
}

fun printLog(directory: String) {
    val branchName = println("Enter branch name:").run { readln() }
    val commitObject = File("$directory/refs/heads/$branchName")
    if (commitObject.exists()) readCommit(directory, commitObject.readText().removeSuffix("\n"))
}

fun decompress(filePath: String) = InflaterInputStream(FileInputStream(filePath))

fun getTimestamp(seconds: Long, zone: String): String {
    return Instant.ofEpochSecond(seconds).atZone(ZoneOffset.of(zone))
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxx"))
}