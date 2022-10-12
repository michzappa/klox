package klox

import org.junit.Test
import kotlin.test.assertEquals

class ResolverTest {
    private val klox = Klox()

    @Test
    fun testDoubleVarError() {
        assertEquals(listOf(Stmt.Var(Token(TokenType.IDENTIFIER, "0", null, 1),
            Expr.Literal(1, Token(TokenType.NUMBER, "1", 1, 1))),
            Stmt.Var(Token(TokenType.IDENTIFIER, "1", null, 2),
                Expr.Literal(2, Token(TokenType.NUMBER, "2", 2, 1)))).map { toString() },
        klox.resolve(listOf(
            Stmt.Var(Token(TokenType.IDENTIFIER, "a", null, 1),
                Expr.Literal(1, Token(TokenType.NUMBER, "1", 1, 1))),
            Stmt.Var(Token(TokenType.IDENTIFIER, "a", null, 2),
                Expr.Literal(2, Token(TokenType.NUMBER, "2", 2, 1))))).map { toString() })
    }


}