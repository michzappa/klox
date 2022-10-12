package klox

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Klox {
    private val stdlib = javaClass.classLoader.getResource("stdlib.lox")!!.readText()

    fun compileFile(path: String): String {
        val compiledStdlib = compile(stdlib)
        val bytes = Files.readAllBytes(Paths.get(path))
        return compiledStdlib + compile(String(bytes, Charset.defaultCharset()))
    }

    fun compile(source: String): String {
        val scanner = Scanner(source)
        val tokens = scanner.scanTokens()
        val statements = Parser(tokens).parse()

        // stop if there was a syntax error
        if (hadError) return "Parsing Error"

        val resolvedStatements = resolver.resolve(statements)

        // stop if there was a resolution error
        if (hadError) return "Resolution Error"

        // hoist up declarations
        val hoisted = hoister.hoist(resolvedStatements)
        val hoistedStatements = hoisted.second + hoisted.first

        return compiler.compile(hoistedStatements)
    }

    private fun loadStdLib() {
        run(stdlib)
        if (hadError) {
            System.err.println("Error loading standard library.")
            exitProcess(65)
        }
    }

    fun resolve(ast: List<Stmt>): List<Stmt> {
        return resolver.resolve(ast)
    }

    fun runFile(path: String) {
        loadStdLib()

        val bytes = Files.readAllBytes(Paths.get(path))
        run(String(bytes, Charset.defaultCharset()))

        if (hadError) exitProcess(65)
        else if (hadRuntimeError) exitProcess(70)
    }

    fun runPrompt() {
        val input = InputStreamReader(System.`in`)
        val reader = BufferedReader(input)

        while (true) {
            loadStdLib()

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

        val statements = Parser(tokens).parse()

        // stop if there was a syntax error
        if (hadError) return

        val resolvedStatements = resolver.resolve(statements)

        // stop if there was a resolution error
        if (hadError) return

        interpreter.interpret(resolvedStatements)
    }

    companion object {
        val interpreter = Interpreter()
        val compiler = Compiler()
        val resolver = Resolver()
        val hoister = Hoister()

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

    if (args.size > 2 || (args.size > 1 && args[0] != "compile")) {
        println("Usage: klox [[compile] script]")
        // exit codes in this interpreter are from the UNIX
        // <sysexits.h> header file
        exitProcess(64)
    } else if (args.size == 1) {
        klox.runFile(args[0])
    } else if (args.size == 2) {
        val inputFile = args[1]
        File("${inputFile}.scm").writeText(klox.compileFile(inputFile))
    } else {
        klox.runPrompt()
    }
}
