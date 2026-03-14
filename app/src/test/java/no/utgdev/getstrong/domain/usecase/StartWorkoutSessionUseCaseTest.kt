package no.utgdev.getstrong.domain.usecase

import kotlinx.coroutines.test.runTest
import no.utgdev.getstrong.domain.model.Exercise
import no.utgdev.getstrong.domain.model.EquipmentTypeCode
import no.utgdev.getstrong.domain.model.MuscleGroupCode
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.model.SetResult
import no.utgdev.getstrong.domain.model.SlotProgressionUpdate
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import no.utgdev.getstrong.domain.model.WorkoutSession
import no.utgdev.getstrong.domain.repository.ExerciseRepository
import no.utgdev.getstrong.domain.repository.SessionRepository
import no.utgdev.getstrong.domain.repository.WorkoutRepository
import no.utgdev.getstrong.domain.session.WorkoutSessionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StartWorkoutSessionUseCaseTest {
    @Test
    fun reusesExistingUnfinishedSessionBeforeCreatingNewOne() = runTest {
        val sessionRepository = CapturingSessionRepository(existingUnfinishedSessionId = 77L)
        val workoutRepository = FakeWorkoutRepositoryForStart(
            Workout(
                id = 1,
                name = "A",
                slots = emptyList(),
            ),
        )
        val useCase = StartWorkoutSessionUseCase(
            workoutRepository = workoutRepository,
            exerciseRepository = FakeExerciseRepository(emptyList()),
            sessionRepository = sessionRepository,
            sessionEngine = WorkoutSessionEngine(WarmupGenerator()),
        )

        val sessionId = useCase(1L)

        assertEquals(77L, sessionId)
        assertEquals(null, sessionRepository.startedWorkoutId)
    }

    @Test
    fun createsSnapshotWithWarmupsBeforeWorkAndPersistsSamePlan() = runTest {
        val workout = Workout(
            id = 1,
            name = "A",
            slots = listOf(
                WorkoutExerciseSlot(
                    id = 11,
                    workoutId = 1,
                    exerciseId = 1006,
                    position = 0,
                    targetSets = 2,
                    targetReps = 5,
                    repRangeMin = 5,
                    repRangeMax = 5,
                    progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                    incrementKg = 2.5,
                    deloadPercent = 10,
                    currentWorkingWeightKg = 140.0,
                    failureStreak = 0,
                    restSecondsOverride = null,
                ),
            ),
        )
        val workoutRepository = FakeWorkoutRepositoryForStart(workout)
        val exerciseRepository = FakeExerciseRepository(
            listOf(
                Exercise(
                    id = 1006,
                    name = "Deadlift",
                    primaryMuscleGroup = MuscleGroupCode.BACK,
                    secondaryMuscleGroups = emptyList(),
                    equipmentType = EquipmentTypeCode.BARBELL,
                ),
            ),
        )
        val sessionRepository = CapturingSessionRepository()
        val useCase = StartWorkoutSessionUseCase(
            workoutRepository = workoutRepository,
            exerciseRepository = exerciseRepository,
            sessionRepository = sessionRepository,
            sessionEngine = WorkoutSessionEngine(WarmupGenerator()),
        )

        val sessionId = useCase(1L)

        assertNotNull(sessionId)
        val plan = sessionRepository.lastPlannedSets
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), plan.map { it.setOrder })
        assertEquals(listOf(60.0, 80.0, 100.0, 120.0, 130.0), plan.take(5).map { it.targetWeightKg })
        assertEquals(
            listOf(
                SessionSetType.WARMUP,
                SessionSetType.WARMUP,
                SessionSetType.WARMUP,
                SessionSetType.WARMUP,
                SessionSetType.WARMUP,
                SessionSetType.WORK,
                SessionSetType.WORK,
            ),
            plan.map { it.setType },
        )
    }
}

private class FakeWorkoutRepositoryForStart(
    private val workout: Workout,
) : WorkoutRepository {
    override suspend fun createWorkout(workout: Workout): Long = workout.id
    override suspend fun updateWorkout(workout: Workout) = Unit
    override suspend fun deleteWorkout(workoutId: Long) = Unit
    override suspend fun getWorkout(workoutId: Long): Workout? = workout.takeIf { it.id == workoutId }
    override suspend fun getAllWorkouts(): List<Workout> = listOf(workout)
}

private class FakeExerciseRepository(
    private val exercises: List<Exercise>,
) : ExerciseRepository {
    override suspend fun save(exercise: Exercise): Long = exercise.id
    override suspend fun getById(exerciseId: Long): Exercise? = exercises.firstOrNull { it.id == exerciseId }
    override suspend fun getAll(): List<Exercise> = exercises
    override suspend fun delete(exerciseId: Long) = Unit
}

private class CapturingSessionRepository : SessionRepository {
    constructor(existingUnfinishedSessionId: Long? = null) {
        this.existingUnfinishedSessionId = existingUnfinishedSessionId
    }

    var lastPlannedSets: List<SessionPlannedSet> = emptyList()
    var startedWorkoutId: Long? = null
    private var existingUnfinishedSessionId: Long? = null

    override suspend fun startSession(workoutId: Long, plannedSets: List<SessionPlannedSet>): Long {
        startedWorkoutId = workoutId
        lastPlannedSets = plannedSets
        return 99L
    }

    override suspend fun findUnfinishedSessionId(): Long? = existingUnfinishedSessionId

    override suspend fun discardSessionIfNoProgress(sessionId: Long): Boolean = false

    override suspend fun getActiveSessionState(sessionId: Long) = null
    override suspend fun completePlannedSet(sessionId: Long, plannedSetId: Long, repsAchieved: Int) = null
    override suspend fun updatePlannedSetWeight(sessionId: Long, plannedSetId: Long, weightKg: Double) = null
    override suspend fun addExtraSet(sessionId: Long, anchorPlannedSetId: Long) = null
    override suspend fun removeExtraSet(sessionId: Long, plannedSetId: Long) = null
    override suspend fun completeSession(sessionId: Long) = Unit
    override suspend fun completeSessionWithProgression(sessionId: Long, updates: List<SlotProgressionUpdate>) = Unit
    override suspend fun saveSession(session: WorkoutSession): Long = session.id
    override suspend fun saveSetResult(result: SetResult): Long = result.id
    override suspend fun getSetResults(sessionId: Long): List<SetResult> = emptyList()
}
