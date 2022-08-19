package klox

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

class Klox {
    var hadError = false

    fun runFile(path: String) {
        val bytes = Files.readAllBytes(Paths.get(path))
        run(String(bytes, Charset.defaultCharset()))

        if (hadError) {
            System.exit(65)
        }
    }

    fun runPrompt() {
        val input = InputStreamReader(System.`in`)
        val reader = BufferedReader(input)

        while (true) {
            print("> ")
            val line = reader.readLine()
            if (line.isNullOrEmpty()) {
                break; }
            run(line)
            hadError = false
        }
    }

    fun run(source: String) {
        val scanner = Scanner(source)
        val tokens = scanner.scanTokens()

        tokens.forEach { token ->
            println(token)
        }
    }

    companion object {
        fun error(line: Int, message: String) {
            report(line, "", message)
        }

        fun report(line: Int, where: String, message: String) {
            println("[line $line] Error$where: $message")
        }
    }
}

fun main(args: Array<String>) {
    val klox = Klox()

    if (args.size > 1) {
        println("Usage: klox [script]")
        // exit codes in this interpreter are from the UNIX
        // <sysexits.h> header file
        System.exit(64)
    } else if (args.size == 1) {
        klox.runFile(args[0])
    } else {
        klox.runPrompt()
    }
}
