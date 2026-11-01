package com.example.data.repository

import com.example.dsl.DslParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

data class MarketplaceScript(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val author: String,
    val version: String,
    val rating: Float,
    val reviewCount: Int,
    val downloads: Int,
    val isVerified: Boolean,
    val tags: List<String>,
    val dslCode: String
)

class MarketplaceRepository {
    private val parser = DslParser()

    private val presetListings = listOf(
        MarketplaceScript(
            id = "mkt_1",
            title = "Fast Turbo Clicker 50 CPS",
            description = "High-speed sequential tapping with anti-detection micro-jitter.",
            category = "Auto Tap",
            author = "ProAuto Team",
            version = "2.1.0",
            rating = 4.9f,
            reviewCount = 1420,
            downloads = 48200,
            isVerified = true,
            tags = listOf("Turbo", "Tapping", "High Speed"),
            dslCode = """
# Fast Turbo Clicker
VAR target_x = 540
VAR target_y = 960
VAR count = 100

REPEAT count {
    TAP target_x target_y 20
    WAIT 20
}
""".trimIndent()
        ),
        MarketplaceScript(
            id = "mkt_2",
            title = "Infinite Social Feed Auto-Scroller",
            description = "Smooth upward swipe simulation ideal for reading news and feeds hands-free.",
            category = "Auto Scroll",
            author = "DevAutomation",
            version = "1.4.0",
            rating = 4.8f,
            reviewCount = 890,
            downloads = 32100,
            isVerified = true,
            tags = listOf("Scroll", "Reading", "Feed"),
            dslCode = """
# Infinite Social Feed Scroller
VAR loops = 50

REPEAT loops {
    SWIPE 540 1400 540 500 400
    WAIT 1200
}
""".trimIndent()
        ),
        MarketplaceScript(
            id = "mkt_3",
            title = "Triangle Multi-Point Farmer",
            description = "Cycles sequentially between three strategic points on the display.",
            category = "Multi Click",
            author = "GamerStudio",
            version = "3.0.1",
            rating = 4.7f,
            reviewCount = 630,
            downloads = 21500,
            isVerified = true,
            tags = listOf("Multi-Target", "Points", "Combo"),
            dslCode = """
# Triangle Multi-Point Sequence
VAR repetitions = 20

REPEAT repetitions {
    TAP 340 700 40
    WAIT 150
    TAP 740 700 40
    WAIT 150
    TAP 540 1200 40
    WAIT 300
}
""".trimIndent()
        ),
        MarketplaceScript(
            id = "mkt_4",
            title = "Long Press Recharge Loop",
            description = "Holds down continuous touch for charge attacks or mining buttons.",
            category = "Repetitive Tasks",
            author = "TacticalBot",
            version = "1.0.2",
            rating = 4.6f,
            reviewCount = 310,
            downloads = 12400,
            isVerified = true,
            tags = listOf("Long Press", "Hold", "Recharge"),
            dslCode = """
# Long Press Recharge Loop
VAR cycles = 15

REPEAT cycles {
    LONG_PRESS 540 960 1500
    WAIT 500
}
""".trimIndent()
        ),
        MarketplaceScript(
            id = "mkt_5",
            title = "Horizontal Card Deck Swiper",
            description = "Automates leftward and rightward card swipes with randomized delays.",
            category = "Swipe Automation",
            author = "SpeedFlow",
            version = "1.2.0",
            rating = 4.8f,
            reviewCount = 450,
            downloads = 16700,
            isVerified = true,
            tags = listOf("Swipe", "Cards", "Gesture"),
            dslCode = """
# Card Deck Swiper
VAR deck_size = 30

REPEAT deck_size {
    SWIPE 850 1000 200 1000 250
    WAIT 600
}
""".trimIndent()
        ),
        MarketplaceScript(
            id = "mkt_6",
            title = "Double Tap Like Streak",
            description = "Executes double-tap heart gesture in rapid succession.",
            category = "Productivity",
            author = "MediaCraft",
            version = "2.0.0",
            rating = 4.9f,
            reviewCount = 1120,
            downloads = 39800,
            isVerified = true,
            tags = listOf("Double Tap", "Heart", "Like"),
            dslCode = """
# Double Tap Like Streak
VAR total = 40

REPEAT total {
    DOUBLE_TAP 540 960
    WAIT 800
}
""".trimIndent()
        )
    )

    private val _marketplaceScripts = MutableStateFlow(presetListings)
    val marketplaceScripts = _marketplaceScripts.asStateFlow()

    fun searchScripts(query: String, category: String): List<MarketplaceScript> {
        val q = query.trim().lowercase()
        return _marketplaceScripts.value.filter { script ->
            val matchesQuery = q.isEmpty() || script.title.lowercase().contains(q) || script.description.lowercase().contains(q) || script.tags.any { it.lowercase().contains(q) }
            val matchesCategory = category == "All" || script.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }

    fun validateAndImportScript(script: MarketplaceScript): Result<String> {
        val (_, errors) = parser.parse(script.dslCode)
        return if (errors.isEmpty()) {
            Result.success(script.dslCode)
        } else {
            Result.failure(IllegalArgumentException("Script validation failed: ${errors.first().message}"))
        }
    }
}
