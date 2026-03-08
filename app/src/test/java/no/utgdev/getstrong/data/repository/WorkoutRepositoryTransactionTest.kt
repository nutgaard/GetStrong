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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class WorkoutRepositoryTransactionTest {
    @Test
    fun saveWorkoutCommitsAllEntitiesOnSuccess() = runTest {
        val dao = TransactionalFakeWorkoutDao()
        val repository = WorkoutRepositoryImpl(dao)

        val workoutId = repository.createWorkout(
            Workout(
                name = "A",
                slots = listOf(
                    slot(exerciseId = 1001, position = 0),
                    slot(exerciseId = 1002, position = 1),
                ),
            ),
        )

        val loaded = repository.getWorkout(workoutId)
        assertNotNull(loaded)
        assertEquals("A", loaded!!.name)
        assertEquals(2, loaded.slots.size)
        assertEquals(listOf(1001L, 1002L), loaded.slots.map { it.exerciseId })
    }

    @Test
    fun saveWorkoutRollsBackOnMidWriteFailure() = runTest {
        val dao = TransactionalFakeWorkoutDao()
        val repository = WorkoutRepositoryImpl(dao)

        val workoutId = repository.createWorkout(
            Workout(
                name = "Original",
                slots = listOf(
                    slot(exerciseId = 1001, position = 0),
                    slot(exerciseId = 1002, position = 1),
                ),
            ),
        )
        val before = repository.getWorkout(workoutId)!!
        assertEquals(2, before.slots.size)

        dao.failAfterDeleteBeforeInsert = true
        try {
            repository.updateWorkout(
                before.copy(
                    name = "Broken Update",
                    slots = listOf(slot(id = 0, exerciseId = 1003, position = 0)),
                ),
            )
            fail("Expected failure from injected hook")
        } catch (_: IllegalStateException) {
        }

        val after = repository.getWorkout(workoutId)
        assertNotNull(after)
        assertEquals("Original", after!!.name)
        assertEquals(2, after.slots.size)
        assertEquals(listOf(1001L, 1002L), after.slots.map { it.exerciseId })
    }

    @Test
    fun deleteWorkoutRemovesWorkoutAndSlots() = runTest {
        val dao = TransactionalFakeWorkoutDao()
        val repository = WorkoutRepositoryImpl(dao)

        val workoutId = repository.createWorkout(
            Workout(
                name = "Delete Me",
                slots = listOf(slot(exerciseId = 1001, position = 0)),
            ),
        )
        assertNotNull(repository.getWorkout(workoutId))

        repository.deleteWorkout(workoutId)

        assertNull(repository.getWorkout(workoutId))
    }

    private fun slot(
        id: Long = 0,
        exerciseId: Long,
        position: Int,
    ): WorkoutExerciseSlot =
        WorkoutExerciseSlot(
            id = id,
            workoutId = 0,
            exerciseId = exerciseId,
            position = position,
            targetSets = 5,
            targetReps = 5,
            repRangeMin = 5,
            repRangeMax = 5,
            progressionMode = ProgressionModeCode.WEIGHT_ONLY,
            incrementKg = 2.5,
            deloadPercent = 10,
            currentWorkingWeightKg = 100.0,
            failureStreak = 0,
            restSecondsOverride = null,
        )
}

private class TransactionalFakeWorkoutDao : WorkoutDao {
    private var nextWorkoutId = 1L
    private var nextSlotId = 1L
    val workouts = linkedMapOf<Long, WorkoutEntity>()
    val slotsByWorkout = linkedMapOf<Long, MutableList<WorkoutExerciseSlotEntity>>()
    var failAfterDeleteBeforeInsert: Boolean = false

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
                if (index >= 0) current[index] = normalized else current += normalized
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
        val workoutsSnapshot = linkedMapOf<Long, WorkoutEntity>().apply { putAll(workouts) }
        val slotsSnapshot = linkedMapOf<Long, MutableList<WorkoutExerciseSlotEntity>>().apply {
            putAll(slotsByWorkout.mapValues { it.value.toMutableList() })
        }
        val nextWorkoutSnapshot = nextWorkoutId
        val nextSlotSnapshot = nextSlotId

        return try {
            val workoutId = upsertWorkout(workout)
            if (slots.isEmpty()) {
                deleteSlotsForWorkout(workoutId)
                return workoutId
            }

            val normalized = slots.map { it.copy(workoutId = workoutId) }
            val keepIds = normalized.mapNotNull { it.id.takeIf { id -> id > 0 } }
            if (keepIds.isEmpty()) deleteSlotsForWorkout(workoutId) else deleteSlotsForWorkoutExcept(workoutId, keepIds)

            if (failAfterDeleteBeforeInsert) {
                failAfterDeleteBeforeInsert = false
                throw IllegalStateException("Injected failure after delete")
            }

            upsertSlots(normalized)
            workoutId
        } catch (t: Throwable) {
            workouts.clear()
            workouts.putAll(workoutsSnapshot)
            slotsByWorkout.clear()
            slotsByWorkout.putAll(slotsSnapshot)
            nextWorkoutId = nextWorkoutSnapshot
            nextSlotId = nextSlotSnapshot
            throw t
        }
    }

    override suspend fun getWorkoutWithSlots(workoutId: Long): WorkoutWithSlotsEntity? {
        val workout = workouts[workoutId] ?: return null
        return WorkoutWithSlotsEntity(workout = workout, slots = slotsByWorkout[workoutId].orEmpty())
    }

    override suspend fun getAllWorkoutsWithSlots(): List<WorkoutWithSlotsEntity> =
        workouts.values.map { workout ->
            WorkoutWithSlotsEntity(workout = workout, slots = slotsByWorkout[workout.id].orEmpty())
        }
}
