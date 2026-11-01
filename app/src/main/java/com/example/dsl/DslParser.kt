package com.example.dsl

class DslParser {

    fun tokenize(source: String): Pair<List<Token>, List<DslValidationError>> {
        val tokens = mutableListOf<Token>()
        val errors = mutableListOf<DslValidationError>()
        var line = 1
        var col = 1
        var i = 0

        while (i < source.length) {
            val c = source[i]
            when {
                c == '\n' -> {
                    tokens.add(Token(TokenType.NEWLINE, "\n", line, col))
                    line++
                    col = 1
                    i++
                }
                c.isWhitespace() -> {
                    col++
                    i++
                }
                c == '#' || (c == '/' && i + 1 < source.length && source[i + 1] == '/') -> {
                    while (i < source.length && source[i] != '\n') {
                        i++
                    }
                }
                c == '{' -> { tokens.add(Token(TokenType.LBRACE, "{", line, col++)); i++ }
                c == '}' -> { tokens.add(Token(TokenType.RBRACE, "}", line, col++)); i++ }
                c == '(' -> { tokens.add(Token(TokenType.LPAREN, "(", line, col++)); i++ }
                c == ')' -> { tokens.add(Token(TokenType.RPAREN, ")", line, col++)); i++ }
                c == ',' -> { tokens.add(Token(TokenType.COMMA, ",", line, col++)); i++ }
                c == '+' -> { tokens.add(Token(TokenType.OP_PLUS, "+", line, col++)); i++ }
                c == '-' -> { tokens.add(Token(TokenType.OP_MINUS, "-", line, col++)); i++ }
                c == '=' -> {
                    if (i + 1 < source.length && source[i + 1] == '=') {
                        tokens.add(Token(TokenType.OP_EQ, "==", line, col))
                        col += 2; i += 2
                    } else {
                        tokens.add(Token(TokenType.EQUALS, "=", line, col++)); i++
                    }
                }
                c == '!' && i + 1 < source.length && source[i + 1] == '=' -> {
                    tokens.add(Token(TokenType.OP_NEQ, "!=", line, col))
                    col += 2; i += 2
                }
                c == '<' -> {
                    if (i + 1 < source.length && source[i + 1] == '=') {
                        tokens.add(Token(TokenType.OP_LTE, "<=", line, col))
                        col += 2; i += 2
                    } else {
                        tokens.add(Token(TokenType.OP_LT, "<", line, col++)); i++
                    }
                }
                c == '>' -> {
                    if (i + 1 < source.length && source[i + 1] == '=') {
                        tokens.add(Token(TokenType.OP_GTE, ">=", line, col))
                        col += 2; i += 2
                    } else {
                        tokens.add(Token(TokenType.OP_GT, ">", line, col++)); i++
                    }
                }
                c == '"' -> {
                    val startCol = col
                    val sb = StringBuilder()
                    i++; col++
                    while (i < source.length && source[i] != '"' && source[i] != '\n') {
                        sb.append(source[i])
                        i++; col++
                    }
                    if (i < source.length && source[i] == '"') {
                        i++; col++
                        tokens.add(Token(TokenType.STRING_LITERAL, sb.toString(), line, startCol))
                    } else {
                        errors.add(DslValidationError(line, startCol, "Unterminated string literal"))
                    }
                }
                c.isDigit() -> {
                    val startCol = col
                    val sb = StringBuilder()
                    while (i < source.length && (source[i].isDigit() || source[i] == '.')) {
                        sb.append(source[i])
                        i++; col++
                    }
                    tokens.add(Token(TokenType.NUMBER, sb.toString(), line, startCol))
                }
                c.isLetter() || c == '_' -> {
                    val startCol = col
                    val sb = StringBuilder()
                    while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_')) {
                        sb.append(source[i])
                        i++; col++
                    }
                    val word = sb.toString()
                    val tokenType = when (word.uppercase()) {
                        "VAR" -> TokenType.KEYWORD_VAR
                        "SET" -> TokenType.KEYWORD_SET
                        "CONST" -> TokenType.KEYWORD_CONST
                        "IF" -> TokenType.KEYWORD_IF
                        "ELSE" -> TokenType.KEYWORD_ELSE
                        "REPEAT" -> TokenType.KEYWORD_REPEAT
                        "FOR" -> TokenType.KEYWORD_FOR
                        "FROM" -> TokenType.KEYWORD_FROM
                        "TO" -> TokenType.KEYWORD_TO
                        "WAIT" -> TokenType.KEYWORD_WAIT
                        "TAP" -> TokenType.KEYWORD_TAP
                        "DOUBLE_TAP" -> TokenType.KEYWORD_DOUBLE_TAP
                        "LONG_PRESS" -> TokenType.KEYWORD_LONG_PRESS
                        "SWIPE" -> TokenType.KEYWORD_SWIPE
                        "SCROLL" -> TokenType.KEYWORD_SCROLL
                        "DRAG" -> TokenType.KEYWORD_DRAG
                        "GROUP" -> TokenType.KEYWORD_GROUP
                        "FUNCTION" -> TokenType.KEYWORD_FUNCTION
                        "CALL" -> TokenType.KEYWORD_CALL
                        "RUN_MACRO" -> TokenType.KEYWORD_RUN_MACRO
                        "STOP" -> TokenType.KEYWORD_STOP
                        "PAUSE" -> TokenType.KEYWORD_PAUSE
                        else -> TokenType.IDENTIFIER
                    }
                    tokens.add(Token(tokenType, word, line, startCol))
                }
                else -> {
                    errors.add(DslValidationError(line, col, "Unexpected character '$c'"))
                    i++; col++
                }
            }
        }
        tokens.add(Token(TokenType.EOF, "", line, col))
        return Pair(tokens, errors)
    }

    private class ParserContext(val tokens: List<Token>) {
        val errors = mutableListOf<DslValidationError>()
        var pos = 0

        fun peek(): Token = tokens.getOrElse(pos) { Token(TokenType.EOF, "", 0, 0) }
        fun isAtEnd(): Boolean = peek().type == TokenType.EOF
        fun advance(): Token {
            val t = peek()
            if (!isAtEnd()) pos++
            return t
        }

        fun match(vararg types: TokenType): Boolean {
            if (types.any { it == peek().type }) {
                advance()
                return true
            }
            return false
        }

        fun skipNewlines() {
            while (peek().type == TokenType.NEWLINE) {
                advance()
            }
        }

        fun parseExpression(): DslExpression? {
            val t = peek()
            return when (t.type) {
                TokenType.NUMBER -> {
                    advance()
                    val num = t.lexeme.toDoubleOrNull() ?: 0.0
                    var expr: DslExpression = NumberLiteral(num)
                    if (peek().type in listOf(TokenType.OP_PLUS, TokenType.OP_MINUS, TokenType.OP_EQ, TokenType.OP_NEQ, TokenType.OP_LT, TokenType.OP_LTE, TokenType.OP_GT, TokenType.OP_GTE)) {
                        val op = advance().type
                        val right = parseExpression() ?: NumberLiteral(0.0)
                        expr = BinaryOp(expr, op, right)
                    }
                    expr
                }
                TokenType.STRING_LITERAL -> {
                    advance()
                    StringLiteral(t.lexeme)
                }
                TokenType.IDENTIFIER -> {
                    advance()
                    var expr: DslExpression = VariableReference(t.lexeme)
                    if (peek().type in listOf(TokenType.OP_PLUS, TokenType.OP_MINUS, TokenType.OP_EQ, TokenType.OP_NEQ, TokenType.OP_LT, TokenType.OP_LTE, TokenType.OP_GT, TokenType.OP_GTE)) {
                        val op = advance().type
                        val right = parseExpression() ?: NumberLiteral(0.0)
                        expr = BinaryOp(expr, op, right)
                    }
                    expr
                }
                else -> null
            }
        }

        fun parseBlock(): List<DslStatement> {
            val blockStmts = mutableListOf<DslStatement>()
            skipNewlines()
            if (match(TokenType.LBRACE)) {
                skipNewlines()
                while (!isAtEnd() && peek().type != TokenType.RBRACE) {
                    val stmt = parseStatement()
                    if (stmt != null) blockStmts.add(stmt)
                    skipNewlines()
                }
                if (!match(TokenType.RBRACE)) {
                    errors.add(DslValidationError(peek().line, peek().column, "Expected '}' closing block"))
                }
            }
            return blockStmts
        }

        fun parseStatement(): DslStatement? {
            skipNewlines()
            if (isAtEnd()) return null
            val token = advance()
            val line = token.line

            return when (token.type) {
                TokenType.KEYWORD_TAP -> {
                    val x = parseExpression() ?: NumberLiteral(540.0)
                    val y = parseExpression() ?: NumberLiteral(960.0)
                    val dur = parseExpression()
                    TapStatement(x, y, dur, line)
                }
                TokenType.KEYWORD_DOUBLE_TAP -> {
                    val x = parseExpression() ?: NumberLiteral(540.0)
                    val y = parseExpression() ?: NumberLiteral(960.0)
                    DoubleTapStatement(x, y, line)
                }
                TokenType.KEYWORD_LONG_PRESS -> {
                    val x = parseExpression() ?: NumberLiteral(540.0)
                    val y = parseExpression() ?: NumberLiteral(960.0)
                    val dur = parseExpression() ?: NumberLiteral(800.0)
                    LongPressStatement(x, y, dur, line)
                }
                TokenType.KEYWORD_SWIPE -> {
                    val x1 = parseExpression() ?: NumberLiteral(540.0)
                    val y1 = parseExpression() ?: NumberLiteral(1400.0)
                    val x2 = parseExpression() ?: NumberLiteral(540.0)
                    val y2 = parseExpression() ?: NumberLiteral(400.0)
                    val dur = parseExpression() ?: NumberLiteral(300.0)
                    SwipeStatement(x1, y1, x2, y2, dur, line)
                }
                TokenType.KEYWORD_SCROLL -> {
                    val dir = if (peek().type == TokenType.IDENTIFIER || peek().type == TokenType.STRING_LITERAL) advance().lexeme else "UP"
                    val dur = parseExpression()
                    ScrollStatement(dir, dur, line)
                }
                TokenType.KEYWORD_WAIT -> {
                    val dur = parseExpression() ?: NumberLiteral(500.0)
                    WaitStatement(dur, line)
                }
                TokenType.KEYWORD_PAUSE -> {
                    val dur = parseExpression() ?: NumberLiteral(500.0)
                    PauseStatement(dur, line)
                }
                TokenType.KEYWORD_STOP -> StopStatement(line)
                TokenType.KEYWORD_VAR -> {
                    val name = if (peek().type == TokenType.IDENTIFIER) advance().lexeme else "v"
                    match(TokenType.EQUALS)
                    val expr = parseExpression() ?: NumberLiteral(0.0)
                    VarDeclStatement(name, expr, isConst = false, line)
                }
                TokenType.KEYWORD_CONST -> {
                    val name = if (peek().type == TokenType.IDENTIFIER) advance().lexeme else "c"
                    match(TokenType.EQUALS)
                    val expr = parseExpression() ?: NumberLiteral(0.0)
                    VarDeclStatement(name, expr, isConst = true, line)
                }
                TokenType.KEYWORD_SET -> {
                    val name = if (peek().type == TokenType.IDENTIFIER) advance().lexeme else "v"
                    match(TokenType.EQUALS)
                    val expr = parseExpression() ?: NumberLiteral(0.0)
                    VarAssignStatement(name, expr, line)
                }
                TokenType.KEYWORD_REPEAT -> {
                    val count = parseExpression() ?: NumberLiteral(5.0)
                    val body = parseBlock()
                    RepeatStatement(count, body, line)
                }
                TokenType.KEYWORD_FOR -> {
                    val varName = if (peek().type == TokenType.IDENTIFIER) advance().lexeme else "i"
                    match(TokenType.KEYWORD_FROM)
                    val startExpr = parseExpression() ?: NumberLiteral(1.0)
                    match(TokenType.KEYWORD_TO)
                    val endExpr = parseExpression() ?: NumberLiteral(5.0)
                    val body = parseBlock()
                    ForStatement(varName, startExpr, endExpr, body, line)
                }
                TokenType.KEYWORD_IF -> {
                    val cond = parseExpression() ?: NumberLiteral(1.0)
                    val thenBranch = parseBlock()
                    var elseBranch: List<DslStatement>? = null
                    skipNewlines()
                    if (match(TokenType.KEYWORD_ELSE)) {
                        elseBranch = parseBlock()
                    }
                    IfStatement(cond, thenBranch, elseBranch, line)
                }
                TokenType.KEYWORD_GROUP -> {
                    val name = if (peek().type == TokenType.STRING_LITERAL || peek().type == TokenType.IDENTIFIER) advance().lexeme else "Group"
                    val body = parseBlock()
                    GroupStatement(name, body, line)
                }
                TokenType.KEYWORD_FUNCTION -> {
                    val name = if (peek().type == TokenType.IDENTIFIER) advance().lexeme else "fn"
                    if (match(TokenType.LPAREN)) match(TokenType.RPAREN)
                    val body = parseBlock()
                    FunctionDefStatement(name, body, line)
                }
                TokenType.KEYWORD_CALL -> {
                    val name = if (peek().type == TokenType.IDENTIFIER) advance().lexeme else "fn"
                    if (match(TokenType.LPAREN)) match(TokenType.RPAREN)
                    FunctionCallStatement(name, line)
                }
                TokenType.KEYWORD_RUN_MACRO -> {
                    val id = if (peek().type == TokenType.STRING_LITERAL || peek().type == TokenType.IDENTIFIER) advance().lexeme else "macro_1"
                    RunMacroStatement(id, line)
                }
                TokenType.NEWLINE -> null
                else -> {
                    errors.add(DslValidationError(line, token.column, "Unknown statement or unexpected keyword '${token.lexeme}'"))
                    null
                }
            }
        }
    }

    fun parse(source: String): Pair<List<DslStatement>, List<DslValidationError>> {
        val (tokens, lexErrors) = tokenize(source)
        val context = ParserContext(tokens)
        context.errors.addAll(lexErrors)

        val statements = mutableListOf<DslStatement>()
        while (!context.isAtEnd()) {
            val stmt = context.parseStatement()
            if (stmt != null) statements.add(stmt)
            context.skipNewlines()
        }

        return Pair(statements, context.errors)
    }

    fun format(source: String): String {
        val (stmts, _) = parse(source)
        val sb = StringBuilder()

        fun printStatements(list: List<DslStatement>, indentLevel: Int) {
            val indent = "    ".repeat(indentLevel)
            for (stmt in list) {
                when (stmt) {
                    is TapStatement -> sb.append("${indent}TAP ${exprToString(stmt.xExpr)} ${exprToString(stmt.yExpr)}${stmt.durationExpr?.let { " " + exprToString(it) } ?: ""}\n")
                    is DoubleTapStatement -> sb.append("${indent}DOUBLE_TAP ${exprToString(stmt.xExpr)} ${exprToString(stmt.yExpr)}\n")
                    is LongPressStatement -> sb.append("${indent}LONG_PRESS ${exprToString(stmt.xExpr)} ${exprToString(stmt.yExpr)} ${exprToString(stmt.durationExpr)}\n")
                    is SwipeStatement -> sb.append("${indent}SWIPE ${exprToString(stmt.x1Expr)} ${exprToString(stmt.y1Expr)} ${exprToString(stmt.x2Expr)} ${exprToString(stmt.y2Expr)}${stmt.durationExpr?.let { " " + exprToString(it) } ?: ""}\n")
                    is ScrollStatement -> sb.append("${indent}SCROLL ${stmt.direction}\n")
                    is WaitStatement -> sb.append("${indent}WAIT ${exprToString(stmt.durationExpr)}\n")
                    is PauseStatement -> sb.append("${indent}PAUSE ${exprToString(stmt.durationExpr)}\n")
                    is StopStatement -> sb.append("${indent}STOP\n")
                    is VarDeclStatement -> sb.append("${indent}${if (stmt.isConst) "CONST" else "VAR"} ${stmt.name} = ${exprToString(stmt.expr)}\n")
                    is VarAssignStatement -> sb.append("${indent}SET ${stmt.name} = ${exprToString(stmt.expr)}\n")
                    is RepeatStatement -> {
                        sb.append("${indent}REPEAT ${exprToString(stmt.countExpr)} {\n")
                        printStatements(stmt.body, indentLevel + 1)
                        sb.append("${indent}}\n")
                    }
                    is ForStatement -> {
                        sb.append("${indent}FOR ${stmt.varName} FROM ${exprToString(stmt.startExpr)} TO ${exprToString(stmt.endExpr)} {\n")
                        printStatements(stmt.body, indentLevel + 1)
                        sb.append("${indent}}\n")
                    }
                    is IfStatement -> {
                        sb.append("${indent}IF ${exprToString(stmt.condition)} {\n")
                        printStatements(stmt.thenBranch, indentLevel + 1)
                        if (stmt.elseBranch != null) {
                            sb.append("${indent}} ELSE {\n")
                            printStatements(stmt.elseBranch, indentLevel + 1)
                        }
                        sb.append("${indent}}\n")
                    }
                    is GroupStatement -> {
                        sb.append("${indent}GROUP \"${stmt.name}\" {\n")
                        printStatements(stmt.body, indentLevel + 1)
                        sb.append("${indent}}\n")
                    }
                    is FunctionDefStatement -> {
                        sb.append("${indent}FUNCTION ${stmt.name}() {\n")
                        printStatements(stmt.body, indentLevel + 1)
                        sb.append("${indent}}\n")
                    }
                    is FunctionCallStatement -> sb.append("${indent}CALL ${stmt.name}()\n")
                    is RunMacroStatement -> sb.append("${indent}RUN_MACRO \"${stmt.macroId}\"\n")
                }
            }
        }

        printStatements(stmts, 0)
        return if (sb.isNotEmpty()) sb.toString().trimEnd() else source
    }

    private fun exprToString(expr: DslExpression): String {
        return when (expr) {
            is NumberLiteral -> if (expr.value % 1.0 == 0.0) expr.value.toLong().toString() else expr.value.toString()
            is StringLiteral -> "\"${expr.value}\""
            is VariableReference -> expr.name
            is BinaryOp -> "${exprToString(expr.left)} ${opToString(expr.operator)} ${exprToString(expr.right)}"
        }
    }

    private fun opToString(op: TokenType): String = when (op) {
        TokenType.OP_PLUS -> "+"
        TokenType.OP_MINUS -> "-"
        TokenType.OP_EQ -> "=="
        TokenType.OP_NEQ -> "!="
        TokenType.OP_LT -> "<"
        TokenType.OP_LTE -> "<="
        TokenType.OP_GT -> ">"
        TokenType.OP_GTE -> ">="
        else -> "="
    }
}
