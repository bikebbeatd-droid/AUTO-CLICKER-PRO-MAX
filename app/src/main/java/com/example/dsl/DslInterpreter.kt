package com.example.dsl

import com.example.data.model.GestureType
import com.example.engine.GestureAutomationEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InterpreterDebugState(
    val currentLine: Int = 0,
    val totalStepsExecuted: Int = 0,
    val variables: Map<String, String> = emptyMap(),
    val isRunning: Boolean = false,
    val lastAction: String = "IDLE",
    val error: String? = null
)

class DslInterpreter(
    private val automationEngine: GestureAutomationEngine,
    private val onActionDispatched: (Float, Float, GestureType) -> Unit = { _, _, _ -> }
) {
    private val _debugState = MutableStateFlow(InterpreterDebugState())
    val debugState: StateFlow<InterpreterDebugState> = _debugState.asStateFlow()

    private val variables = mutableMapOf<String, Double>()
    private val constants = mutableSetOf<String>()
    private val functions = mutableMapOf<String, List<DslStatement>>()
    private var isCancelled = false
    private var isPaused = false
    private var stepCount = 0
    private val MAX_STEPS = 10000

    fun cancel() {
        isCancelled = true
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    suspend fun execute(statements: List<DslStatement>, stepByStep: Boolean = false): Boolean {
        isCancelled = false
        isPaused = false
        stepCount = 0
        variables.clear()
        constants.clear()
        functions.clear()

        _debugState.value = InterpreterDebugState(isRunning = true, lastAction = "Starting script...")

        try {
            // First pass: register function definitions
            for (stmt in statements) {
                if (stmt is FunctionDefStatement) {
                    functions[stmt.name] = stmt.body
                }
            }

            executeStatementList(statements, stepByStep)
            _debugState.value = _debugState.value.copy(
                isRunning = false,
                lastAction = "Completed successfully ($stepCount steps)",
                error = null
            )
            return true
        } catch (e: CancellationException) {
            _debugState.value = _debugState.value.copy(
                isRunning = false,
                lastAction = "Execution stopped by user",
                error = "Stopped"
            )
            return false
        } catch (e: Exception) {
            _debugState.value = _debugState.value.copy(
                isRunning = false,
                lastAction = "Execution failed",
                error = e.message ?: "Unknown error"
            )
            return false
        }
    }

    private suspend fun executeStatementList(list: List<DslStatement>, stepByStep: Boolean) {
        for (stmt in list) {
            while (isPaused && !isCancelled) {
                delay(100)
            }
            if (isCancelled) throw CancellationException("Script cancelled")
            if (stepCount++ > MAX_STEPS) {
                throw IllegalStateException("Execution limit exceeded (Maximum $MAX_STEPS steps allowed).")
            }

            _debugState.value = _debugState.value.copy(
                currentLine = stmt.line,
                totalStepsExecuted = stepCount,
                variables = variables.mapValues { if (it.value % 1.0 == 0.0) it.value.toLong().toString() else "%.2f".format(it.value) }
            )

            executeStatement(stmt, stepByStep)

            if (stepByStep) {
                delay(400) // Delay for human observation during debugging
            }
        }
    }

    private suspend fun executeStatement(stmt: DslStatement, stepByStep: Boolean) {
        when (stmt) {
            is TapStatement -> {
                val x = eval(stmt.xExpr).toFloat()
                val y = eval(stmt.yExpr).toFloat()
                val dur = stmt.durationExpr?.let { eval(it).toLong() } ?: 50L
                _debugState.value = _debugState.value.copy(lastAction = "TAP (${x.toInt()}, ${y.toInt()})")
                onActionDispatched(x, y, GestureType.TAP)
                delay(dur.coerceAtLeast(10L))
            }
            is DoubleTapStatement -> {
                val x = eval(stmt.xExpr).toFloat()
                val y = eval(stmt.yExpr).toFloat()
                _debugState.value = _debugState.value.copy(lastAction = "DOUBLE_TAP (${x.toInt()}, ${y.toInt()})")
                onActionDispatched(x, y, GestureType.DOUBLE_TAP)
                delay(60L)
                onActionDispatched(x, y, GestureType.DOUBLE_TAP)
                delay(100L)
            }
            is LongPressStatement -> {
                val x = eval(stmt.xExpr).toFloat()
                val y = eval(stmt.yExpr).toFloat()
                val dur = eval(stmt.durationExpr).toLong()
                _debugState.value = _debugState.value.copy(lastAction = "LONG_PRESS ${dur}ms (${x.toInt()}, ${y.toInt()})")
                onActionDispatched(x, y, GestureType.LONG_PRESS)
                delay(dur.coerceAtLeast(500L))
            }
            is SwipeStatement -> {
                val x1 = eval(stmt.x1Expr).toFloat()
                val y1 = eval(stmt.y1Expr).toFloat()
                val x2 = eval(stmt.x2Expr).toFloat()
                val y2 = eval(stmt.y2Expr).toFloat()
                val dur = stmt.durationExpr?.let { eval(it).toLong() } ?: 300L
                _debugState.value = _debugState.value.copy(lastAction = "SWIPE (${x1.toInt()}, ${y1.toInt()}) -> (${x2.toInt()}, ${y2.toInt()})")
                onActionDispatched(x1, y1, GestureType.SWIPE)
                delay(dur.coerceAtLeast(100L))
            }
            is ScrollStatement -> {
                _debugState.value = _debugState.value.copy(lastAction = "SCROLL ${stmt.direction}")
                onActionDispatched(540f, 1000f, GestureType.SWIPE)
                delay(300L)
            }
            is WaitStatement -> {
                val dur = eval(stmt.durationExpr).toLong()
                _debugState.value = _debugState.value.copy(lastAction = "WAIT ${dur}ms")
                delay(dur.coerceAtLeast(10L))
            }
            is PauseStatement -> {
                val dur = eval(stmt.durationExpr).toLong()
                _debugState.value = _debugState.value.copy(lastAction = "PAUSE ${dur}ms")
                delay(dur.coerceAtLeast(10L))
            }
            is StopStatement -> {
                _debugState.value = _debugState.value.copy(lastAction = "STOP")
                throw CancellationException("Script encountered STOP statement")
            }
            is VarDeclStatement -> {
                if (stmt.isConst) constants.add(stmt.name)
                variables[stmt.name] = eval(stmt.expr)
            }
            is VarAssignStatement -> {
                if (constants.contains(stmt.name)) {
                    throw IllegalStateException("Cannot reassign constant '${stmt.name}' at line ${stmt.line}")
                }
                variables[stmt.name] = eval(stmt.expr)
            }
            is RepeatStatement -> {
                val count = eval(stmt.countExpr).toInt().coerceAtLeast(0)
                for (r in 0 until count) {
                    if (isCancelled) break
                    executeStatementList(stmt.body, stepByStep)
                }
            }
            is ForStatement -> {
                val start = eval(stmt.startExpr).toInt()
                val end = eval(stmt.endExpr).toInt()
                for (v in start..end) {
                    if (isCancelled) break
                    variables[stmt.varName] = v.toDouble()
                    executeStatementList(stmt.body, stepByStep)
                }
            }
            is IfStatement -> {
                val conditionMet = eval(stmt.condition) != 0.0
                if (conditionMet) {
                    executeStatementList(stmt.thenBranch, stepByStep)
                } else if (stmt.elseBranch != null) {
                    executeStatementList(stmt.elseBranch, stepByStep)
                }
            }
            is GroupStatement -> {
                _debugState.value = _debugState.value.copy(lastAction = "GROUP: ${stmt.name}")
                executeStatementList(stmt.body, stepByStep)
            }
            is FunctionCallStatement -> {
                val body = functions[stmt.name] ?: throw IllegalStateException("Undefined function '${stmt.name}' called at line ${stmt.line}")
                executeStatementList(body, stepByStep)
            }
            is RunMacroStatement -> {
                _debugState.value = _debugState.value.copy(lastAction = "RUN_MACRO: ${stmt.macroId}")
                delay(300L)
            }
            is FunctionDefStatement -> {
                // Already registered
            }
        }
    }

    private fun eval(expr: DslExpression): Double {
        return when (expr) {
            is NumberLiteral -> expr.value
            is StringLiteral -> expr.value.toDoubleOrNull() ?: 0.0
            is VariableReference -> variables[expr.name] ?: 0.0
            is BinaryOp -> {
                val left = eval(expr.left)
                val right = eval(expr.right)
                when (expr.operator) {
                    TokenType.OP_PLUS -> left + right
                    TokenType.OP_MINUS -> left - right
                    TokenType.OP_EQ -> if (left == right) 1.0 else 0.0
                    TokenType.OP_NEQ -> if (left != right) 1.0 else 0.0
                    TokenType.OP_LT -> if (left < right) 1.0 else 0.0
                    TokenType.OP_LTE -> if (left <= right) 1.0 else 0.0
                    TokenType.OP_GT -> if (left > right) 1.0 else 0.0
                    TokenType.OP_GTE -> if (left >= right) 1.0 else 0.0
                    else -> 0.0
                }
            }
        }
    }
}
