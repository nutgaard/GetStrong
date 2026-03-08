package no.utgdev.getstrong.data.repository

import kotlinx.coroutines.test.runTest
import no.utgdev.getstrong.data.local.dao.WorkoutDao
import no.utgdev.getstrong.data.local.entity.WorkoutEntity
import no.utgdev.getstrong.data.local.entity.WorkoutExerciseSlotEntity
import no.utgdev.getstrong.data.local.entity.WorkoutWithSlotsEntity
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class WorkoutRepositoryImplTest {
    @Test
    fun createAndLoadWorkoutPreservesOrderedSlotsAndConfig() = runTest {
        val dao = FakeWorkoutDao()
        val repository = WorkoutRepositoryImpl(dao)

        val workoutId = repository.createWorkout(
            Workout(
                name = "Push",
                slots = listOf(
                    WorkoutExerciseSlot(
                        exerciseId = 1015,
                        workoutId = 0,
                        position = 1,
                        targetSets = 3,
                        targetReps = 8,
                        repRangeMin = 6,
                        repRangeMax = 10,
                        progressionMode = ProgressionModeCode.REPS_THEN_WEIGHT,
                        incrementKg = 2.5,
                        deloadPercent = 10,
                        restSecondsOverride = 120,
                    ),
                    WorkoutExerciseSlot(
                        exerciseId = 1006,
                        workoutId = 0,
                        position = 0,
                        targetSets = 5,
                        targetReps = 5,
                        repRangeMin = 5,
                        repRangeMax = 5,
                        progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                        incrementKg = 2.5,
                        deloadPercent = 10,
                        restSecondsOverride = null,
                    ),
                ),
            ),
        )

        val loaded = repository.getWorkout(workoutId)
        assertNotNull(loaded)
        assertEquals("Push", loaded?.name)
        assertEquals(2, loaded?.slots?.size)

        val first = loaded!!.slots[0]
        val second = loaded.slots[1]
        assertEquals(0, first.position)
        assertEquals(1006, first.exerciseId)
        assertEquals(5, first.targetSets)
        assertEquals(5, first.repRangeMin)
        assertEquals(5, first.repRangeMax)
        assertEquals(ProgressionModeCode.WEIGHT_ONLY, first.progressionMode)

        assertEquals(1, second.position)
        assertEquals(1015, second.exerciseId)
        assertEquals(3, second.targetSets)
        assertEquals(6, second.repRangeMin)
        assertEquals(10, second.repRangeMax)
        assertEquals(120, second.restSecondsOverride)
    }

    @Test
    fun updateReorderAndDeleteArePersistedAndIsolated() = runTest {
        val dao = FakeWorkoutDao()
        val repository = WorkoutRepositoryImpl(dao)

        val firstWorkoutId = repository.createWorkout(
            Workout(
                name = "Workout A",
                slots = listOf(
                    WorkoutExerciseSlot(
                        exerciseId = 1001,
                        workoutId = 0,
                        position = 0,
                        targetSets = 5,
                        targetReps = 5,
                        repRangeMin = 5,
                        repRangeMax = 5,
                        progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                        incrementKg = 2.5,
                        deloadPercent = 10,
                        restSecondsOverride = null,
                    ),
                    WorkoutExerciseSlot(
                        exerciseId = 1002,
                        workoutId = 0,
                        position = 1,
                        targetSets = 3,
                        targetReps = 8,
                        repRangeMin = 6,
                        repRangeMax = 10,
                        progressionMode = ProgressionModeCode.REPS_ONLY,
                        incrementKg = 1.0,
                        deloadPercent = 8,
                        restSecondsOverride = 90,
                    ),
                ),
            ),
        )

        val secondWorkoutId = repository.createWorkout(
            Workout(
                name = "Workout B",
                slots = listOf(
                    WorkoutExerciseSlot(
                        exerciseId = 1003,
                        workoutId = 0,
                        position = 0,
                        targetSets = 4,
                        targetReps = 6,
                        repRangeMin = 6,
                        repRangeMax = 8,
                        progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                        incrementKg = 2.5,
                        deloadPercent = 10,
                        restSecondsOverride = null,
                    ),
                ),
            ),
        )

        val loadedFirstBeforeUpdate = repository.getWorkout(firstWorkoutId)!!
        repository.updateWorkout(
            loadedFirstBeforeUpdate.copy(
                name = "Workout A Renamed",
                slots = loadedFirstBeforeUpdate.slots.reversed().mapIndexed { idx, slot ->
                    slot.copy(position = idx)
                },
            ),
        )

        val updatedFirst = repository.getWorkout(firstWorkoutId)!!
        assertEquals("Workout A Renamed", updatedFirst.name)
        assertEquals(1002, updatedFirst.slots[0].exerciseId)
        assertEquals(1001, updatedFirst.slots[1].exerciseId)

        repository.deleteWorkout(firstWorkoutId)
        assertNull(repository.getWorkout(firstWorkoutId))
        assertNotNull(repository.getWorkout(secondWorkoutId))
        assertEquals(1, repository.getWorkout(secondWorkoutId)?.slots?.size)
    }
}

