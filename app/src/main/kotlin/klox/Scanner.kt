package klox

import klox.TokenType.*

class Scanner(private val source: String) {
    private val tokens = ArrayList<Token>()
    private var start = 0
    private var current = 0
    private var line = 1

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            when (val c = advance()) {
                '(' -> addToken(LEFT_PAREN)
                ')' -> addToken(RIGHT_PAREN)
                '{' -> addToken(LEFT_BRACE)
                '}' -> addToken(RIGHT_BRACE)
                '[' -> addToken(LEFT_BRACKET)
                ']' -> addToken(RIGHT_BRACKET)
                ',' -> addToken(COMMA)
                '.' -> addToken(DOT)
                '-' -> addToken(MINUS)
                '+' -> addToken(if (match('+')) PLUS_PLUS else PLUS)
                ';' -> addToken(SEMICOLON)
                '*' -> addToken(STAR)
                '%' -> addToken(PERCENT)
                '?' -> addToken(QUESTION)
                ':' -> addToken(COLON)
                '!' -> addToken(if (match('=')) BANG_EQUAL else BANG)
                '=' -> addToken(if (match('=')) EQUAL_EQUAL else EQUAL)
                '<' -> addToken(if (match('=')) LESS_EQUAL else LESS)
                '>' -> addToken(if (match('=')) GREATER_EQUAL else GREATER)
                '/' -> {
                    // comment handling, since they start with '//' or '/*'
                    if (match('/')) {
                        while (peek() != '\n' && !isAtEnd()) {
                            advance()
                        }
                    } else if (match('*')) {
                        while (peek() != '*' && peekNext() != '/') {
                            advance()
                        }
                        advance()
                        advance()
                    } else {
                        addToken(SLASH)
                    }
                }
                ' ' -> {}
                '\r' -> {}
                '\t' -> {}
                '\n' -> line++
                '"' -> scanString()
                else -> {
                    if (isDigit(c)) {
                        scanNumber()
                    } else if (isAlpha(c)) {
                        scanIdentifier()
                    } else {
                        Klox.error(line, "Unexpected character.")
                    }
                }
            }
        }

        tokens.add(Token(EOF, "", null, line))
        return tokens
    }

    private fun scanIdentifier() {
        while (isAlphaNumeric(peek())) advance()
        val text = source.substring(start, current)
        // is this text a keyword?
        var type = keywords[text]
        if (type == null) type = IDENTIFIER
        addToken(type)
    }

    private fun scanNumber() {
        while (isDigit(peek())) {
            advance()
        }

        if (peek() == '.' && isDigit(peekNext())) {
            // consume the "."
            advance()
            while (isDigit(peek())) advance()
        }
        addToken(NUMBER, source.substring(start, current).toDouble())
    }

    private fun scanString() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++
            }
            advance()
        }
        if (isAtEnd()) {
            Klox.error(line, "Unterminated string.")
            return
        }

        // move past the closing '"'
        advance()

        // trim the surrounding quotes to get the String's contents
        val value = source.substring(start + 1, current - 1)
        addToken(STRING, value)
    }

    private fun addToken(type: TokenType) {
        addToken(type, null)
    }

    private fun addToken(type: TokenType, literal: Any?) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }

    private fun advance(): Char {
        return source[current++]
    }

    private fun isAlpha(c: Char): Boolean {
        return (c in 'a'..'z') || (c in 'A'..'Z' || c == '_')
    }

    private fun isAlphaNumeric(c: Char): Boolean {
        return isAlpha(c) || isDigit(c)
    }

    private fun isAtEnd(): Boolean {
        return current >= source.length
    }

    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    private fun match(expected: Char): Boolean {
        return if (isAtEnd() || source[current] != expected) {
            false
        } else {
            current++
            true
        }
    }

    private fun peek(): Char {
        return if (isAtEnd()) {
            '\u0000'
        } else {
            source[current]
        }
    }

    private fun peekNext(): Char {
        return if (current + 1 >= source.length) '\u0000' else source[current + 1]
    }

    companion object {
        val keywords = hashMapOf(
            "and" to AND,
            "break" to BREAK,
            "class" to CLASS,
            "else" to ELSE,
            "false" to FALSE,
            "for" to FOR,
            "fun" to FUN,
            "if" to IF,
            "nil" to NIL,
            "or" to OR,
            "print" to PRINT,
            "return" to RETURN,
            "super" to SUPER,
            "this" to THIS,
            "true" to TRUE,
            "var" to VAR,
            "while" to WHILE
        )
    }
}
