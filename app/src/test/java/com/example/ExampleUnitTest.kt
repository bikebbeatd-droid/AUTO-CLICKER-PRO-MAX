package com.example

import com.example.data.model.ClickMode
import com.example.data.model.ClickTarget
import com.example.data.model.FloatingThemeConfig
import com.example.data.model.GestureType
import com.example.data.model.MacroAction
import com.example.data.model.SequenceBlock
import com.example.data.model.SequenceBlockType
import com.example.data.repository.MarketplaceRepository
import com.example.dsl.AiScriptAssistant
import com.example.dsl.DslParser
import com.example.dsl.TapStatement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testClickTargetDefaultValues() {
        val target = ClickTarget(number = 1, x = 500f, y = 800f)
        assertEquals(1, target.number)
        assertEquals(500f, target.x)
        assertEquals(800f, target.y)
        assertEquals(100L, target.intervalMs)
        assertTrue(target.enabled)
    }

    @Test
    fun testMacroActionOrdering() {
        val actions = listOf(
            MacroAction(orderIndex = 2, type = GestureType.SWIPE),
            MacroAction(orderIndex = 0, type = GestureType.TAP),
            MacroAction(orderIndex = 1, type = GestureType.DOUBLE_TAP)
        )
        val sorted = actions.sortedBy { it.orderIndex }
        assertEquals(GestureType.TAP, sorted[0].type)
        assertEquals(GestureType.DOUBLE_TAP, sorted[1].type)
        assertEquals(GestureType.SWIPE, sorted[2].type)
    }

    @Test
    fun testSequenceBlockTypes() {
        val block = SequenceBlock(type = SequenceBlockType.TAP, delayMs = 250L)
        assertEquals(SequenceBlockType.TAP, block.type)
        assertEquals(250L, block.delayMs)
        assertTrue(block.isEnabled)
    }

    @Test
    fun testFloatingThemeDefaults() {
        val theme = FloatingThemeConfig()
        assertEquals("GRADIENT", theme.bgType)
        assertEquals("Neon Cyber", theme.gradientName)
        assertEquals("#FFFFFF", theme.iconColorHex)
        assertFalse(theme.isLocked)
        assertTrue(theme.showClickCounter)
    }

    @Test
    fun testClickModeDisplayNames() {
        assertEquals("Single Target", ClickMode.SINGLE_TAP.displayName)
        assertEquals("Multi-Target", ClickMode.MULTI_TAP.displayName)
        assertEquals("Auto Swipe", ClickMode.AUTO_SWIPE.displayName)
    }

    @Test
    fun testDslParserTokenizationAndValidation() {
        val parser = DslParser()
        val script = """
            VAR x = 500
            VAR y = 800
            REPEAT 5 {
                TAP x y 40
                WAIT 100
            }
        """.trimIndent()
        val (stmts, errors) = parser.parse(script)
        assertTrue("Parser should have zero errors on valid code", errors.isEmpty())
        assertTrue("Statements should not be empty", stmts.isNotEmpty())
    }

    @Test
    fun testDslParserAutoFormat() {
        val parser = DslParser()
        val raw = "REPEAT 3 {\nTAP 100 200\nWAIT 50\n}"
        val formatted = parser.format(raw)
        assertTrue(formatted.contains("    TAP 100 200"))
        assertTrue(formatted.contains("    WAIT 50"))
    }

    @Test
    fun testAiAssistantPromptSynthesis() {
        val assistant = AiScriptAssistant()
        val result = assistant.generateScriptFromPrompt("Tap center 50 times with 100ms interval")
        assertNotNull(result)
        assertTrue(result.dslCode.contains("REPEAT"))
        assertTrue(result.dslCode.contains("TAP"))
        assertTrue(result.actionPlan.isNotEmpty())
    }

    @Test
    fun testMarketplacePreloadAndSearch() {
        val repo = MarketplaceRepository()
        val results = repo.searchScripts("Turbo", "All")
        assertTrue("Should find turbo script in marketplace", results.isNotEmpty())
        val validationResult = repo.validateAndImportScript(results.first())
        assertTrue("Marketplace template should pass validation", validationResult.isSuccess)
    }
}
