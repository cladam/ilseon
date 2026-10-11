package com.ilseon.data.routine

data class RoutineStepDraft(
    val title: String,
    val targetDurationMinutes: Int? = null
)

data class RoutineTemplate(
    val id: String,
    val title: String,
    val description: String,
    val steps: List<RoutineStepDraft>
)

object RoutineTemplates {
    val allTemplates: List<RoutineTemplate> = listOf(
        RoutineTemplate(
            id = "morning_launch",
            title = "Morning Launch",
            description = "Gentle baseline to ease into the day without overwhelm.",
            steps = listOf(
                RoutineStepDraft("Drink glass of water", 2),
                RoutineStepDraft("Take morning meds / vitamins", 2),
                RoutineStepDraft("Gentle stretch / movement", 5),
                RoutineStepDraft("Wash face / brush teeth", 4),
                RoutineStepDraft("Breakfast / Coffee", 15)
            )
        ),
        RoutineTemplate(
            id = "evening_wind_down",
            title = "Evening Wind-Down",
            description = "Quiet transition to disconnect and prepare for sleep.",
            steps = listOf(
                RoutineStepDraft("Clear desk / 2-minute reset", 5),
                RoutineStepDraft("Brush & floss teeth", 4),
                RoutineStepDraft("Plug in devices away from bed", 2),
                RoutineStepDraft("Write down tomorrow's top thought", 3),
                RoutineStepDraft("Wind-down reading / soothing sound", 15)
            )
        ),
        RoutineTemplate(
            id = "sensory_reset",
            title = "Sensory & Focus Reset",
            description = "Regulate when feeling overstimulated, scattered, or stuck.",
            steps = listOf(
                RoutineStepDraft("Step away & close eyes (deep breaths)", 3),
                RoutineStepDraft("Drink cool water", 2),
                RoutineStepDraft("Clear physical clutter around hands", 3),
                RoutineStepDraft("Brain dump raw thoughts on paper", 5)
            )
        ),
        RoutineTemplate(
            id = "quick_tidy",
            title = "Quick Tidy",
            description = "Rapid micro-reset before inertia takes over.",
            steps = listOf(
                RoutineStepDraft("Gather dishes & put in sink", 3),
                RoutineStepDraft("Wipe main surface", 2),
                RoutineStepDraft("Toss obvious trash & recycling", 3),
                RoutineStepDraft("Return 3 misplaced items to their home", 4)
            )
        )
    )

    /**
     * Parse bulk raw text into micro-steps.
     * Handles:
     * - Bullet points: "- ", "* ", "• ", "1. ", "1) "
     * - Duration hints: "(5m)", "(5 mins)", "[10 min]", "5m", "15 mins"
     */
    fun parseBulkText(text: String): List<RoutineStepDraft> {
        val durationRegex = Regex("""[(\[]?\b(\d{1,3})\s*(?:m|min|mins|minutes)\b[)\]]?""", RegexOption.IGNORE_CASE)
        val bulletRegex = Regex("""^(\s*[-*•]|\s*\d+[.)])\s*""")

        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { rawLine ->
                val lineWithoutBullet = rawLine.replace(bulletRegex, "").trim()
                val match = durationRegex.find(lineWithoutBullet)
                val duration = match?.groupValues?.getOrNull(1)?.toIntOrNull()
                val cleanTitle = if (match != null) {
                    lineWithoutBullet.removeRange(match.range).replace(Regex("""\s+"""), " ").trim(' ', '-', ',', ':', '(', ')')
                } else {
                    lineWithoutBullet
                }
                RoutineStepDraft(
                    title = cleanTitle.ifBlank { "Step" },
                    targetDurationMinutes = duration
                )
            }
            .filter { it.title.isNotBlank() }
            .toList()
    }
}
