import java.io.File
import java.io.FileInputStream
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.InflaterInputStream

fun main() {
    val dir = println("Enter .git directory location:").run { readln() }

    when (println("Enter command:").run { readln() }) {
        "cat-file" -> {
            val hash = println("Enter git object hash:").run { readln() }
            val path = "$dir/objects/${hash.take(2)}/${hash.drop(2)}"
            val content =
                InflaterInputStream(FileInputStream(path)).readBytes()
            val type = String(content.sliceArray(0 until content.indexOf(0))).split(' ')[0]
            val bytes = content.slice(content.indexOf(0) + 1 until content.size).toByteArray()

            when (type) {
                "blob" -> println(Blob(bytes))
                "commit" -> println(Commit(bytes))
                "tree" -> println(Tree(bytes))
            }
        }
        "list-branches" -> {
            val path = "$dir/refs/heads"
            val branches = File(path).listFiles().map { it.name }.sorted()
            val currentBranch = File("$dir/HEAD").readText().split('/').last().trim()

            branches.forEach {
                if (it == currentBranch) println("* $it")
                else println("  $it")
            }
        }

        else -> println("Unknown command")
    }
}

class Tree(private val bytes: ByteArray) {
    data class TreeEntry(val permissionsMetadata: String, val filename: String, val sha1: String) {
        override fun toString(): String = "$permissionsMetadata $sha1 $filename"
    }

    private val entries: Set<TreeEntry> = scanEntries()

    private fun scanEntries(): Set<TreeEntry> {
        // [firstPermissionNumber][space][firstFileName][nullChar][firstHash][secondPermissionNumber][space][secondFileName][nullChar][secondHash][thirdPermissionNumber]...
        val result = mutableSetOf<TreeEntry>()
        var startPos = 0 // index of entry start
        bytes.forEachIndexed { index, byte ->
            if (byte == 0.toByte() && index > startPos) {
                val info = String(bytes.slice(startPos until index).toByteArray()).split(" ")
                startPos = index + 21
                result.add(
                    TreeEntry(info.component1(), info.component2(),
                        bytes.slice(index + 1 until startPos).joinToString("") { "%02x".format(it) })
                )
            }
        }
        return result
    }

    override fun toString() = "*TREE*\n${entries.joinToString("\n")}"
}

class Blob(private val bytes: ByteArray) {
    override fun toString() = "*BLOB*\n${String(bytes)}"
}

class Commit(bytes: ByteArray) {
    private val sc = Scanner(String(bytes))
    private val tree = Pair(sc.next(), sc.next()).second
    private val parents = scanParents()
    private val authorName = sc.next()
    private val authorEmail = sc.next().filter { it != '>' && it != '<' }
    private val originalTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(sc.nextLong()), ZoneId.of(sc.next()))
    private val committerName = Pair(sc.next(), sc.next()).second
    private val committerEmail = sc.next().filter { it != '>' && it != '<' }
    private val commitTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(sc.nextLong()), ZoneId.of(sc.next()))
    private val message = scanMessage()

    private fun scanParents(): Set<String> {
        val parentsList = mutableSetOf<String>()
        while (sc.next() == "parent") parentsList.add(sc.next())
        return parentsList.toSet()
    }

    private fun scanMessage(): String {
        sc.nextLine()
        val lines = mutableListOf<String>()
        while (sc.hasNextLine()) lines.add(sc.nextLine())
        return lines.joinToString("\n")
    }

    override fun toString(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
        val result = StringBuilder("*COMMIT*\n")
        result.append("tree: $tree\n")
        if (parents.isNotEmpty()) result.append("parents: ${parents.joinToString(" | ")}\n")
        result.append("author: $authorName $authorEmail original timestamp: ${originalTime.format(formatter)}\n")
        result.append("committer: $committerName $committerEmail commit timestamp: ${commitTime.format(formatter)}\n")
        result.append("commit message:$message")
        return result.toString()
    }
}