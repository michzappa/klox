package klox

// put every variable declaration in the statement's tree into a list and remove them from the tree,
// return the list and the tree
class Hoister : Stmt.Visitor<Pair<Stmt, List<Stmt.Var>>> {

    fun hoist(stmts: List<Stmt>): Pair<List<Stmt>, List<Stmt.Var>>{
        val mapped = stmts.map { s -> hoist(s) }
        val foo: Pair<List<Stmt>, List<Stmt.Var>> = mapped.fold(Pair(listOf(), listOf())) { acc, p ->
            Pair(acc.first + p.first, acc.second + p.second)
        }

        return Pair(foo.first, foo.second)
    }

    private fun hoist(stmt: Stmt): Pair<Stmt, List<Stmt.Var>> {
        return stmt.accept(this)
    }

    override fun visitBlockStmt(stmt: Stmt.Block): Pair<Stmt, List<Stmt.Var>> {
        val hoists = hoist(stmt.statements)

        return Pair(Stmt.Block(hoists.first), hoists.second)
    }

    override fun visitBreakStmt(stmt: Stmt.Break): Pair<Stmt, List<Stmt.Var>> {
        return Pair(stmt, listOf())
    }

    override fun visitClassStmt(stmt: Stmt.Class): Pair<Stmt, List<Stmt.Var>> {
        TODO("Not yet implemented")
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression): Pair<Stmt, List<Stmt.Var>> {
        return Pair(stmt, listOf())
    }

    override fun visitFunctionStmt(stmt: Stmt.Function): Pair<Stmt, List<Stmt.Var>> {
        val hoists = hoist(stmt.body)

        return Pair(Stmt.Function(stmt.name, stmt.params, hoists.first), hoists.second)
    }

    override fun visitIfStmt(stmt: Stmt.If): Pair<Stmt, List<Stmt.Var>> {
        val (thenSanitized, thenVars) = hoist(stmt.thenBranch)
        val (elseSanitized, elseVars) = hoist(stmt.elseBranch)

        return Pair(Stmt.If(stmt.condition, thenSanitized, elseSanitized), thenVars + elseVars)
    }

    override fun visitInvalidStmt(stmt: Stmt.Invalid): Pair<Stmt, List<Stmt.Var>> {
        return Pair(stmt, listOf())
    }

    override fun visitPrintStmt(stmt: Stmt.Print): Pair<Stmt, List<Stmt.Var>> {
        return Pair(stmt, listOf())
    }

    override fun visitReturnStmt(stmt: Stmt.Return): Pair<Stmt, List<Stmt.Var>> {
        return Pair(stmt, listOf())
    }

    override fun visitVarStmt(stmt: Stmt.Var): Pair<Stmt, List<Stmt.Var>> {
        return Pair(Stmt.Block(listOf()), listOf(stmt))
    }

    override fun visitWhileStmt(stmt: Stmt.While): Pair<Stmt, List<Stmt.Var>> {
        val foo = hoist(stmt.body)
        return Pair(Stmt.While(stmt.condition, foo.first), foo.second)
    }
}