package klox

class Environment(val enclosing: Environment?) {
    constructor() : this(null)

    // name, (value, assigned)
    private val values = mutableMapOf<String, Pair<Any?, Boolean>>()

    fun ancestor(distance: Int): Environment? {
        var environment: Environment? = this
        for (i in 0 until distance) {
            environment = environment?.enclosing
        }
        return environment
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = Pair(value, true)
        } else if (enclosing != null) {
            enclosing.assign(name, value)
        } else {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
    }

    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance)?.values?.put(name.lexeme, Pair(value, true))
    }

    fun define(name: String, value: Any?, assign: Boolean) {
        values[name] = Pair(value, assign)
    }

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

    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance)?.values?.get(name)?.first
    }
}
