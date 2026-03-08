package no.utgdev.getstrong.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import no.utgdev.getstrong.data.local.dao.ExerciseDao
import no.utgdev.getstrong.data.local.dao.SessionDao
import no.utgdev.getstrong.data.local.dao.WorkoutDao
import no.utgdev.getstrong.data.local.dao.WorkoutSummaryDao
import no.utgdev.getstrong.data.local.entity.ExerciseEntity
import no.utgdev.getstrong.data.local.entity.SetResultEntity
import no.utgdev.getstrong.data.local.entity.SessionPlannedSetEntity
import no.utgdev.getstrong.data.local.entity.WorkoutEntity
import no.utgdev.getstrong.data.local.entity.WorkoutExerciseSlotEntity
import no.utgdev.getstrong.data.local.entity.WorkoutSessionEntity
import no.utgdev.getstrong.data.local.entity.WorkoutSummaryEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseSlotEntity::class,
        WorkoutSessionEntity::class,
        SetResultEntity::class,
        SessionPlannedSetEntity::class,
        WorkoutSummaryEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(StringListConverters::class)
abstract class GetStrongDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun sessionDao(): SessionDao
    abstract fun workoutSummaryDao(): WorkoutSummaryDao
}
