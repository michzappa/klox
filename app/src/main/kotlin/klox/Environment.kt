package klox

class Environment(private val enclosing: Environment?) {
    constructor() : this(null)

    // name, (value, assigned)
    private val values = mutableMapOf<String, Pair<Any?, Boolean>>()

    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            if (values[name.lexeme]!!.second) {
                return values[name.lexeme]!!.first
            } else {
                throw RuntimeError(name, "Unassigned variable '${name.lexeme}'.")
            }
        } else {
            if (enclosing != null) return enclosing.get(name)
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = Pair(value, true)
        } else if (enclosing != null) {
            enclosing.assign(name, value)
        } else {
            throw RuntimeError(name, "Undefined variable '" + name.lexeme + "'.")
        }
    }

    fun define(name: String, value: Any?, assign: Boolean = false) {
        values[name] = Pair(value, assign)
    }
}