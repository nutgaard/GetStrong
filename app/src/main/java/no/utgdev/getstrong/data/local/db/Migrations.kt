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

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE workout_exercise_slots ADD COLUMN currentWorkingWeightKg REAL NOT NULL DEFAULT 0.0",
        )
        db.execSQL(
            "ALTER TABLE workout_exercise_slots ADD COLUMN lastProgressionSessionId INTEGER",
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE workout_exercise_slots ADD COLUMN failureStreak INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "ALTER TABLE session_planned_sets ADD COLUMN workoutSlotId INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "ALTER TABLE set_results ADD COLUMN workoutSlotId INTEGER",
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE session_planned_sets ADD COLUMN targetWeightKg REAL",
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE set_results ADD COLUMN plannedSetId INTEGER",
        )
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE workout_summaries ADD COLUMN workoutName TEXT NOT NULL DEFAULT ''",
        )
        db.execSQL(
            """
            DELETE FROM workout_summaries
            WHERE id NOT IN (
                SELECT MIN(id)
                FROM workout_summaries
                GROUP BY sessionId
            )
            """.trimIndent(),
        )
        db.execSQL("DROP INDEX IF EXISTS index_workout_summaries_sessionId")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_workout_summaries_sessionId ON workout_summaries(sessionId)",
        )
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE session_planned_sets ADD COLUMN isExtra INTEGER NOT NULL DEFAULT 0",
        )
    }
}
