package no.utgdev.getstrong.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE exercises ADD COLUMN secondaryMuscleGroups TEXT NOT NULL DEFAULT '[]'",
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE workout_exercise_slots ADD COLUMN targetSets INTEGER NOT NULL DEFAULT 5",
        )
        db.execSQL(
            "ALTER TABLE workout_exercise_slots ADD COLUMN targetReps INTEGER NOT NULL DEFAULT 5",
        )
        db.execSQL(
            "ALTER TABLE workout_exercise_slots ADD COLUMN progressionMode TEXT NOT NULL DEFAULT 'WEIGHT_ONLY'",
        )
        db.execSQL(
            "ALTER TABLE workout_exercise_slots ADD COLUMN incrementKg REAL NOT NULL DEFAULT 2.5",
        )
        db.execSQL(
            "ALTER TABLE workout_exercise_slots ADD COLUMN deloadPercent INTEGER NOT NULL DEFAULT 10",
        )
        db.execSQL(
            "ALTER TABLE workout_exercise_slots ADD COLUMN restSecondsOverride INTEGER",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_workout_exercise_slots_workoutId_position ON workout_exercise_slots(workoutId, position)",
        )
    }
}
