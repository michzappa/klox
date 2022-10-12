package klox

import klox.TokenType.*

// unidiomatic scheme back-end
class Compiler : Expr.Visitor<String>, Stmt.Visitor<String> {
    fun compile(statements: List<Stmt>): String {
        var scheme = ""
        for(stmt in statements){
            scheme += "${emit(stmt)}\n"
        }
        return scheme
    }

    private fun emit(expr: Expr): String {
        return expr.accept(this)
    }

    private fun emit(stmt: Stmt): String {
        return stmt.accept(this)
    }

    override fun visitAssignExpr(expr: Expr.Assign): String {
        return "(set! ${expr.name.lexeme} ${emit(expr.value)})"
    }

    override fun visitBinaryExpr(expr: Expr.Binary): String {
        val left = emit(expr.left)
        val right = emit(expr.right)

        return when (expr.operator.type) {
            GREATER -> {
                "(> $left $right)"
            }
            GREATER_EQUAL -> {
                "(>= $left $right)"
            }
            LESS -> {
                "(< $left $right)"
            }
            LESS_EQUAL -> {
                "(<= $left $right)"
            }
            BANG_EQUAL -> {
                "(not (= $left $right))"
            }
            EQUAL_EQUAL -> {
                "(= $left $right)"
            }
            MINUS -> {
                "(- $left $right)"
            }
            PLUS -> {
                "(+ $left $right)"
            }
            PLUS_PLUS -> {
                "(string-append $left $right)"
            }
            SLASH -> {
                "(/ $left $right)"
            }
            STAR -> {
                "(* $left $right)"
            }
            PERCENT -> {
                "(modulo $left $right)"
            }
            else -> throw RuntimeException("OPERATOR-NOT-FOUND")
        }
    }

    override fun visitCallExpr(expr: Expr.Call): String {
        val schemeCallee = when(val callee = emit(expr.callee)) {
            "empty" -> "null?"
            "first" -> "car"
            "rest" -> "cdr"
            else -> callee
        }

        return "($schemeCallee${if (expr.arguments.isEmpty()) "" else " "}${expr.arguments.joinToString(" ") { e -> emit(e) }})"
    }

    override fun visitCommaExpr(expr: Expr.Comma): String {
        return "(begin ${emit(expr.left)} ${emit(expr.right)})"
    }

    override fun visitConditionalExpr(expr: Expr.Conditional): String {
        return "(if ${emit(expr.cond)} ${emit(expr.left)} ${emit(expr.right)})"
    }

    override fun visitGetExpr(expr: Expr.Get): String {
        TODO("Not yet implemented")
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        return emit(expr.expression)
    }

    override fun visitInvalidExpr(expr: Expr.Invalid): String {
        throw RuntimeException("Don't compile an invalid expression.")
    }

    override fun visitKloxListExpr(expr: Expr.KloxList): String {
        return "(list${if (expr.values.isEmpty()) "" else " "}${expr.values.joinToString(" ") { e -> emit(e) }})"
    }

    override fun visitLambdaExpr(expr: Expr.Lambda): String {
        return "(lambda (${expr.params.joinToString(" ") { e -> e.lexeme }}) ${expr.body.joinToString(" ") { e -> emit(e) }})"
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String {
        return when (expr.token.lexeme){
            "true" -> "#t"
            "false" -> "#f"
            "nil" -> "'()"
            else -> expr.token.lexeme
        }
    }

    override fun visitLogicalExpr(expr: Expr.Logical): String {
        return "(${expr.operator.lexeme} ${emit(expr.left)} ${emit(expr.right)})"
    }

    override fun visitSetExpr(expr: Expr.Set): String {
        TODO("Not yet implemented")
    }

    override fun visitSuperExpr(expr: Expr.Super): String {
        TODO("Not yet implemented")
    }

    override fun visitThisExpr(expr: Expr.This): String {
        TODO("Not yet implemented")
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        return "(${expr.operator.lexeme} ${emit(expr.right)})"
    }

    override fun visitVariableExpr(expr: Expr.Variable): String {
        return expr.name.lexeme
    }

    override fun visitBlockStmt(stmt: Stmt.Block): String {
        return stmt.statements.joinToString(" ") { e -> emit(e) }
    }

    override fun visitBreakStmt(stmt: Stmt.Break): String {
        return "(return)"
    }

    override fun visitClassStmt(stmt: Stmt.Class): String {
        TODO("Not yet implemented")
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression): String {
        return emit(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function): String {
      return "(define (${stmt.name.lexeme}${if (stmt.params.isEmpty()) "" else " "}${stmt.params.joinToString(" ") { e -> e.lexeme }}) " +
              "${stmt.body.joinToString(" ") { s -> emit(s) }})"
    }

    override fun visitIfStmt(stmt: Stmt.If): String {
        return "(if ${emit(stmt.condition)} ${emit(stmt.thenBranch)} " +
                "${if (stmt.elseBranch !is Stmt.Invalid) {
                    emit(stmt.elseBranch)
                } else {
                    "'()"
                }
                })"
    }

    override fun visitInvalidStmt(stmt: Stmt.Invalid): String {
        throw RuntimeException("Don't evaluate an invalid expression.")
    }

    override fun visitPrintStmt(stmt: Stmt.Print): String {
        return "(begin (display ${emit(stmt.expression)}) (newline))"
    }

    override fun visitReturnStmt(stmt: Stmt.Return): String {
        return emit(stmt.value)
    }

    override fun visitVarStmt(stmt: Stmt.Var): String {
        return "(define ${stmt.name.lexeme} ${emit(stmt.initializer)})"
    }

    override fun visitWhileStmt(stmt: Stmt.While): String {
        val recur = "(lambda () (if ${emit(stmt.condition)} (begin ${emit(stmt.body)} (recur)) (void)))"
        return "(call-with-current-continuation (lambda (return) (letrec ((recur ${recur})) (recur))))"
    }
}
