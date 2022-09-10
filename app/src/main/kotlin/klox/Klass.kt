package klox

class Klass(val name: String, private val superclass: Klass?, private val methods: Map<String, Function>, metaclass: Klass?) :
    Callable, Instance(metaclass) {
    fun findMethod(name: String): Function? {
        return if (methods.containsKey(name)) {
            methods[name]
        } else superclass?.findMethod(name)
    }

    override fun arity(): Int {
        val initializer = findMethod("init")
        return initializer?.arity() ?: 0
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>, token: Token): Any {
        val instance = Instance(this)
        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, arguments, initializer.token)
        return instance
    }

    override fun toString(): String {
        return name
    }
}
