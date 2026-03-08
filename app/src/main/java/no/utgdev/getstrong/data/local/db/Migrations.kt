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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `session_planned_sets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sessionId` INTEGER NOT NULL,
                `setOrder` INTEGER NOT NULL,
                `exerciseId` INTEGER NOT NULL,
                `setType` TEXT NOT NULL,
                `targetReps` INTEGER NOT NULL,
                `isCompleted` INTEGER NOT NULL,
                `completedReps` INTEGER,
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_planned_sets_sessionId` ON `session_planned_sets` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_planned_sets_exerciseId` ON `session_planned_sets` (`exerciseId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_session_planned_sets_sessionId_setOrder` ON `session_planned_sets` (`sessionId`, `setOrder`)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE workout_exercise_slots ADD COLUMN repRangeMin INTEGER NOT NULL DEFAULT 5",
        )
        db.execSQL(
            "ALTER TABLE workout_exercise_slots ADD COLUMN repRangeMax INTEGER NOT NULL DEFAULT 5",
        )
    }
}
