package klox

import klox.TokenType.*

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    private val globals = Environment()
    private val locals: MutableMap<Expr, Int> = mutableMapOf()
    private var environment = globals

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

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)
        val distance = locals[expr]
        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }
        return value
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

    override fun visitGetExpr(expr: Expr.Get): Any? {
        val obj = evaluate(expr.obj)
        if (obj is Instance) {
            var result = obj.get(expr.name)
            if (result is Function && result.isGetter) {
                result = result.call(this, ArrayList(), expr.name)
            }

            return result
        }

        throw RuntimeError(expr.name, "Only instances have properties.")
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitInvalidExpr(expr: Expr.Invalid): Any? {
        throw RuntimeException("Don't evaluate an invalid expression.")
    }

    override fun visitKloxListExpr(expr: Expr.KloxList): Any {
        return expr.values.map { v -> evaluate(v) }
    }

    override fun visitLambdaExpr(expr: Expr.Lambda): Lambda {
        return Lambda(environment, expr)
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

    override fun visitSetExpr(expr: Expr.Set): Any? {
        val obj = evaluate(expr.obj)

        if (obj !is Instance) {
            throw RuntimeError(expr.name, "Only instances have fields.")
        } else {
            val value = evaluate(expr.value)
            obj.set(expr.name, value)
            return value
        }
    }

    override fun visitSuperExpr(expr: Expr.Super): Any {
        val distance = locals[expr]
        val superclass = environment.getAt(distance!!, "super") as Klass
        val obj = environment.getAt(distance - 1, "this") as Instance
        val method = superclass.findMethod(expr.method.lexeme)!!

        return method.bind(obj)
    }

    override fun visitThisExpr(expr: Expr.This): Any? {
        return lookUpVariable(expr.keyword, expr)
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

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    private class BreakException : RuntimeException()

    override fun visitBreakStmt(stmt: Stmt.Break) {
        throw BreakException()
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val superclass: Klass? = if (stmt.superclass != null) {
            val e = evaluate(stmt.superclass)
            if (e !is Klass) {
                throw RuntimeError(stmt.superclass.name, "Superclass must be a class.")
            } else {
                e
            }
        } else {
            null
        }
        environment.define(stmt.name.lexeme, null, false)

        if (stmt.superclass != null) {
            environment = Environment(environment)
            environment.define("super", superclass, true)
        }

        val staticMethods: MutableMap<String, Function> = HashMap()
        for (method in stmt.staticMethods) {
            staticMethods[method.name.lexeme] = Function(environment, method, false, method.name, false)
        }

        val metaclass = Klass("${stmt.name.lexeme} metaclass", null, staticMethods, null)

        val methods: MutableMap<String, Function> = HashMap()
        for (method in stmt.methods) {
            methods[method.name.lexeme] = Function(environment, method, method.name.lexeme == "init", method.name, method.isGetter)
        }

        val klass = Klass(stmt.name.lexeme, superclass, methods, metaclass)

        if (superclass != null) {
            environment = environment.enclosing!!
        }

        environment.assign(stmt.name, klass)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
        return
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = Function(environment, stmt, false, stmt.name, stmt.isGetter)
        environment.define(stmt.name.lexeme, function, true)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch !is Stmt.Invalid) {
            execute(stmt.elseBranch)
        }
    }

    override fun visitInvalidStmt(stmt: Stmt.Invalid) {
        throw RuntimeError(stmt.token, "Don't evaluate an invalid statement.")
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

    // utility methods
    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operands must be numbers.")
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

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        return if (distance != null) {
            environment.getAt(distance, name.lexeme)
        } else {
            globals.get(name)
        }
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
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

    // native functions
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
                    return if (arguments[1] is List<Any?>) {
                        val l: MutableList<Any?> = mutableListOf(arguments[0])
                        l.addAll(arguments[1] as List<Any?>)
                        l
                    } else if (arguments[1] is String) {
                        val s = arguments[1] as String
                        arguments[0].toString() + s
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
            "empty",
            object : Callable {
                override fun arity(): Int {
                    return 1
                }

                override fun call(interpreter: Interpreter, arguments: List<Any?>, token: Token): Any {
                    return if (arguments[0] is List<Any?>) {
                        (arguments[0] as List<Any?>).isEmpty()
                    } else if (arguments[0] is String) {
                        (arguments[0] as String).isEmpty()
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
            "first",
            object : Callable {
                override fun arity(): Int {
                    return 1
                }

                override fun call(interpreter: Interpreter, arguments: List<Any?>, token: Token): Any? {
                    return if (arguments[0] is List<Any?>) {
                        val l = (arguments[0] as List<Any?>)
                        if (l.isEmpty()) {
                            null
                        } else {
                            l.first()
                        }
                    } else if (arguments[0] is String) {
                        val s = (arguments[0] as String)
                        if (s.isEmpty()) {
                            null
                        } else {
                            s[0]
                        }
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
            "rest",
            object : Callable {
                override fun arity(): Int {
                    return 1
                }

                override fun call(interpreter: Interpreter, arguments: List<Any?>, token: Token): Any? {
                    return if (arguments[0] is List<Any?>) {
                        (arguments[0] as List<Any?>).drop(1)
                    } else if (arguments[0] is String) {
                        val s = (arguments[0] as String)
                        if (s.isEmpty()) {
                            null
                        } else {
                            s.substring(1)
                        }
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
}
