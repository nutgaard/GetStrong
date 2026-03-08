package no.utgdev.getstrong.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSeedDataTest {
    @Test
    fun catalogContainsAtLeastFiftyExercisesWithDeterministicIds() {
        val exercises = ExerciseSeedData.exercises
        assertTrue(exercises.size >= 50)
        assertEquals(exercises.size, exercises.map { it.id }.distinct().size)
    }

    @Test
    fun catalogEntriesHaveRequiredFields() {
        ExerciseSeedData.exercises.forEach { exercise ->
            assertTrue(exercise.id > 0)
            assertTrue(exercise.name.isNotBlank())
            assertTrue(exercise.primaryMuscleGroup.isNotBlank())
            assertTrue(exercise.equipmentType.isNotBlank())
        }
    }
}
