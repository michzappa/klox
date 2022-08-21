package klox

import klox.TokenType.*

class Parser(private val tokens: List<Token>) {
    private class ParseError : RuntimeException()

    private var current: Int = 0

    fun parse(): List<Stmt> {
        val statements = ArrayList<Stmt>()
        while (!isAtEnd()){
            statements.add(parseDeclaration())
        }

        return statements
    }

    fun parseExpression(): Expr {
        return parseComma()
    }

    private fun parseDeclaration(): Stmt {
        return try {
            if (match(VAR)) parseVarDeclaration()
            else parseStatement()
        } catch (error : ParseError){
            synchronize()
            Stmt.Invalid()
        }
    }

    private fun parseStatement(): Stmt {
        return if (match(PRINT)) parsePrintStatement()
        else if (match(LEFT_BRACE)) return Stmt.Block(parseBlock())
        else parseExpressionStatement()
    }

    private fun parsePrintStatement(): Stmt {
        val value = parseExpression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun parseVarDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")

        var initializer: Expr = Expr.Invalid()
        if (match(EQUAL)) {
            initializer = parseExpression()
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)

    }

    private fun parseExpressionStatement(): Stmt {
        val expr = parseExpression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun parseBlock(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(parseDeclaration())
        }
        consume(RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun parseAssignment(): Expr {
        val expr = parseEquality()
        if (match(EQUAL)) {
            val equals = previous()
            val value = parseAssignment()
            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }
            error(equals, "Invalid assignment target.")
        }
        return expr
    }

    private fun parseComma(): Expr {
        var expr = parseTernary()

        while (match(COMMA)) {
            expr = Expr.Comma(expr, parseTernary())
        }

        return expr
    }

    private fun parseTernary(): Expr {
        val expr = parseAssignment()

        if (match(QUESTION)){
            val left = parseExpression()
            consume(COLON, "Ternary operator is of the form <cond> ? <expr1> : <expr2>")
            val right = parseTernary()
            return Expr.Ternary(expr, left, right)
        }

        return expr
    }

    private fun parseEquality(): Expr {
        var expr = parseComparison()

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            expr = Expr.Binary(expr, previous(), parseComparison())
        }

        return expr
    }

    private fun parseComparison(): Expr {
        var expr = parseTerm()

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            expr = Expr.Binary(expr, previous(), parseTerm())
        }

        return expr
    }

    private fun parseTerm(): Expr {
        var expr: Expr = parseFactor()

        while (match(MINUS, PLUS)) {
            expr = Expr.Binary(expr, previous(), parseFactor())
        }

        return expr
    }

    private fun parseFactor(): Expr {
        var expr = parseUnary()

        while (match(SLASH, STAR)) {
            expr = Expr.Binary(expr, previous(), parseUnary())
        }

        return expr
    }

    private fun parseUnary(): Expr {
        return if (match(BANG, MINUS)) {
            Expr.Unary(previous(), parseUnary())
        } else {
            parsePrimary()
        }
    }

    private fun parsePrimary(): Expr {
        if (match(FALSE)) return Expr.Literal(false)
        else if (match(TRUE)) return Expr.Literal(true)
        else if (match(NIL)) return Expr.Literal(null)
        else if (match(NUMBER, STRING)) return Expr.Literal(previous().literal)
        else if (match(IDENTIFIER)) return Expr.Variable(previous())
        else if (match(LEFT_PAREN)) {
            val expr = parseExpression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
            return Expr.Grouping(expr)
        }
        // better error messages for some invalid expressions
        else if (match(BANG_EQUAL, EQUAL_EQUAL)) {
            error(previous(), "Missing left-hand operand.")
            parseEquality()
            return Expr.Invalid()
        }
        else if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            error(previous(), "Missing left-hand operand.")
            parseComparison()
            return Expr.Invalid()
        }
        else if (match(PLUS)) {
            error(previous(), "Missing left-hand operand.")
            parseTerm()
            return Expr.Invalid()
        }
        else if (match(SLASH, STAR)) {
            error(previous(), "Missing left-hand operand.")
            parseFactor()
            return Expr.Invalid()
        }
        throw error(peek(), "Expect expression.")
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun check(type: TokenType): Boolean {
        return if (isAtEnd()) false else peek().type === type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean {
        return peek().type === EOF
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }

    private fun error(token: Token, message: String): ParseError {
        Klox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type === SEMICOLON) return
            when (peek().type) {
                CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
                else -> {}
            }
            advance()
        }
    }
}
