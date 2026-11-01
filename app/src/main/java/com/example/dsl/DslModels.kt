package com.example.dsl

enum class TokenType {
    KEYWORD_VAR,
    KEYWORD_SET,
    KEYWORD_CONST,
    KEYWORD_IF,
    KEYWORD_ELSE,
    KEYWORD_REPEAT,
    KEYWORD_FOR,
    KEYWORD_FROM,
    KEYWORD_TO,
    KEYWORD_WAIT,
    KEYWORD_TAP,
    KEYWORD_DOUBLE_TAP,
    KEYWORD_LONG_PRESS,
    KEYWORD_SWIPE,
    KEYWORD_SCROLL,
    KEYWORD_DRAG,
    KEYWORD_GROUP,
    KEYWORD_FUNCTION,
    KEYWORD_CALL,
    KEYWORD_RUN_MACRO,
    KEYWORD_STOP,
    KEYWORD_PAUSE,
    
    IDENTIFIER,
    NUMBER,
    STRING_LITERAL,
    
    EQUALS,
    OP_EQ,
    OP_NEQ,
    OP_LT,
    OP_LTE,
    OP_GT,
    OP_GTE,
    OP_PLUS,
    OP_MINUS,
    
    LBRACE,
    RBRACE,
    LPAREN,
    RPAREN,
    COMMA,
    NEWLINE,
    EOF
}

data class Token(
    val type: TokenType,
    val lexeme: String,
    val line: Int,
    val column: Int
)

data class DslValidationError(
    val line: Int,
    val column: Int,
    val message: String,
    val isFatal: Boolean = true
)

sealed class DslStatement(val line: Int)

data class TapStatement(val xExpr: DslExpression, val yExpr: DslExpression, val durationExpr: DslExpression?, val lineNum: Int) : DslStatement(lineNum)
data class DoubleTapStatement(val xExpr: DslExpression, val yExpr: DslExpression, val lineNum: Int) : DslStatement(lineNum)
data class LongPressStatement(val xExpr: DslExpression, val yExpr: DslExpression, val durationExpr: DslExpression, val lineNum: Int) : DslStatement(lineNum)
data class SwipeStatement(val x1Expr: DslExpression, val y1Expr: DslExpression, val x2Expr: DslExpression, val y2Expr: DslExpression, val durationExpr: DslExpression?, val lineNum: Int) : DslStatement(lineNum)
data class ScrollStatement(val direction: String, val durationExpr: DslExpression?, val lineNum: Int) : DslStatement(lineNum)
data class WaitStatement(val durationExpr: DslExpression, val lineNum: Int) : DslStatement(lineNum)
data class PauseStatement(val durationExpr: DslExpression, val lineNum: Int) : DslStatement(lineNum)
data class StopStatement(val lineNum: Int) : DslStatement(lineNum)
data class VarDeclStatement(val name: String, val expr: DslExpression, val isConst: Boolean, val lineNum: Int) : DslStatement(lineNum)
data class VarAssignStatement(val name: String, val expr: DslExpression, val lineNum: Int) : DslStatement(lineNum)
data class RepeatStatement(val countExpr: DslExpression, val body: List<DslStatement>, val lineNum: Int) : DslStatement(lineNum)
data class ForStatement(val varName: String, val startExpr: DslExpression, val endExpr: DslExpression, val body: List<DslStatement>, val lineNum: Int) : DslStatement(lineNum)
data class IfStatement(val condition: DslExpression, val thenBranch: List<DslStatement>, val elseBranch: List<DslStatement>?, val lineNum: Int) : DslStatement(lineNum)
data class GroupStatement(val name: String, val body: List<DslStatement>, val lineNum: Int) : DslStatement(lineNum)
data class FunctionDefStatement(val name: String, val body: List<DslStatement>, val lineNum: Int) : DslStatement(lineNum)
data class FunctionCallStatement(val name: String, val lineNum: Int) : DslStatement(lineNum)
data class RunMacroStatement(val macroId: String, val lineNum: Int) : DslStatement(lineNum)

sealed class DslExpression
data class NumberLiteral(val value: Double) : DslExpression()
data class StringLiteral(val value: String) : DslExpression()
data class VariableReference(val name: String) : DslExpression()
data class BinaryOp(val left: DslExpression, val operator: TokenType, val right: DslExpression) : DslExpression()

data class ScriptMetadata(
    val id: String,
    val name: String,
    val description: String,
    val category: String = "General",
    val author: String = "User",
    val version: String = "1.0.0",
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
