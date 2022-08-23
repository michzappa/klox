package klox.tool

import klox.Expr
import klox.Token
import klox.TokenType

// This class may be unmaintained and not up-to-date with the language
class AstPrinter : Expr.Visitor<String?> {
    fun print(expr: Expr): String? {
        return expr.accept(this)
    }

    override fun visitBinaryExpr(expr: Expr.Binary): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        return parenthesize("group", expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String {
        if (expr.value == null) {
            return "nil"
        }
        return expr.value.toString()
    }

    override fun visitLogicalExpr(expr: Expr.Logical): String? {
        TODO("Not yet implemented")
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable): String {
        return "(var  ${expr.name.lexeme})"
    }

    override fun visitAssignExpr(expr: Expr.Assign): String {
        return parenthesize("set ${expr.name.lexeme}", expr.value)
    }

    override fun visitCommaExpr(expr: Expr.Comma): String {
        return "(${print(expr.left)}, ${print(expr.right)})"
    }

    override fun visitConditionalExpr(expr: Expr.Conditional): String {
        return "(if ${print(expr.cond)} then ${print(expr.left)} else ${print(expr.right)})"
    }

    override fun visitInvalidExpr(expr: Expr.Invalid): String {
        return "(invalid expression)"
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ")
            builder.append(expr.accept(this))
        }
        builder.append(")")
        return builder.toString()
    }
}

fun main() {
    val expression: Expr = Expr.Comma(
        Expr.Binary(
            Expr.Unary(Token(TokenType.MINUS, "-", null, 1), Expr.Literal(123)),
            Token(TokenType.STAR, "*", null, 1),
            Expr.Grouping(Expr.Literal(45.67))
        ),
        Expr.Comma(
            Expr.Assign(Token(TokenType.IDENTIFIER, "hello", null, 1), Expr.Literal("hello")),
            Expr.Conditional(Expr.Literal(true), Expr.Literal(4), Expr.Literal(1))
        )
    )
    println(AstPrinter().print(expression))
}
