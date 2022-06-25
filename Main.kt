package svcs

import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

const val CONFIG_HELP = "Get and set a username."
const val ADD_HELP = "Add a file to the index."
const val LOG_HELP = "Show commit logs."
const val COMMIT_HELP = "Save changes."
const val CHECKOUT_HELP = "Restore a file."
const val VCS_DIRECTORY_PATH_NAME = "vcs"
const val COMMITS_DIRECTORY_PATH_NAME = "vcs/commits"
const val CONFIG_FILE_PATH_NAME = "vcs/config.txt"
const val INDEX_FILE_PATH_NAME = "vcs/index.txt"
const val LOG_FILE_PATH_NAME = "vcs/log.txt"

fun main(args: Array<String>) {
    File(VCS_DIRECTORY_PATH_NAME).mkdir()
    File(COMMITS_DIRECTORY_PATH_NAME).mkdir()
    val configFile = File(CONFIG_FILE_PATH_NAME).apply { createNewFile() }
    val indexFile = File(INDEX_FILE_PATH_NAME).apply { createNewFile() }
    val logFile = File(LOG_FILE_PATH_NAME).apply { createNewFile() }
    when (val commend = if (args.isEmpty()) "--help" else args[0]) {
        "--help" -> commendHelp()
        "config" -> config(configFile, inputOption(args))
        "add" -> add(indexFile, inputOption(args))
        "log" -> log(logFile)
        "commit" -> commit(logFile, configFile, indexFile, inputOption(args))
        "checkout" -> checkout(logFile, configFile, indexFile, inputOption(args))
        else -> println("'$commend' is not a SVCS command.")
    }
}

fun log(logFile: File) {
    if (logFile.readText().isEmpty()) {
        println("No commits yet.")
    } else {
        println(logFile.readText())
        println()
    }
}

fun config(fileConfig: File, option: String?) {
    fileConfig.run {
        if (option == null) {
            if (readText().isEmpty()) println("Please, tell me who you are.")
            else println("The username is ${readText()}.")
        } else {
            println("The username is $option.")
            writeText(option)
        }
    }
}

fun add(fileAdd: File, option: String?) {
    fileAdd.run {
        if (option == null) {
            if (readText().isEmpty()) println("Add a file to the index.")
            else println("Tracked files:\n${readText()}")
        } else {
            if (!File(option).exists()) println("Can't find '$option'.")
            else {
                println("The file '$option' is tracked.")
                readText().let { writeText(if (it.isEmpty()) option else it + "\n$option") }
            }
        }
    }
}

fun commit(fileLog: File, fileConfig: File, indexFile: File, option: String?) {
    if (option == null) {
        println("Message was not passed.")
    } else if (buildID(indexFile) == lastCommitID(fileLog)) {
        println("Nothing to commit.")
    } else {
        val commitID = buildID(indexFile)
        fileLog.readText().run {
            fileLog.writeText("${commitInfo(fileConfig, commitID, option)}\n$this")
        }
        println("Changes are committed.")
        val commitDir = File("vcs/commits/$commitID")
        commitDir.mkdir()
        indexFile.readLines().forEach {
            File(it).copyTo(File("vcs/commits/$commitID/$it"))
        }
    }
}

fun commendHelp() = println(
    """
        These are SVCS commands:
        config     $CONFIG_HELP
        add        $ADD_HELP
        log        $LOG_HELP
        commit     $COMMIT_HELP
        checkout   $CHECKOUT_HELP
    """.trimIndent()
)

fun lastCommitID(logFile: File): String {
    logFile.readLines().run {
        return if (this.isEmpty()) " " else this.first().substringAfter(' ')
    }
}

fun inputOption(args: Array<String>) = if (args.size == 2) args[1] else null

fun commitInfo(fileConfig: File, hashID: String, option: String) = """
            commit $hashID
            Author: ${fileConfig.readText()}
            $option
            
        """.trimIndent()

fun buildID(indexFile: File) = md5(indexFile.readLines().joinToString { File(it).readText() })


fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
}

fun checkout(fileLog: File, fileConfig: File, indexFile: File, option: String?) {
    if (option == null)
        println("Commit id was not passed.")
    else {
        val coms = fileLog.readText()
        if (coms.contains(option)) {
            println("Switched to commit $option.")
            indexFile.readLines().forEach {
                File("vcs/commits/$option/$it").copyTo(File(it), overwrite = true)
            }
        } else println("Commit does not exist.")
    }
}