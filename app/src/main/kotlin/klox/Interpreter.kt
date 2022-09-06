package klox

import klox.TokenType.*

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    private val globals = Environment()
    private var environment = globals
    private val locals: MutableMap<Expr, Int> = mutableMapOf()

    init {
        globals.define(
            "clock",
            object : Callable {
                override fun arity(): Int {
                    return 0
                }

                override fun call(interpreter: Interpreter, arguments: List<Any?>, token: Token): Any {
                    return System.currentTimeMillis() / 1000.0
                }

                override fun toString(): String {
                    return "<native fn>"
                }
            },
            true
        )
        globals.define(
            "cons",
            object : Callable {
                override fun arity(): Int {
                    return 2
                }

                override fun call(interpreter: Interpreter, arguments: List<Any?>, token: Token): Any {
                    if (arguments[1] is List<Any?>) {
                        val l: MutableList<Any?> = mutableListOf(arguments[0])
                        l.addAll(arguments[1] as List<Any?>)
                        return l
                    } else {
                        throw RuntimeError(token, "probably a type error")
                    }
                }

                override fun toString(): String {
                    return "<native fn>"
                }
            },
            true
        )
        globals.define(
            "length",
            object : Callable {
                override fun arity(): Int {
                    return 1
                }

                override fun call(interpreter: Interpreter, arguments: List<Any?>, token: Token): Any {
                    if (arguments[0] is List<Any?>) {
                        return (arguments[0] as List<Any?>).size
                    } else {
                        throw RuntimeError(token, "probably a type error")
                    }
                }

                override fun toString(): String {
                    return "<native fn>"
                }
            },
            true
        )
        globals.define(
            "head",
            object : Callable {
                override fun arity(): Int {
                    return 1
                }

                override fun call(interpreter: Interpreter, arguments: List<Any?>, token: Token): Any? {
                    if (arguments[0] is List<Any?>) {
                        val l = arguments[0]
                        return if ((l as List<Any?>).size == 0) { null } else { l.first() }
                    } else {
                        throw RuntimeError(token, "probably a type error")
                    }
                }

                override fun toString(): String {
                    return "<native fn>"
                }
            },
            true
        )
        globals.define(
            "tail",
            object : Callable {
                override fun arity(): Int {
                    return 1
                }

                override fun call(interpreter: Interpreter, arguments: List<Any?>, token: Token): Any? {
                    if (arguments[0] is List<Any?>) {
                        return (arguments[0] as List<Any?>).drop(1)
                    } else {
                        throw RuntimeError(token, "probably a type error")
                    }
                }

                override fun toString(): String {
                    return "<native fn>"
                }
            },
            true
        )
    }

    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                execute(statement)
            }
        } catch (error: RuntimeError) {
            Klox.runtimeError(error)
        }
    }

    fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    private fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    fun resolve(expr: Expr, depth: Int) {
        locals.put(expr, depth)
    }

    fun executeBlock(statements: List<Stmt>, environment: Environment?) {
        val previous = this.environment
        try {
            this.environment = environment!!
            for (statement in statements) {
                execute(statement)
            }
        } finally {
            this.environment = previous
        }
    }

    private fun isTruthy(obj: Any?): Boolean {
        return when (obj) {
            null -> false
            is Boolean -> obj
            else -> true
        }
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        return (
            if (a == null && b == null) true
            else if (a == null) false
            else a == b
            )
    }

    private fun stringify(obj: Any?): String {
        return when (obj) {
            null -> "nil"
            is Double -> {
                var text = obj.toString()
                if (text.endsWith(".0")) {
                    text = text.substring(0, text.length - 2)
                }
                text
            }
            else -> {
                obj.toString()
            }
        }
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operands must be numbers.")
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        return if (distance != null) {
            environment.getAt(distance, name.lexeme)
        } else {
            globals.get(name)
        }
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) > (right as Double)
            }
            GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) >= (right as Double)
            }
            LESS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) < (right as Double)
            }
            LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) <= (right as Double)
            }
            BANG_EQUAL -> !isEqual(left, right)
            EQUAL_EQUAL -> isEqual(left, right)
            MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) - (right as Double)
            }
            PLUS -> {
                if (left is Double && right is Double) {
                    left + right
                } else if (left is String && right is String) {
                    left + right
                }
                // This ain't javascript
                // else if (left is String && right is Double) {
                //     left + stringify(right)
                // } else if  (left is Double && right is String) {
                //     stringify(left) + right
                // }

                else {
                    throw RuntimeError(
                        expr.operator,
                        "Operands must be two numbers or two strings, this ain't javascript."
                    )
                }
            }
            SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                if (right == 0.0) {
                    throw RuntimeError(expr.operator, "Cannot divide by zero.")
                } else {
                    (left as Double) / (right as Double)
                }
            }
            STAR -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) * (right as Double)
            }
            PERCENT -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) % (right as Double)
            }
            else -> null
        }
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)

        val arguments: MutableList<Any?> = ArrayList()
        for (argument in expr.arguments) {
            arguments.add(evaluate(argument))
        }

        if (callee !is Callable) {
            throw RuntimeError(expr.paren, "Can only call functions and classes.")
        } else if (arguments.size != callee.arity()) {
            throw RuntimeError(expr.paren, "Expected ${callee.arity()} arguments but got ${arguments.size}.")
        }

        return callee.call(this, arguments, expr.token)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitKloxListExpr(expr: Expr.KloxList): Any? {
        val values: List<Any?> = expr.values.map { v -> evaluate(v) }
        return values
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)

        if (expr.operator.type == OR) {
            if (isTruthy(left)) return left
        } else {
            if (!isTruthy(left)) return left
        }

        return evaluate(expr.right)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)

        return (
            when (expr.operator.type) {
                BANG -> !isTruthy(right)
                MINUS -> {
                    checkNumberOperand(expr.operator, right)
                    -(right as Double)
                }
                // unreachable
                else -> null
            }
            )
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return lookUpVariable(expr.name, expr)
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)
        val distance = locals.get(expr)
        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }
        return value
    }

    override fun visitCommaExpr(expr: Expr.Comma): Any? {
        // evaluate the left, and throw it away
        evaluate(expr.left)

        // return the right
        return evaluate(expr.right)
    }

    override fun visitConditionalExpr(expr: Expr.Conditional): Any? {
        return if (isTruthy(evaluate(expr.cond))) {
            evaluate(expr.left)
        } else {
            evaluate(expr.right)
        }
    }

    override fun visitLambdaExpr(expr: Expr.Lambda): Lambda {
        return Lambda(environment, expr)
    }

    override fun visitInvalidExpr(expr: Expr.Invalid): Any? {
        throw RuntimeException("Don't evaluate an invalid expression.")
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
        return
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = Function(environment, stmt, stmt.name)
        environment.define(stmt.name.lexeme, function, true)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch !is Stmt.Invalid) {
            execute(stmt.elseBranch)
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
        return
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        val value = evaluate(stmt.value)
        throw Return(value)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        val assignment = stmt.initializer !is Expr.Invalid
        val value = if (assignment) {
            evaluate(stmt.initializer)
        } else {
            null
        }

        environment.define(stmt.name.lexeme, value, assignment)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        try {
            while (isTruthy(evaluate(stmt.condition))) {
                execute(stmt.body)
            }
        } catch (e: BreakException) {
            // swallow exception, jump out of the loop
        }
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    private class BreakException : RuntimeException()

    override fun visitBreakStmt(stmt: Stmt.Break) {
        throw BreakException()
    }

    override fun visitInvalidStmt(stmt: Stmt.Invalid) {
        throw RuntimeError(stmt.token, "Don't evaluate an invalid statement.")
    }
}
