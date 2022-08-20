package klox

import klox.TokenType.*

class Interpreter : Expr.Visitor<Any?> {
    fun interpret(expression: Expr?) {
        try {
            val value = evaluate(expression!!)
            println(stringify(value))
        } catch (error: RuntimeError) {
            Klox.runtimeError(error)
        }
    }
    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    private fun isTruthy(obj: Any?): Boolean {
        return when (obj) {
            null -> false
            is Boolean -> obj
            else -> true
        }
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        return (if (a == null && b == null)  true
        else  if (a == null) false
        else a == b)
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
                if (left is Double && right is Double){
                    left + right
                } else if (left is String && right is String){
                    left + right
                }
//                This ain't javascript
//                else if (left is String && right is Double) {
//                    left + stringify(right)
//                } else if  (left is Double && right is String) {
//                    stringify(left) + right
//                }

                else {
                  throw RuntimeError(expr.operator, "Operands must be two numbers or two strings, this ain't javascript.")
                }
            }
            SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                if (right == 0.0) {
                    throw RuntimeError(expr.operator, "Cannot divide by zero.")
                }else {
                    (left as Double) / (right as Double)
                }
            }
            STAR -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) * (right as Double)
            }
            else -> null
        }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)

        return (when (expr.operator.type) {
                    BANG -> !isTruthy(right)
                    MINUS -> {
                        checkNumberOperand(expr.operator, right)
                        -(right as Double)
                    }
                    // unreachable
                    else -> null
        })
    }

    override fun visitCommaExpr(expr: Expr.Comma): Any? {
        // evaluate the left, and throw it away
        evaluate(expr.left)

        // return the right
        return evaluate(expr.right)
    }

    override fun visitTernaryExpr(expr: Expr.Ternary): Any? {
        return if (isTruthy(evaluate(expr.cond))){
            evaluate(expr.left)
        } else{
            evaluate(expr.right)
        }
    }

    override fun visitInvalidExpr(expr: Expr.Invalid): Any? {
        throw RuntimeException("Don't evaluate an invalid expression.")
    }
}