private class FakeWorkoutDao : WorkoutDao {
    private var nextWorkoutId = 1L
    private var nextSlotId = 1L
    private val workouts = linkedMapOf<Long, WorkoutEntity>()
    private val slotsByWorkout = linkedMapOf<Long, MutableList<WorkoutExerciseSlotEntity>>()

    override suspend fun upsertWorkout(workout: WorkoutEntity): Long {
        val id = if (workout.id == 0L) nextWorkoutId++ else workout.id
        workouts[id] = workout.copy(id = id)
        return id
    }

    override suspend fun upsertSlots(slots: List<WorkoutExerciseSlotEntity>) {
        slots.groupBy { it.workoutId }.forEach { (workoutId, groupedSlots) ->
            val current = slotsByWorkout.getOrPut(workoutId) { mutableListOf() }
            groupedSlots.forEach { slot ->
                val normalized = if (slot.id == 0L) slot.copy(id = nextSlotId++) else slot
                val index = current.indexOfFirst { it.id == normalized.id }
                if (index >= 0) {
                    current[index] = normalized
                } else {
                    current += normalized
                }
            }
        }
    }

    override suspend fun deleteSlotsForWorkout(workoutId: Long) {
        slotsByWorkout.remove(workoutId)
    }

    override suspend fun deleteSlotsForWorkoutExcept(workoutId: Long, slotIds: List<Long>) {
        val current = slotsByWorkout[workoutId] ?: return
        slotsByWorkout[workoutId] = current.filter { it.id in slotIds }.toMutableList()
    }

    override suspend fun deleteWorkout(workoutId: Long) {
        workouts.remove(workoutId)
        slotsByWorkout.remove(workoutId)
    }

    override suspend fun saveWorkoutWithSlots(
        workout: WorkoutEntity,
        slots: List<WorkoutExerciseSlotEntity>,
    ): Long {
        val workoutId = upsertWorkout(workout)
        val normalized = slots.map { it.copy(workoutId = workoutId) }
        val keepIds = normalized.mapNotNull { it.id.takeIf { id -> id > 0 } }
        if (keepIds.isEmpty()) {
            deleteSlotsForWorkout(workoutId)
        } else {
            deleteSlotsForWorkoutExcept(workoutId, keepIds)
        }
        upsertSlots(normalized)
        return workoutId
    }

    override suspend fun getWorkoutWithSlots(workoutId: Long): WorkoutWithSlotsEntity? {
        val workout = workouts[workoutId] ?: return null
        val slots = slotsByWorkout[workoutId].orEmpty()
        return WorkoutWithSlotsEntity(workout = workout, slots = slots)
    }

    override suspend fun getAllWorkoutsWithSlots(): List<WorkoutWithSlotsEntity> {
        return workouts.values.map { workout ->
            WorkoutWithSlotsEntity(
                workout = workout,
                slots = slotsByWorkout[workout.id].orEmpty(),
            )
        }
    }
}
