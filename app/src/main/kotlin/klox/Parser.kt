package klox

import klox.Expr.Logical
import klox.TokenType.*

class Parser(private val tokens: List<Token>, private val createError: Boolean = true) {
    class ParseError : RuntimeException()

    private var current = 0
    private var loopDepth = 0

    fun parse(): List<Stmt> {
        val statements = ArrayList<Stmt>()
        while (!isAtEnd()) {
            statements.add(parseDeclaration())
        }

        return statements
    }

    private fun parseDeclaration(): Stmt {
        return try {
            if (match(CLASS)) return parseClassDeclaration()
            else if (checkNext(IDENTIFIER) && match(FUN)) parseFunction("function")
            else if (match(VAR)) parseVarDeclaration()
            else parseStatement()
        } catch (error: ParseError) {
            synchronize()
            Stmt.Invalid(tokens[current])
        }
    }

    private fun parseClassDeclaration(): Stmt {
        val name: Token = consume(IDENTIFIER, "Expect class name.")

        var superclass: Expr.Variable? = null
        if (match(LESS)) {
            consume(IDENTIFIER, "Expect superclass name.")
            superclass = Expr.Variable(previous())
        }

        consume(LEFT_BRACE, "Expect '{' before class body.")

        val methods: MutableList<Stmt.Function> = ArrayList()
        val staticMethods: MutableList<Stmt.Function> = ArrayList()

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            if (match(CLASS)) {
                staticMethods
            } else {
                methods
            }.add(parseFunction("method"))
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.")

        return Stmt.Class(name, superclass, methods, staticMethods)
    }

    private fun parseFunction(kind: String): Stmt.Function {
        val name = consume(IDENTIFIER, "Expect $kind name.")
        val parameters: MutableList<Token> = ArrayList()
        var isGetter = true

        // getter "methods" have no parameters
        if (kind != ("method") || check(LEFT_PAREN)) {
            isGetter = false
            consume(LEFT_PAREN, "Expect '(' after $kind name.")
            if (!check(RIGHT_PAREN)) {
                do {
                    if (parameters.size >= 255) {
                        error(peek(), "Can't have more than 255 parameters.")
                    } else {
                        parameters.add(consume(IDENTIFIER, "Expect parameter name."))
                    }
                } while (match(COMMA))
            }
            consume(RIGHT_PAREN, "Expect ')' after parameters.")
        }
        consume(LEFT_BRACE, "Expect '{' before $kind body.")

        return Stmt.Function(name, parameters, parseBlock(), isGetter)
    }

    private fun parseVarDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")

