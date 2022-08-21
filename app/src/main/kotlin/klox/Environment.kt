package klox

class Environment(var enclosing: Environment? = null) {
    // name, (value, assigned)
    private val values = mutableMapOf<String, Pair<Any?, Boolean>>()

    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            if(values[name.lexeme]!!.second) {
                return values[name.lexeme]!!.first
            }else{
                throw RuntimeError(name, "Unassigned variable '${name.lexeme}'.")
            }
        } else {
            if (enclosing != null) return enclosing!!.get(name)
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = Pair(value, true)
            return
        } else if (enclosing != null) {
            enclosing!!.assign(name, value)
        }

        throw RuntimeError(name, "Undefined variable '" + name.lexeme + "'.")
    }

    fun define(name: String, value: Any?, assign: Boolean = false) {
        values[name] = Pair(value, assign)
    }
}