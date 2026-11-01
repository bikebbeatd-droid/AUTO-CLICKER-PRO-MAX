package com.example.dsl

data class AiGeneratedScript(
    val title: String,
    val dslCode: String,
    val explanation: String,
    val actionPlan: List<String>,
    val estimatedDurationMs: Long,
    val warnings: List<String> = emptyList()
)

class AiScriptAssistant {

    fun generateScriptFromPrompt(prompt: String): AiGeneratedScript {
        val lower = prompt.trim().lowercase()

        // 1. "tap ... times" or "clicks"
        val tapCountRegex = Regex("""(\d+)\s*(?:times|clicks|taps)""")
        val delayRegex = Regex("""(\d+)\s*(?:seconds?|s|sec|millis?|ms)""")

        val countMatch = tapCountRegex.find(lower)
        val delayMatch = delayRegex.find(lower)

        val count = countMatch?.groupValues?.get(1)?.toIntOrNull() ?: when {
            "once" in lower -> 1
            "twice" in lower -> 2
            "hundred" in lower -> 100
            "100" in lower -> 100
            "50" in lower -> 50
            "10" in lower -> 10
            "5" in lower -> 5
            else -> 10
        }

        var delayMs = 200L
        if (delayMatch != null) {
            val num = delayMatch.groupValues[1].toLongOrNull() ?: 1L
            delayMs = if ("sec" in delayMatch.value || "s" in delayMatch.value) num * 1000L else num
        }

        return when {
            "scroll" in lower -> {
                val dir = if ("up" in lower) "UP" else "DOWN"
                val code = """
# Auto-generated Scroll Automation
# Command: $prompt
VAR repeat_count = $count
VAR scroll_delay = $delayMs

REPEAT repeat_count {
    SCROLL $dir
    WAIT scroll_delay
}
""".trimIndent()
                AiGeneratedScript(
                    title = "Smooth Continuous Scroll ($dir)",
                    dslCode = code,
                    explanation = "Executes $count automated $dir scrolls with ${delayMs}ms delay between scrolls.",
                    actionPlan = listOf(
                        "Set loop counter to $count",
                        "Dispatch scroll gesture in direction $dir",
                        "Wait for ${delayMs}ms before next cycle"
                    ),
                    estimatedDurationMs = count * (delayMs + 300L)
                )
            }
            "swipe" in lower -> {
                val isDown = "down" in lower
                val isUp = "up" in lower || (!isDown && "down" !in lower)
                val y1 = if (isDown) 400 else 1400
                val y2 = if (isDown) 1400 else 400
                val code = """
# Auto-generated Swipe Automation
# Command: $prompt
VAR swipe_count = $count
VAR interval = $delayMs

REPEAT swipe_count {
    SWIPE 540 $y1 540 $y2 350
    WAIT interval
}
""".trimIndent()
                AiGeneratedScript(
                    title = "Automated Swipe Sequence",
                    dslCode = code,
                    explanation = "Executes $count vertical swipes ($y1 -> $y2) with ${delayMs}ms wait time.",
                    actionPlan = listOf(
                        "Initialize iteration count ($count loops)",
                        "Perform linear swipe from y=$y1 to y=$y2 (350ms duration)",
                        "Wait ${delayMs}ms buffer between gestures"
                    ),
                    estimatedDurationMs = count * (delayMs + 350L)
                )
            }
            "double" in lower && "tap" in lower -> {
                val code = """
# Auto-generated Double Tap Automation
# Command: $prompt
VAR target_x = 540
VAR target_y = 960
VAR loops = $count
VAR cooldown = $delayMs

REPEAT loops {
    DOUBLE_TAP target_x target_y
    WAIT cooldown
}
""".trimIndent()
                AiGeneratedScript(
                    title = "Double Tap Sequence",
                    dslCode = code,
                    explanation = "Repeats rapid double-tap gestures at center coordinates (540, 960) for $count cycles.",
                    actionPlan = listOf(
                        "Dispatch double-tap event with 60ms inter-tap delay",
                        "Wait ${delayMs}ms before repeating",
                        "Repeat for $count total cycles"
                    ),
                    estimatedDurationMs = count * (delayMs + 160L)
                )
            }
            "long press" in lower || "hold" in lower -> {
                val holdDuration = 1200L
                val code = """
# Auto-generated Long Press Automation
# Command: $prompt
VAR target_x = 540
VAR target_y = 960
VAR hold_duration = $holdDuration
VAR repeats = $count

REPEAT repeats {
    LONG_PRESS target_x target_y hold_duration
    WAIT $delayMs
}
""".trimIndent()
                AiGeneratedScript(
                    title = "Long Press Routine",
                    dslCode = code,
                    explanation = "Performs continuous $holdDuration ms sustained presses at (540, 960) repeated $count times.",
                    actionPlan = listOf(
                        "Dispatch sustained touch down at (540, 960)",
                        "Hold touch for $holdDuration ms",
                        "Release and wait ${delayMs}ms"
                    ),
                    estimatedDurationMs = count * (holdDuration + delayMs)
                )
            }
            else -> {
                // Default rapid tap automation
                val code = """
# Auto-generated Tap Automation
# Command: $prompt
VAR click_x = 540
VAR click_y = 960
VAR total_clicks = $count
VAR interval_ms = $delayMs

REPEAT total_clicks {
    TAP click_x click_y 40
    WAIT interval_ms
}
""".trimIndent()
                AiGeneratedScript(
                    title = "High-Speed Tap Script",
                    dslCode = code,
                    explanation = "Dispatches $count sequential taps at coordinates (540, 960) with ${delayMs}ms delay between clicks.",
                    actionPlan = listOf(
                        "Target center point (540, 960)",
                        "Execute tap with 40ms touch contact time",
                        "Delay ${delayMs}ms between taps",
                        "Loop $count times then stop automatically"
                    ),
                    estimatedDurationMs = count * (delayMs + 40L)
                )
            }
        }
    }
}
