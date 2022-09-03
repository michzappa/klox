package klox

import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals

class KloxErrorTest {
    private val klox = Klox()
    private val outputStreamCaptor = ByteArrayOutputStream()

    @Before
    fun setup() {
        System.setErr(PrintStream(outputStreamCaptor))
    }

    @Test
    fun testUnusedVariable() {
        klox.run("var a = 1; { var b = 2; }")
        testOutput("Error: b is unused.")
    }

    private fun testOutput(expectedOut: String) {
        assertEquals(expectedOut, outputStreamCaptor.toString().trim())
    }
}
