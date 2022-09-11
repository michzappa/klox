package klox

class Function(private val closure: Environment, private val declaration: Stmt.Function, private val isInitializer: Boolean, val token: Token, val isGetter: Boolean) :
    Callable {
    fun bind(instance: Instance): Function {
        val environment = Environment(closure)
        environment.define("this", instance, true)
        return Function(environment, declaration, isInitializer, token, isGetter)
    }

    override fun arity(): Int {
        return declaration.params.size
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>, token: Token): Any? {
        val environment = Environment(closure)
        if (!isGetter) {
            for (i in 0 until arity()) {
                environment.define(declaration.params[i].lexeme, arguments[i], true)
            }
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            if (isInitializer) return closure.getAt(0, "this")
            return returnValue.value
        }

        if (isInitializer) return closure.getAt(0, "this")
        return null
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }
}
