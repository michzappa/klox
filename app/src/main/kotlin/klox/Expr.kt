package klox

abstract class Expr {
  abstract fun <R> accept(visitor: Visitor<R>): R
  interface Visitor<R> {
    fun visitBinaryExpr(expr: Binary): R
    fun visitGroupingExpr(expr: Grouping): R
    fun visitLiteralExpr(expr: Literal): R
    fun visitUnaryExpr(expr: Unary): R
    fun visitVariableExpr(expr: Variable): R
    fun visitAssignExpr(expr: Assign): R
    fun visitCommaExpr(expr: Comma): R
    fun visitTernaryExpr(expr: Ternary): R
    fun visitInvalidExpr(expr: Invalid): R
  }
  class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitBinaryExpr(this);
    }
  }
  class Grouping(val expression: Expr) : Expr() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitGroupingExpr(this);
    }
  }
  class Literal(val value: Any?) : Expr() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitLiteralExpr(this);
    }
  }
  class Unary(val operator: Token, val right: Expr) : Expr() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitUnaryExpr(this);
    }
  }
  class Variable(val name: Token) : Expr() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitVariableExpr(this);
    }
  }
  class Assign(val name: Token, val value: Expr) : Expr() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitAssignExpr(this);
    }
  }
  class Comma(val left: Expr, val right: Expr) : Expr() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitCommaExpr(this);
    }
  }
  class Ternary(val cond: Expr, val left: Expr, val right: Expr) : Expr() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitTernaryExpr(this);
    }
  }
  class Invalid : Expr() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitInvalidExpr(this);
    }
  }
}
