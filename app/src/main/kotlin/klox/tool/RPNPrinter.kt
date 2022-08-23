package klox.tool

import klox.Expr
import klox.Token
import klox.TokenType

// This class may be unmaintained and not up-to-date with the language
class RPNPrinter : Expr.Visitor<String?> {
    fun print(expr: Expr): String? {
        return expr.accept(this)
    }

    override fun visitBinaryExpr(expr: Expr.Binary): String {
        return "${print(expr.left)} ${print(expr.right)} ${expr.operator.lexeme}"
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        return "${print(expr.expression)} group"
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
        return "${print(expr.right)} ${expr.operator.lexeme}"
    }

    override fun visitVariableExpr(expr: Expr.Variable): String? {
        TODO("Not yet implemented")
    }

    override fun visitAssignExpr(expr: Expr.Assign): String? {
        TODO("Not yet implemented")
    }

    override fun visitCommaExpr(expr: Expr.Comma): String? {
        TODO("Not yet implemented")
    }

    override fun visitConditionalExpr(expr: Expr.Conditional): String? {
        TODO("Not yet implemented")
    }

    override fun visitInvalidExpr(expr: Expr.Invalid): String? {
        TODO("Not yet implemented")
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
    println(RPNPrinter().print(expression))
}
