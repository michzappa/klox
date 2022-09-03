package klox

import java.util.*

class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private enum class FunctionType {
        NONE, FUNCTION
    }

    private class ResolverInfo(var defined: Boolean, var used: Boolean) {
        fun define() {
            defined = true
        }

        fun use() {
            used = true
        }
    }

    // name, <defined, used>
    private val scopes = Stack<MutableMap<String, ResolverInfo>>()
    private var currentFunction: FunctionType = FunctionType.NONE

    fun resolve(statements: List<Stmt>) {
        for (statement in statements) {
            resolve(statement)
        }
        for (scope: MutableMap<String, ResolverInfo> in scopes) {
            for (entry: Map.Entry<String, ResolverInfo> in scope) {
                if (!entry.value.used) {
                    Klox.hadError = true
                    System.err.println("Error: ${entry.key} is unused.")
                }
            }
        }
    }

    private fun beginScope() {
        scopes.push(mutableMapOf())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.size - 1 downTo 0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()

        currentFunction = enclosingFunction
    }

    private fun resolveFunction(function: Expr.Lambda, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()

        currentFunction = enclosingFunction
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return
        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme)) {
            Klox.error(name, "Already a variable with this name in this scope.")
        }
        scope[name.lexeme] = ResolverInfo(defined = false, used = false)
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        val scope = scopes.peek()
        scope[name.lexeme]?.define()
    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)

        for (argument in expr.arguments) {
            resolve(argument)
        }
    }

    override fun visitCommaExpr(expr: Expr.Comma) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitConditionalExpr(expr: Expr.Conditional) {
        resolve(expr.cond)
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitInvalidExpr(expr: Expr.Invalid) {
        throw RuntimeException("Don't resolve an invalid expression.")
    }

    override fun visitLambdaExpr(expr: Expr.Lambda) {
        resolveFunction(expr, FunctionType.FUNCTION)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {}

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (!scopes.isEmpty() && (scopes.peek()[expr.name.lexeme] != null) && !scopes.peek()[expr.name.lexeme]!!.defined) {
            Klox.error(expr.name, "Can't read local variable in its own initializer.")
        }

        resolveLocal(expr, expr.name)

        // mark the variable as used
        for (scope: MutableMap<String, ResolverInfo> in scopes) {
            for (entry: Map.Entry<String, ResolverInfo> in scope) {
                scope[entry.key]?.use()
            }
        }
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {}

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        resolve(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch !is Stmt.Invalid) resolve(stmt.elseBranch)
    }

    override fun visitInvalidStmt(stmt: Stmt.Invalid) {
        throw RuntimeException("Don't resolve an invalid statement.")
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) {
            Klox.error(stmt.keyword, "Can't return from top-level code.")
        }

        if (stmt.value !is Expr.Invalid) {
            resolve(stmt.value)
        }
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer !is Expr.Invalid) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }
}