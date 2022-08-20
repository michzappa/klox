package tool

import java.io.PrintWriter
import java.util.*
import kotlin.system.exitProcess

fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path, "UTF-8")
    writer.println("package klox")
    writer.println()
    writer.println("abstract class $baseName {")
    writer.println("  abstract fun <R> accept(visitor: Visitor<R>): R")
    defineVisitor(writer, baseName, types)
    for (type in types) {
        val info = type.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        println(info)
        val className =
            info[0].trim { it <= ' ' }
        val fields = if (info.size > 1) (info[1].trim { it <= ' ' }) else ""
        defineType(writer, baseName, className, fields)
    }
    writer.println("}")
    writer.close()
}

fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
    fun makeInitializer(fields: Array<String>): String {
        return if (fields.isEmpty()) {
            ""
        } else {
            val str = StringBuilder()
            str.append("(")
            for (field: String in fields) {
                val name = field.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                val type = field.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                str.append("val $name: $type, ")
            }
            str.deleteRange(str.length - 2, str.length)
            str.append(")")
            str.toString()
        }
    }

    // break up the input
    val fields = fieldList.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    writer.println("  class $className${makeInitializer(fields)} : $baseName() {")
    // visitor pattern
    writer.println("    override fun <R> accept(visitor: Visitor<R> ): R {")
    writer.println("      return visitor.visit$className$baseName(this);")
    writer.println("    }")
    writer.println("  }")
}

fun defineVisitor(writer: PrintWriter, baseName: String, types: List<String>) {
    writer.println("  interface Visitor<R> {")
    for (type in types) {
        val typeName = type.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].trim { it <= ' ' }
        writer.println("    fun visit$typeName$baseName(${baseName.lowercase(Locale.getDefault())}: $typeName): R")
    }
    writer.println("  }")
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output directory>")
        exitProcess(64)
    }
    val outputDir = args[0]
    defineAst(
        outputDir,
        "Expr",
        listOf(
            "Binary   : Expr left, Token operator, Expr right",
            "Grouping : Expr expression",
            "Literal  : Any? value",
            "Unary    : Token operator, Expr right",
            "Comma    : Expr left, Expr right",
            "Ternary  : Expr cond, Expr left, Expr right",
            "Invalid  :"
        )
    )
}
