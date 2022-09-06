package klox

import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals

/*
 * Likely-incomplete unit test suite, for regression testing refactoring.
 */

class KloxTest {
    private val klox = Klox()
    private val outputStreamCaptor = ByteArrayOutputStream()

    @Before
    fun setup() {
        System.setOut(PrintStream(outputStreamCaptor))
    }

    @Test
    fun testWhileLoops() {
        klox.run("var a = 0; while(a < 10){ print a; a = a + 1;}")
        testOutput("0\n1\n2\n3\n4\n5\n6\n7\n8\n9")
    }

    @Test
    fun testWhileLoopsBreak() {
        klox.run("var a = 0; while(a < 10){ print a; a = a + 1; if(a > 4){ break; }}")
        testOutput("0\n1\n2\n3\n4")
    }

    @Test
    fun testForLoops() {
        klox.run("for(var a = 0; a < 10; a = a + 1){ print a;}")
        testOutput("0\n1\n2\n3\n4\n5\n6\n7\n8\n9")
    }

    @Test
    fun testForLoopsBreak() {
        klox.run("for(var a = 0; a < 10; a = a + 1){ print a; if(a > 4){ break; }}")
        testOutput("0\n1\n2\n3\n4\n5")
    }

    @Test
    fun testMath() {
        klox.run("4 + 4", true)
        klox.run("4 - 2", true)
        klox.run("5 * 3", true)
        klox.run("10 / 4", true)
        testOutput("8.0\n2.0\n15.0\n2.5")
    }

    @Test
    fun testPrint() {
        klox.run("print 4; print true; print (4 + 4);")
        testOutput("4\ntrue\n8")
    }

    @Test
    fun testFunPrint() {
        klox.run(
            "fun sayHi(first, last) { print \"Hi, \" + first + \" \" + last + \"!\"; }\n" +
                "sayHi(\"Dear\", \"Reader\");"
        )
        testOutput("Hi, Dear Reader!")
    }

    @Test
    fun testLambdas() {
        klox.run("var f = fun(a, b){ return a + b; }; print(f(4, 3));")
        testOutput("7")
    }

    @Test
    fun testLexicalScope() {
        klox.run("var a = 1; { fun showA() { print a; } showA(); var a  = 2; showA(); }")
        testOutput("1\n1")
    }

    @Test
    fun testLists() {
        klox.run("var l = []; print(length(l)); print(l); print(l = cons(1, l)); print(l = cons(2, l)); print(length(l)); print(head(l)); print(l = tail(l)); print(l);")
        testOutput("0\n[]\n[1.0]\n[2.0, 1.0]\n2\n2\n[1.0]\n[1.0]")
    }

    private fun testOutput(expectedOut: String) {
        assertEquals(expectedOut, outputStreamCaptor.toString().trim())
    }
}
