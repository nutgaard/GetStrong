package no.utgdev.getstrong.data.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import no.utgdev.getstrong.data.local.db.GetStrongDatabase
import no.utgdev.getstrong.data.local.entity.ExerciseEntity
import no.utgdev.getstrong.data.repository.SessionRepositoryImpl
import no.utgdev.getstrong.data.repository.SessionSummaryRepositoryImpl
import no.utgdev.getstrong.data.repository.WorkoutRepositoryImpl
import no.utgdev.getstrong.data.repository.WorkoutSummaryRepositoryImpl
import no.utgdev.getstrong.domain.model.ProgressionModeCode
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType
import no.utgdev.getstrong.domain.model.Workout
import no.utgdev.getstrong.domain.model.WorkoutExerciseSlot
import no.utgdev.getstrong.domain.usecase.CompleteSessionAndSaveSummaryUseCase
import no.utgdev.getstrong.domain.usecase.CompleteSessionWithProgressionUseCase
import no.utgdev.getstrong.domain.usecase.ElapsedTimeCalculator
import no.utgdev.getstrong.domain.usecase.ProgressionCalculator
import no.utgdev.getstrong.domain.usecase.SessionSummaryCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionCompletionPersistenceIntegrationTest {
    @Test
    fun completionPersistsWorkoutSummaryAndSurvivesReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "t18-${System.nanoTime()}.db"
        val database = openDatabase(context, dbName)

        val sessionId: Long
        val expectedWorkoutId: Long
        try {
            seedExercise(database)
            val workoutRepository = WorkoutRepositoryImpl(database.workoutDao())
            val sessionRepository = SessionRepositoryImpl(database.sessionDao())
            expectedWorkoutId = createWorkout(workoutRepository)
            sessionId = createCompletedSession(sessionRepository, workoutRepository, expectedWorkoutId)

            val useCase = buildCompleteUseCase(database, sessionRepository, workoutRepository)
            useCase(sessionId)

            val summary = database.workoutSummaryDao().getSummaryBySessionId(sessionId)
            assertNotNull(summary)
            assertEquals(expectedWorkoutId, summary!!.workoutId)
            assertEquals(sessionId, summary.sessionId)
            assertEquals("Workout A", summary.workoutName)
            assertEquals(500.0, summary.totalVolumeKg, 0.0)
            assertTrue(summary.totalDurationSeconds >= 0L)
            assertTrue(summary.completedAtEpochMs > 0L)
        } finally {
            database.close()
        }

        val reopened = openDatabase(context, dbName)
        try {
            val history = WorkoutSummaryRepositoryImpl(reopened.workoutSummaryDao()).getHistory()
            val reloaded = history.single { it.sessionId == sessionId }
            assertEquals("Workout A", reloaded.workoutName)
            assertEquals(500.0, reloaded.totalVolumeKg, 0.0)
            assertTrue(reloaded.completedAtEpochMs > 0L)
        } finally {
            reopened.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun completingSameSessionTwiceKeepsSingleHistoryRow() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "t18-idempotent-${System.nanoTime()}.db"
        val database = openDatabase(context, dbName)

        try {
            seedExercise(database)
            val workoutRepository = WorkoutRepositoryImpl(database.workoutDao())
            val sessionRepository = SessionRepositoryImpl(database.sessionDao())
            val workoutId = createWorkout(workoutRepository)
            val sessionId = createCompletedSession(sessionRepository, workoutRepository, workoutId)

            val useCase = buildCompleteUseCase(database, sessionRepository, workoutRepository)
            useCase(sessionId)
            useCase(sessionId)

            val summaries = database.workoutSummaryDao().getAllSummaries().filter { it.sessionId == sessionId }
            assertEquals(1, summaries.size)
        } finally {
            database.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun openDatabase(context: Context, dbName: String): GetStrongDatabase =
        Room.databaseBuilder(context, GetStrongDatabase::class.java, dbName).build()

    private suspend fun seedExercise(database: GetStrongDatabase) {
        database.exerciseDao().insert(
            ExerciseEntity(
                id = 1006L,
                name = "Deadlift",
                primaryMuscleGroup = "BACK",
                secondaryMuscleGroups = emptyList(),
                equipmentType = "BARBELL",
            ),
        )
    }

    private suspend fun createWorkout(workoutRepository: WorkoutRepositoryImpl): Long =
        workoutRepository.createWorkout(
            Workout(
                name = "Workout A",
                slots = listOf(
                    WorkoutExerciseSlot(
                        workoutId = 0L,
                        exerciseId = 1006L,
                        position = 0,
                        targetSets = 1,
                        targetReps = 5,
                        repRangeMin = 5,
                        repRangeMax = 5,
                        progressionMode = ProgressionModeCode.WEIGHT_ONLY,
                        incrementKg = 2.5,
                        deloadPercent = 10,
                        currentWorkingWeightKg = 100.0,
                    ),
                ),
            ),
        )

    private suspend fun createCompletedSession(
        sessionRepository: SessionRepositoryImpl,
        workoutRepository: WorkoutRepositoryImpl,
        workoutId: Long,
    ): Long {
        val slotId = workoutRepository.getWorkout(workoutId)!!.slots.single().id
        val sessionId = sessionRepository.startSession(
            workoutId = workoutId,
            plannedSets = listOf(
                SessionPlannedSet(
                    sessionId = 0L,
                    workoutSlotId = slotId,
                    setOrder = 0,
                    exerciseId = 1006L,
                    setType = SessionSetType.WORK,
                    targetReps = 5,
                    targetWeightKg = 100.0,
                ),
            ),
        )
        val plannedSetId = sessionRepository.getActiveSessionState(sessionId)!!.plannedSets.single().id
        sessionRepository.completePlannedSet(sessionId, plannedSetId, repsAchieved = 5)
        return sessionId
    }

    private fun buildCompleteUseCase(
        database: GetStrongDatabase,
        sessionRepository: SessionRepositoryImpl,
        workoutRepository: WorkoutRepositoryImpl,
    ): CompleteSessionAndSaveSummaryUseCase {
        val summaryRepository = SessionSummaryRepositoryImpl(
            sessionDao = database.sessionDao(),
            sessionSummaryCalculator = SessionSummaryCalculator(ElapsedTimeCalculator()),
        )
        val workoutSummaryRepository = WorkoutSummaryRepositoryImpl(database.workoutSummaryDao())
        return CompleteSessionAndSaveSummaryUseCase(
            completeSessionWithProgressionUseCase = CompleteSessionWithProgressionUseCase(
                sessionRepository = sessionRepository,
                workoutRepository = workoutRepository,
                progressionCalculator = ProgressionCalculator(),
            ),
            sessionRepository = sessionRepository,
            sessionSummaryRepository = summaryRepository,
            workoutSummaryRepository = workoutSummaryRepository,
            workoutRepository = workoutRepository,
        )
    }
}
