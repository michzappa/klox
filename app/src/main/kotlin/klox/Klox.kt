package klox

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Klox {
    private val stdlib = javaClass.classLoader.getResource("stdlib.lox")!!.readText()

    fun runFile(path: String) {
        val bytes = Files.readAllBytes(Paths.get(path))

        run(stdlib)
        if (hadError) {
            System.err.println("Error loading standard library.")
            exitProcess(65)
        }

        run(String(bytes, Charset.defaultCharset()))

        if (hadError) exitProcess(65)
        if (hadRuntimeError) exitProcess(70)
    }

    fun runPrompt() {
        val input = InputStreamReader(System.`in`)
        val reader = BufferedReader(input)

        while (true) {
            run(stdlib)
            if (hadError) {
                System.err.println("Error loading standard library.")
                exitProcess(65)
            }

            print("> ")
            val line = reader.readLine()
            if (line.isNullOrEmpty()) break
            try {
                run(line, true)
            } catch (e: RuntimeError) {
                error(e.token, e.message)
            }
            hadError = false
        }
    }

    fun run(source: String, repl: Boolean = false) {
        val scanner = Scanner(source)
        val tokens = scanner.scanTokens()

        // allow expressions in the repl
        if (repl) {
            try {
                val expr = Parser(tokens, false).parseExpression()

                println(interpreter.evaluate(expr))
                return
            } catch (e: Parser.ParseError) {
                // no nothing, try to parse as a statement
            }
        }

        val parser = Parser(tokens)
        val statements = parser.parse()

        // stop if there was a syntax error
        if (hadError) return

        val resolver = Resolver(interpreter)
        resolver.resolve(statements)

        // stop if there was a resolution error
        if (hadError) return

        interpreter.interpret(statements)
    }

    companion object {
        val interpreter = Interpreter()
        var hadError = false
        var hadRuntimeError = false

        fun error(line: Int, message: String) {
            report(line, "", message)
        }

        fun error(token: Token, message: String) {
            if (token.type === TokenType.EOF) {
                report(token.line, " at end", message)
            } else {
                report(token.line, " at '" + token.lexeme + "'", message)
            }
        }

        private fun report(line: Int, where: String, message: String) {
            println("[line $line] Error$where: $message")
        }

        fun runtimeError(error: RuntimeError) {
            System.err.println("${error.message}\n[line ${error.token.line}]")
            hadRuntimeError = true
        }
    }
}

fun main(args: Array<String>) {
    val klox = Klox()

    if (args.size > 1) {
        println("Usage: klox [script]")
        // exit codes in this interpreter are from the UNIX
        // <sysexits.h> header file
        exitProcess(64)
    } else if (args.size == 1) {
        klox.runFile(args[0])
    } else {
        klox.runPrompt()
    }
}
