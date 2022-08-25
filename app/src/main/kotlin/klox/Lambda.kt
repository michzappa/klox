package klox

class Lambda(private val closure: Environment, private val declaration: Expr.Lambda) : Callable {
    override fun arity(): Int {
        return declaration.params.size
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for (i in 0 until arity()) {
            environment.define(declaration.params[i].lexeme, arguments[i], true)
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            return returnValue.value
        }

        return null
    }

    override fun toString(): String {
        return "<lambda>"
    }
}