        var initializer: Expr = Expr.Invalid(tokens[current])
        if (match(EQUAL)) {
            initializer = parseExpression()
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun parseStatement(): Stmt {
        return if (match(LEFT_BRACE)) Stmt.Block(parseBlock())
        else if (match(BREAK)) parseBreakStatement()
        else if (match(FOR)) parseForStatement()
        else if (match(IF)) parseIfStatement()
        else if (match(PRINT)) parsePrintStatement()
        else if (match(RETURN)) parseReturnStatement()
        else if (match(WHILE)) parseWhileStatement()
        else parseExpressionStatement()
    }

    private fun parseBlock(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(parseDeclaration())
        }
        consume(RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun parseBreakStatement(): Stmt {
        if (loopDepth == 0) {
            error(previous(), "'break' cannot be used outside of a loop.")
        }
        consume(SEMICOLON, "Expect ';' after 'break'.")
        return Stmt.Break()
    }

    private fun parseForStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'for'.")
        val initializer = if (match(SEMICOLON)) {
            Stmt.Invalid(tokens[current])
        } else if (match(VAR)) {
            parseVarDeclaration()
        } else {
            parseExpressionStatement()
        }

        val condition = if (!check(SEMICOLON)) {
            parseExpression()
        } else {
            Expr.Literal(true, tokens[current])
        }
        consume(SEMICOLON, "Expect ';' after loop condition.")

        val increment = if (!check(RIGHT_PAREN)) {
            parseExpression()
        } else {
            Expr.Invalid(tokens[current])
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.")

        try {
            loopDepth += 1

            val body = if (increment is Expr.Invalid) {
                parseStatement()
            } else {
                Stmt.Block(listOf(parseStatement(), Stmt.Expression(increment)))
            }

            return if (initializer is Stmt.Invalid) {
                Stmt.While(condition, body)
            } else {
                Stmt.Block(listOf(initializer, Stmt.While(condition, body)))
            }
        } finally {
            loopDepth -= 1
        }
    }

    private fun parseIfStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = parseExpression()
        consume(RIGHT_PAREN, "Expect ')' after if condition.")
        val thenBranch = parseStatement()
        val elseBranch = if (match(ELSE)) {
            parseStatement()
        } else Stmt.Invalid(tokens[current])

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun parsePrintStatement(): Stmt {
        val value = parseExpression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun parseReturnStatement(): Stmt {
        val keyword = previous()
        val value = if (!check(SEMICOLON)) {
            parseExpression()
        } else {
            Expr.Literal(null, tokens[current])
        }
        consume(SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
    }

    private fun parseWhileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition: Expr = parseExpression()
        consume(RIGHT_PAREN, "Expect ')' after condition.")
        try {
            loopDepth += 1
            return Stmt.While(condition, parseStatement())
        } finally {
            loopDepth -= 1
        }
    }

    private fun parseExpressionStatement(): Stmt {
        val expr = parseExpression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    fun parseExpression(): Expr {
        return parseComma()
    }

    private fun parseComma(): Expr {
        var expr = parseLambda()

        while (match(COMMA)) {
            expr = Expr.Comma(expr, parseLambda(), tokens[current - 1])
        }

        return expr
    }

    private fun parseLambda(): Expr {
        if (match(FUN)) {
            val funToken = tokens[current]
            consume(LEFT_PAREN, "Expect '(' after 'fun'")
            val parameters: MutableList<Token> = ArrayList()
            if (!check(RIGHT_PAREN)) {
                do {
                    if (parameters.size >= 255) {
                        error(peek(), "Can't have more than 255 parameters.")
                    } else {
                        parameters.add(consume(IDENTIFIER, "Expect parameter name."))
                    }
                } while (match(COMMA))
            }
            consume(RIGHT_PAREN, "Expect ')' after parameters.")
            consume(LEFT_BRACE, "Expect '{' before lambda body.")

            return Expr.Lambda(parameters, parseBlock(), funToken)
        } else {
            return parseTernary()
        }
    }

    private fun parseTernary(): Expr {
        val expr = parseAssignment()

        if (match(QUESTION)) {
            val questionToken = tokens[current]
            val left = parseExpression()
            consume(COLON, "Ternary operator is of the form <cond> ? <expr1> : <expr2>")
            val right = parseTernary()
            return Expr.Conditional(expr, left, right, questionToken)
        }

        return expr
    }

    private fun parseAssignment(): Expr {
        val expr = parseOr()

        if (match(EQUAL)) {
            val equals = previous()
            val value = parseAssignment()
            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value, tokens[current - 1])
            } else if (expr is Expr.Get) {
                return Expr.Set(expr.obj, expr.name, value)
            }
            error(equals, "Invalid assignment target.")
        }
        return expr
    }

    private fun parseOr(): Expr {
        var expr = parseAnd()
        while (match(OR)) {
            val operator = previous()
            val right: Expr = parseAnd()
            expr = Logical(expr, operator, right)
        }
        return expr
    }

    private fun parseAnd(): Expr {
        var expr = parseEquality()

        while (match(AND)) {
            val operator = previous()
            val right = parseEquality()
            expr = Logical(expr, operator, right)
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
        var expr = parseFactor()

        while (match(MINUS, PLUS)) {
            expr = Expr.Binary(expr, previous(), parseFactor())
        }

        return expr
    }

    private fun parseFactor(): Expr {
        var expr = parseUnary()

        while (match(SLASH, STAR, PERCENT)) {
            expr = Expr.Binary(expr, previous(), parseUnary())
        }

        return expr
    }

    private fun parseUnary(): Expr {
        return if (match(BANG, MINUS)) {
            Expr.Unary(previous(), parseUnary())
        } else {
            parseCall()
        }
    }

    private fun parseCall(): Expr {
        var expr = parsePrimary()

        while (true) {
            expr = if (match(LEFT_PAREN)) {
                val arguments: MutableList<Expr> = ArrayList()

                if (!check(RIGHT_PAREN)) {
                    do {
                        if (arguments.size >= 255) {
                            error(peek(), "Can't have more than 255 arguments.")
                        }
                        arguments.add(parseLambda())
                    } while (match(COMMA))
                }

                val paren = consume(RIGHT_PAREN, "Expect ')' after arguments.")

                Expr.Call(expr, paren, arguments, expr.token)
            } else if (match(DOT)) {
                val name = consume(IDENTIFIER, "Expect property name after '.'.")
                Expr.Get(expr, name)
            } else {
                break
            }
        }

        return expr
    }

    private fun parsePrimary(): Expr {
        if (match(FALSE)) return Expr.Literal(false, tokens[current - 1])
        else if (match(TRUE)) return Expr.Literal(true, tokens[current - 1])
        else if (match(NIL)) return Expr.Literal(null, tokens[current - 1])
        else if (match(NUMBER, STRING)) return Expr.Literal(previous().literal, tokens[current - 1])
        else if (match(SUPER)) {
            val keyword = previous()
            consume(DOT, "Expect '.' after 'super'.")
            val method = consume(IDENTIFIER, "Expect superclass method name.")
            return Expr.Super(keyword, method)
        } else if (match(THIS)) return Expr.This(previous())
        else if (match(IDENTIFIER)) return Expr.Variable(previous())
        else if (match(LEFT_PAREN)) {
            val expr = parseExpression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
            return Expr.Grouping(expr, tokens[current - 1])
        } else if (match(LEFT_BRACKET)) {
            val exprs = parseKloxList()
            consume(RIGHT_BRACKET, "Expect ']' to close list.")
            return Expr.KloxList(exprs, tokens[current - 1])
        }
        // better error messages for some invalid expressions
        else if (match(BANG_EQUAL, EQUAL_EQUAL)) {
            error(previous(), "Missing left-hand operand.")
            parseEquality()
            return Expr.Invalid(tokens[current])
        } else if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            error(previous(), "Missing left-hand operand.")
            parseComparison()
            return Expr.Invalid(tokens[current])
        } else if (match(PLUS)) {
            error(previous(), "Missing left-hand operand.")
            parseTerm()
            return Expr.Invalid(tokens[current])
        } else if (match(SLASH, STAR)) {
            error(previous(), "Missing left-hand operand.")
            parseFactor()
            return Expr.Invalid(tokens[current])
        } else {
            throw error(peek(), "Expect expression.")
        }
    }

    private fun parseKloxList(): List<Expr> {
        val values: MutableList<Expr> = ArrayList()
        if (!check(RIGHT_BRACKET)) {
            do {
                values.add(parseLambda())
            } while (match(COMMA))
        }
        return values
    }

    // utility methods
    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }
    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun check(type: TokenType): Boolean {
        return if (isAtEnd()) false else peek().type === type
    }

    private fun checkNext(type: TokenType): Boolean {
        return if (isAtEnd()) false else peekNext().type === type
    }

    private fun error(token: Token, message: String): ParseError {
        if (createError) {
            Klox.hadError = true
            Klox.error(token, message)
        }
        return ParseError()
    }

    private fun isAtEnd(): Boolean {
        return peek().type === EOF
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

    private fun peek(): Token {
        return tokens[current]
    }

    private fun peekNext(): Token {
        return tokens[current + 1]
    }

    private fun previous(): Token {
        return tokens[current - 1]
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
