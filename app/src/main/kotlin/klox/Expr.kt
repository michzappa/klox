package klox

abstract class Expr(val token: Token) {
    abstract fun <R> accept(visitor: Visitor<R>): R

    interface Visitor<R> {
        fun visitAssignExpr(expr: Assign): R
        fun visitBinaryExpr(expr: Binary): R
        fun visitCallExpr(expr: Call): R
        fun visitCommaExpr(expr: Comma): R
        fun visitConditionalExpr(expr: Conditional): R
        fun visitGetExpr(expr: Get): R
        fun visitGroupingExpr(expr: Grouping): R
        fun visitInvalidExpr(expr: Invalid): R
        fun visitKloxListExpr(expr: KloxList): R
        fun visitLambdaExpr(expr: Lambda): R
        fun visitLiteralExpr(expr: Literal): R
        fun visitLogicalExpr(expr: Logical): R
        fun visitSetExpr(expr: Set): R
        fun visitSuperExpr(expr: Super): R
        fun visitThisExpr(expr: This): R
        fun visitUnaryExpr(expr: Unary): R
        fun visitVariableExpr(expr: Variable): R
    }

    class Assign(val name: Token, val value: Expr, token: Token) : Expr(token) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitAssignExpr(this)
        }
    }

    class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr(operator) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitBinaryExpr(this)
        }
    }

    class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>, token: Token) : Expr(token) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitCallExpr(this)
        }
    }

    class Comma(val left: Expr, val right: Expr, token: Token) : Expr(token) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitCommaExpr(this)
        }
    }

    class Conditional(val cond: Expr, val left: Expr, val right: Expr, token: Token) : Expr(token) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitConditionalExpr(this)
        }
    }

    class Get(val obj: Expr, val name: Token) : Expr(name) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitGetExpr(this)
        }
    }

    class Grouping(val expression: Expr, token: Token) : Expr(token) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitGroupingExpr(this)
        }
    }

    class Invalid(token: Token) : Expr(token) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitInvalidExpr(this)
        }
    }

    class KloxList(val values: List<Expr>, token: Token) : Expr(token) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitKloxListExpr(this)
        }
    }

    class Lambda(val params: List<Token>, val body: List<Stmt>, token: Token) : Expr(token) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitLambdaExpr(this)
        }
    }

    class Literal(val value: Any?, token: Token) : Expr(token) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitLiteralExpr(this)
        }
    }

    class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr(operator) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitLogicalExpr(this)
        }
    }

    class Set(val obj: Expr, val name: Token, val value: Expr) : Expr(name) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitSetExpr(this)
        }
    }

    class Super(val keyword: Token, val method: Token) : Expr(keyword) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitSuperExpr(this)
        }
    }

    class This(val keyword: Token) : Expr(keyword) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitThisExpr(this)
        }
    }

    class Unary(val operator: Token, val right: Expr) : Expr(operator) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitUnaryExpr(this)
        }
    }

    class Variable(val name: Token) : Expr(name) {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitVariableExpr(this)
        }
    }
}
