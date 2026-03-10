package no.utgdev.getstrong.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_planned_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId"), Index(value = ["sessionId", "setOrder"], unique = true)],
)
data class SessionPlannedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val workoutSlotId: Long,
    val setOrder: Int,
    val exerciseId: Long,
    val setType: String,
    val targetReps: Int,
    val targetWeightKg: Double?,
    val isCompleted: Boolean,
    val completedReps: Int?,
    val isExtra: Boolean,
)
