package klox

abstract class Stmt {
  abstract fun <R> accept(visitor: Visitor<R>): R
  interface Visitor<R> {
    fun visitExpressionStmt(stmt: Expression): R
    fun visitPrintStmt(stmt: Print): R
    fun visitVarStmt(stmt: Var): R
    fun visitBlockStmt(stmt: Block): R
    fun visitInvalidStmt(stmt: Invalid): R
  }
  class Expression(val expression: Expr) : Stmt() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitExpressionStmt(this);
    }
  }
  class Print(val expression: Expr) : Stmt() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitPrintStmt(this);
    }
  }
  class Var(val name: Token, val initializer: Expr) : Stmt() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitVarStmt(this);
    }
  }
  class Block(val statements: List<Stmt>) : Stmt() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitBlockStmt(this);
    }
  }
  class Invalid : Stmt() {
    override fun <R> accept(visitor: Visitor<R> ): R {
      return visitor.visitInvalidStmt(this);
    }
  }
}
