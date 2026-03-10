package no.utgdev.getstrong.ui.summary

import no.utgdev.getstrong.domain.model.SessionSetType
import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryAccessibilityLabelTest {
    @Test
    fun buildsSummarySetContentDescription() {
        val description = buildSummarySetContentDescription(
            SummarySetRowUi(
                setOrder = 1,
                setType = SessionSetType.WARMUP,
                exerciseId = 1001L,
                exerciseName = "Deadlift",
                targetReps = 3,
                achievedReps = 3,
                loadKg = 80.0,
            ),
        )

        assertEquals(
            "Warmup set 2. Deadlift. Target 3 reps. Achieved 3 reps. Load 80 kg.",
            description,
        )
    }
}
