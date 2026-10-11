package com.ilseon.data.routine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutineTemplateTest {

    @Test
    fun `parseBulkText parses clean lines without duration`() {
        val input = """
            Drink water
            Make coffee
            Stretch
        """.trimIndent()

        val steps = RoutineTemplates.parseBulkText(input)
        assertEquals(3, steps.size)
        assertEquals("Drink water", steps[0].title)
        assertNull(steps[0].targetDurationMinutes)
        assertEquals("Make coffee", steps[1].title)
        assertEquals("Stretch", steps[2].title)
    }

    @Test
    fun `parseBulkText strips bullets and numbers`() {
        val input = """
            - Drink water
            * Brush teeth
            • Take medication
            1. Wash face
            2) Pack bag
        """.trimIndent()

        val steps = RoutineTemplates.parseBulkText(input)
        assertEquals(5, steps.size)
        assertEquals("Drink water", steps[0].title)
        assertEquals("Brush teeth", steps[1].title)
        assertEquals("Take medication", steps[2].title)
        assertEquals("Wash face", steps[3].title)
        assertEquals("Pack bag", steps[4].title)
    }

    @Test
    fun `parseBulkText extracts minute durations in various formats`() {
        val input = """
            - Drink water (2m)
            * 5 min stretch
            • Brush teeth (3 mins)
            1. Shower [15 min]
            2) Read book 10 minutes
        """.trimIndent()

        val steps = RoutineTemplates.parseBulkText(input)
        assertEquals(5, steps.size)

        assertEquals("Drink water", steps[0].title)
        assertEquals(2, steps[0].targetDurationMinutes)

        assertEquals("stretch", steps[1].title)
        assertEquals(5, steps[1].targetDurationMinutes)

        assertEquals("Brush teeth", steps[2].title)
        assertEquals(3, steps[2].targetDurationMinutes)

        assertEquals("Shower", steps[3].title)
        assertEquals(15, steps[3].targetDurationMinutes)

        assertEquals("Read book", steps[4].title)
        assertEquals(10, steps[4].targetDurationMinutes)
    }

    @Test
    fun `starter templates are non-empty and well-formed`() {
        val templates = RoutineTemplates.allTemplates
        assertEquals(4, templates.size)

        templates.forEach { template ->
            assert(template.title.isNotBlank())
            assert(template.description.isNotBlank())
            assert(template.steps.isNotEmpty())
            template.steps.forEach { step ->
                assert(step.title.isNotBlank())
                assert((step.targetDurationMinutes ?: 0) >= 0)
            }
        }
    }
}
