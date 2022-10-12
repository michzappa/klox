package klox

import org.junit.Test
import kotlin.test.assertEquals

class CompilerTest {
    private val klox = Klox()

    @Test
    fun testBinaryExpressions() {
        assertEquals("(< 4 3)\n", klox.compile("4 < 3;"))
        assertEquals("(<= 4 3)\n", klox.compile("4 <= 3;"))
        assertEquals("(> 4 3)\n", klox.compile("4 > 3;"))
        assertEquals("(>= 4 3)\n", klox.compile("4 >= 3;"))
        assertEquals("(= 4 3)\n", klox.compile("4 == 3;"))
        assertEquals("(not (= 4 3))\n", klox.compile("4 != 3;"))
        assertEquals("(+ 4 4)\n", klox.compile("4 + 4;"))
        assertEquals("(- 4 3)\n", klox.compile("4 - 3;"))
        assertEquals("(* 4 3)\n", klox.compile("4 * 3;"))
        assertEquals("(/ 4 3)\n", klox.compile("4 / 3;"))
        assertEquals("(modulo 4 3)\n", klox.compile("4 % 3;"))
    }

    @Test
    fun testPrint() {
        assertEquals("(display 4) (newline)\n" +
                "(display #t) (newline)\n" +
                "(display (+ 4 4)) (newline)\n",
            klox.compile("print 4; print true; print (4 + 4);"))
    }

    @Test
    fun testFunPrint() {
        assertEquals("(define (sayHi first last) (display (string-append (string-append (string-append (string-append \"Hi, \" first) \" \") last) \"!\")) (newline))\n" +
                "(sayHi \"Dear\" \"Reader\")\n",
            klox.compile("fun sayHi(first, last) { print \"Hi, \" ++ first ++ \" \" ++ last ++ \"!\"; }\nsayHi(\"Dear\", \"Reader\");"))
    }

    @Test
    fun testDeclaration(){
        assertEquals("(define x 4)\n" +
                "(display x) (newline)\n" +
                "(define y 3)\n" +
                "(display (+ x y)) (newline)\n",
            klox.compile("var x = 4; print x; var y = 3; print (x + y);"))
    }

    @Test
    fun testAssignment(){
        assertEquals("(define x 4)\n" +
                "(display x) (newline)\n" +
                "(set! x 3)\n" +
                "(display x) (newline)\n",
            klox.compile("var x = 4; print x; x = 3; print x;"))
    }

    @Test
    fun testTernary(){
        assertEquals("(if (< 4 3) #t #f)\n",
            klox.compile("4 < 3 ? true : false;"))
    }

    @Test
    fun testWhileLoop() {
        assertEquals(
            "(define a 0)\n" +
                    "(do () ((not (< a 10))) (let ((b 2)) (display a) (newline) (set! a (+ a 1))))\n",
            klox.compile("var a = 0; while(a < 10){ var b = 2; print a; a = a + 1;}"))
    }

    @Test
    fun testForLoop() {
        assertEquals(
            "(define a 0) (do () ((not (< a 10))) (let ((b 2)) (display a) (newline) (set! a (+ a 1))))\n",
            klox.compile("for(var a = 0; a < 10; a = a + 1){ var b = 2; print a; }"))
    }

    @Test
    fun testReturn() {
        assertEquals("(define (four) 4)\n" +
                "(display (four)) (newline)\n",
            klox.compile("fun four(){ return 4; } print four();"))
    }

    @Test
    fun testIf() {
        assertEquals("(define (four) (if #t 4 5))\n" +
                "(display (four)) (newline)\n",
            klox.compile("fun four(){ if (true) { return 4; } else { return 5; } } print four();"))
    }

    @Test
    fun testLists() {
        assertEquals( "(define l (list))\n" +
                "(display (length l)) (newline)\n" +
                "(display l) (newline)\n" +
                "(display (set! l (cons 1 l))) (newline)\n" +
                "(display (set! l (cons 2 l))) (newline)\n" +
                "(display (length l)) (newline)\n" +
                "(display (car l)) (newline)\n" +
                "(display (set! l (cdr l))) (newline)\n" +
                "(display l) (newline)\n", klox.compile("var l = []; print(length(l)); print(l); print(l = cons(1, l)); print(l = cons(2, l)); print(length(l)); print(first(l)); print(l = rest(l)); print(l);"))
    }

//
//    @Test
//    fun testWhileLoopsBreak() {
//        klox.run("var a = 0; while(a < 10){ print a; a = a + 1; if(a > 4){ break; }}")
//        testOutput("0\n1\n2\n3\n4")
//    }
//
//    @Test
//    fun testForLoops() {
//        klox.run("for(var a = 0; a < 10; a = a + 1){ print a;}")
//        testOutput("0\n1\n2\n3\n4\n5\n6\n7\n8\n9")
//    }
//
//    @Test
//    fun testForLoopsBreak() {
//        klox.run("for(var a = 0; a < 10; a = a + 1){ print a; if(a > 4){ break; }}")
//        testOutput("0\n1\n2\n3\n4\n5")
//    }
//
//
//

//
//    @Test
//    fun testLambdas() {
//        klox.run("var f = fun(a, b){ return a + b; }; print(f(4, 3));")
//        testOutput("7")
//    }
//
//    @Test
//    fun testLexicalScope() {
//        klox.run("var a = 1; { fun showA() { print a; } showA(); var a  = 2; showA(); }")
//        testOutput("1\n1")
//    }
//

//
//    @Test
//    fun testClasses() {
//        klox.run("class T { init(){ this.test = true; } } print T; var t = T(); print t; print t.test;")
//        testOutput("T\nT instance\ntrue")
//    }
//
//    @Test
//    fun testStaticMethods() {
//        klox.run("class Test { class id2(n) { return n; } } print Test.id2(4);")
//        testOutput("4")
//    }
//
//    @Test
//    fun testGetters() {
//        klox.run("class Circle { init(radius) { this.radius = radius; } area { return 3.141592653 * this.radius * this.radius; } } print Circle(4).area;")
//        testOutput("50.265482448")
//    }
//
//    @Test
//    fun testInheritance() {
//        klox.run("class A { hi() { print true; } } class B < A {} B().hi();")
//        testOutput("true")
//    }
//
//    @Test
//    fun testCallSuper() {
//        klox.run("class A { hi() { print true; } } class B < A { hi() { super.hi(); print false; } } B().hi();")
//        klox.run("class A { method() { print \"A\"; } } class B < A { method() { print \"B\"; } test() { super.method(); } } class C < B {} C().test();")
//        testOutput("true\nfalse\nA")
//    }
}
