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
    fun testMath(){
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

    private fun testOutput(expected: String){
        assertEquals(expected, outputStreamCaptor.toString().trim())
    }
}
