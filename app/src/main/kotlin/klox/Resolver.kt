package klox

// makes unambiguous variable bindings
class Resolver : Expr.Visitor<Expr>, Stmt.Visitor<Stmt> {
    private var currentScope = 0
    private var currentVarNum = 0
    private val scopes = mutableListOf<MutableMap<String, Int>>(mutableMapOf())

    private fun getVarNum(varName: String): Int? {
        for(i in currentScope downTo 0 ){
            if(scopes[i].containsKey(varName)){
                return scopes[i][varName]
            }
        }
        return null
    }

    fun resolve(statements: List<Stmt>): List<Stmt> {
        return statements.map { s -> resolve(s) }
    }

    private fun resolve(expr: Expr): Expr {
        return expr.accept(this)
    }

    private fun resolve(stmt: Stmt): Stmt {
        return stmt.accept(this)
    }

    override fun visitAssignExpr(expr: Expr.Assign): Expr {
        val varNum = getVarNum(expr.name.lexeme)

        return Expr.Assign(Token(expr.name.type, "var${varNum}", expr.name.literal, expr.name.line),
            resolve(expr.value),
            Token(expr.name.type, "var${varNum}", expr.name.literal, expr.name.line))
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Expr {
        return Expr.Binary(resolve(expr.left), expr.operator, resolve(expr.right))
    }

    override fun visitCallExpr(expr: Expr.Call): Expr {
        return Expr.Call(resolve(expr.callee), expr.token, expr.arguments.map { e -> resolve(e) }, expr.token)
    }

    override fun visitCommaExpr(expr: Expr.Comma): Expr {
        return Expr.Comma(resolve(expr.left), resolve(expr.right), expr.token)
    }

    override fun visitConditionalExpr(expr: Expr.Conditional): Expr {
        return Expr.Conditional(resolve(expr.cond), resolve(expr.left), resolve(expr.right), expr.token)
    }

    override fun visitGetExpr(expr: Expr.Get): Expr {
        return Expr.Get(resolve(expr.obj), expr.name)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Expr {
        return Expr.Grouping(resolve(expr.expression), expr.token)
    }

    override fun visitInvalidExpr(expr: Expr.Invalid): Expr {
        throw RuntimeException("Don't resolve an invalid expression.")
    }

    override fun visitKloxListExpr(expr: Expr.KloxList): Expr {
        return Expr.KloxList(expr.values.map {e -> resolve(e) }, expr.token)
    }


    override fun visitLambdaExpr(expr: Expr.Lambda): Expr {
        return Expr.Lambda(expr.params, expr.body.map { s -> resolve(s) }, expr.token)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Expr {
        return expr
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Expr {
        return Expr.Logical(resolve(expr.left), expr.operator, resolve(expr.right))
    }

    override fun visitSetExpr(expr: Expr.Set): Expr {
        TODO("Not yet implemented")
    }

    override fun visitSuperExpr(expr: Expr.Super): Expr {
        TODO("Not yet implemented")
    }

    override fun visitThisExpr(expr: Expr.This): Expr {
        return expr
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Expr {
        return Expr.Unary(expr.token, resolve(expr.right))
    }

    override fun visitVariableExpr(expr: Expr.Variable): Expr {
        val varNum = getVarNum(expr.name.lexeme) ?: return expr

        return Expr.Variable(Token(expr.token.type, "var${varNum}", expr.token.literal, expr.token.line))
    }

    override fun visitBlockStmt(stmt: Stmt.Block): Stmt {
        currentScope += 1
        scopes.add(mutableMapOf())
        val resolvedStmt = Stmt.Block(stmt.statements.map { s -> resolve(s) })
        currentScope -= 1
        scopes.removeAt(scopes.size-1)
        return resolvedStmt
    }

    override fun visitBreakStmt(stmt: Stmt.Break): Stmt {
        return stmt
    }

    override fun visitClassStmt(stmt: Stmt.Class): Stmt {
        return Stmt.Class(stmt.name, stmt.superclass, stmt.methods.map { s -> resolve(s) as Stmt.Function }, stmt.staticMethods.map { s-> resolve(s) as Stmt.Function} )
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression): Stmt {
        return Stmt.Expression(resolve(stmt.expression))
    }

    override fun visitFunctionStmt(stmt: Stmt.Function): Stmt {
        currentScope += 1
        scopes.add(mutableMapOf())
        val resolvedParams = stmt.params.map {t ->
            scopes[currentScope][t.lexeme] = currentVarNum
            currentVarNum += 1

            Token(t.type, "var${scopes[currentScope][t.lexeme]}", t.literal, t.line)
        }
        val resolvedFunction = Stmt.Function(stmt.name, resolvedParams, stmt.body.map { s -> resolve(s) }, stmt.isGetter)
        currentScope -= 1
        scopes.removeAt(scopes.size -1 )
        return resolvedFunction
    }

    override fun visitIfStmt(stmt: Stmt.If): Stmt {
        return Stmt.If(resolve(stmt.condition), resolve(stmt.thenBranch), resolve(stmt.elseBranch))
    }

    override fun visitInvalidStmt(stmt: Stmt.Invalid): Stmt {
        return stmt
    }

    override fun visitPrintStmt(stmt: Stmt.Print): Stmt {
        return Stmt.Print(resolve(stmt.expression))
    }

    override fun visitReturnStmt(stmt: Stmt.Return): Stmt {
        return Stmt.Return(stmt.keyword, resolve(stmt.value))
    }

    override fun visitVarStmt(stmt: Stmt.Var): Stmt {
        var varNum = getVarNum(stmt.name.lexeme)
        if(varNum == null){
            varNum = currentVarNum
            scopes[currentScope][stmt.name.lexeme] = varNum
            currentVarNum += 1
        } else {
            Klox.hadError = true
            System.err.println("Cannot redeclare variable ${stmt.name.lexeme} on line ${stmt.name.line}")
        }

        return Stmt.Var(Token(stmt.name.type, "var${varNum}", stmt.name.literal, stmt.name.line), resolve(stmt.initializer))
    }

    override fun visitWhileStmt(stmt: Stmt.While): Stmt {
    currentScope += 1
    scopes.add(mutableMapOf())
    val resolvedStmt = Stmt.While(resolve(stmt.condition), resolve(stmt.body))
    currentScope -= 1
    scopes.removeAt(scopes.size-1)
    return resolvedStmt
    }
}
