package no.utgdev.getstrong.data.seed

import no.utgdev.getstrong.data.local.entity.ExerciseEntity
import no.utgdev.getstrong.domain.model.EquipmentTypeCode
import no.utgdev.getstrong.domain.model.MuscleGroupCode

object ExerciseSeedData {
    val exercises: List<ExerciseEntity> = listOf(
        ex(1001, "Back Squat", MuscleGroupCode.QUADS, listOf(MuscleGroupCode.GLUTES), EquipmentTypeCode.BARBELL),
        ex(1002, "Front Squat", MuscleGroupCode.QUADS, listOf(MuscleGroupCode.CORE), EquipmentTypeCode.BARBELL),
        ex(1003, "Romanian Deadlift", MuscleGroupCode.HAMSTRINGS, listOf(MuscleGroupCode.GLUTES), EquipmentTypeCode.BARBELL),
        ex(1004, "Conventional Deadlift", MuscleGroupCode.LOWER_BACK, listOf(MuscleGroupCode.GLUTES, MuscleGroupCode.HAMSTRINGS), EquipmentTypeCode.BARBELL),
        ex(1005, "Sumo Deadlift", MuscleGroupCode.GLUTES, listOf(MuscleGroupCode.HAMSTRINGS, MuscleGroupCode.ADDUCTORS), EquipmentTypeCode.BARBELL),
        ex(1006, "Bench Press", MuscleGroupCode.CHEST, listOf(MuscleGroupCode.TRICEPS), EquipmentTypeCode.BARBELL),
        ex(1007, "Incline Bench Press", MuscleGroupCode.CHEST, listOf(MuscleGroupCode.SHOULDERS, MuscleGroupCode.TRICEPS), EquipmentTypeCode.BARBELL),
        ex(1008, "Overhead Press", MuscleGroupCode.SHOULDERS, listOf(MuscleGroupCode.TRICEPS), EquipmentTypeCode.BARBELL),
        ex(1009, "Push Press", MuscleGroupCode.SHOULDERS, listOf(MuscleGroupCode.TRICEPS, MuscleGroupCode.CORE), EquipmentTypeCode.BARBELL),
        ex(1010, "Bent-Over Row", MuscleGroupCode.BACK, listOf(MuscleGroupCode.BICEPS), EquipmentTypeCode.BARBELL),

        ex(1011, "Pendlay Row", MuscleGroupCode.BACK, listOf(MuscleGroupCode.LATS), EquipmentTypeCode.BARBELL),
        ex(1012, "Barbell Hip Thrust", MuscleGroupCode.GLUTES, listOf(MuscleGroupCode.HAMSTRINGS), EquipmentTypeCode.BARBELL),
        ex(1013, "Barbell Lunge", MuscleGroupCode.QUADS, listOf(MuscleGroupCode.GLUTES), EquipmentTypeCode.BARBELL),
        ex(1014, "Barbell Split Squat", MuscleGroupCode.QUADS, listOf(MuscleGroupCode.GLUTES), EquipmentTypeCode.BARBELL),
        ex(1015, "Dumbbell Bench Press", MuscleGroupCode.CHEST, listOf(MuscleGroupCode.TRICEPS), EquipmentTypeCode.DUMBBELL),
        ex(1016, "Incline Dumbbell Press", MuscleGroupCode.CHEST, listOf(MuscleGroupCode.SHOULDERS, MuscleGroupCode.TRICEPS), EquipmentTypeCode.DUMBBELL),
        ex(1017, "Dumbbell Shoulder Press", MuscleGroupCode.SHOULDERS, listOf(MuscleGroupCode.TRICEPS), EquipmentTypeCode.DUMBBELL),
        ex(1018, "Dumbbell Row", MuscleGroupCode.BACK, listOf(MuscleGroupCode.BICEPS), EquipmentTypeCode.DUMBBELL),
        ex(1019, "Dumbbell Romanian Deadlift", MuscleGroupCode.HAMSTRINGS, listOf(MuscleGroupCode.GLUTES), EquipmentTypeCode.DUMBBELL),
        ex(1020, "Dumbbell Lunge", MuscleGroupCode.QUADS, listOf(MuscleGroupCode.GLUTES), EquipmentTypeCode.DUMBBELL),

        ex(1021, "Goblet Squat", MuscleGroupCode.QUADS, listOf(MuscleGroupCode.CORE), EquipmentTypeCode.DUMBBELL),
        ex(1022, "Bulgarian Split Squat", MuscleGroupCode.QUADS, listOf(MuscleGroupCode.GLUTES), EquipmentTypeCode.DUMBBELL),
        ex(1023, "Dumbbell Curl", MuscleGroupCode.BICEPS, listOf(MuscleGroupCode.FOREARMS), EquipmentTypeCode.DUMBBELL),
        ex(1024, "Hammer Curl", MuscleGroupCode.BICEPS, listOf(MuscleGroupCode.FOREARMS), EquipmentTypeCode.DUMBBELL),
        ex(1025, "Dumbbell Lateral Raise", MuscleGroupCode.SHOULDERS, listOf(MuscleGroupCode.REAR_DELTS), EquipmentTypeCode.DUMBBELL),
        ex(1026, "Dumbbell Rear Delt Fly", MuscleGroupCode.REAR_DELTS, listOf(MuscleGroupCode.SHOULDERS), EquipmentTypeCode.DUMBBELL),
        ex(1027, "Dumbbell Triceps Extension", MuscleGroupCode.TRICEPS, emptyList(), EquipmentTypeCode.DUMBBELL),
        ex(1028, "Machine Leg Press", MuscleGroupCode.QUADS, listOf(MuscleGroupCode.GLUTES), EquipmentTypeCode.MACHINE),
        ex(1029, "Machine Hack Squat", MuscleGroupCode.QUADS, listOf(MuscleGroupCode.GLUTES), EquipmentTypeCode.MACHINE),
        ex(1030, "Leg Extension", MuscleGroupCode.QUADS, emptyList(), EquipmentTypeCode.MACHINE),

        ex(1031, "Seated Leg Curl", MuscleGroupCode.HAMSTRINGS, emptyList(), EquipmentTypeCode.MACHINE),
        ex(1032, "Lying Leg Curl", MuscleGroupCode.HAMSTRINGS, emptyList(), EquipmentTypeCode.MACHINE),
        ex(1033, "Calf Raise Machine", MuscleGroupCode.CALVES, emptyList(), EquipmentTypeCode.MACHINE),
        ex(1034, "Machine Chest Press", MuscleGroupCode.CHEST, listOf(MuscleGroupCode.TRICEPS), EquipmentTypeCode.MACHINE),
        ex(1035, "Machine Shoulder Press", MuscleGroupCode.SHOULDERS, listOf(MuscleGroupCode.TRICEPS), EquipmentTypeCode.MACHINE),
        ex(1036, "Lat Pulldown", MuscleGroupCode.LATS, listOf(MuscleGroupCode.BICEPS), EquipmentTypeCode.CABLE),
        ex(1037, "Seated Cable Row", MuscleGroupCode.BACK, listOf(MuscleGroupCode.BICEPS), EquipmentTypeCode.CABLE),
        ex(1038, "Cable Face Pull", MuscleGroupCode.REAR_DELTS, listOf(MuscleGroupCode.TRAPS), EquipmentTypeCode.CABLE),
        ex(1039, "Cable Fly", MuscleGroupCode.CHEST, listOf(MuscleGroupCode.SHOULDERS), EquipmentTypeCode.CABLE),
        ex(1040, "Cable Lateral Raise", MuscleGroupCode.SHOULDERS, listOf(MuscleGroupCode.REAR_DELTS), EquipmentTypeCode.CABLE),

        ex(1041, "Cable Triceps Pushdown", MuscleGroupCode.TRICEPS, emptyList(), EquipmentTypeCode.CABLE),
        ex(1042, "Cable Biceps Curl", MuscleGroupCode.BICEPS, listOf(MuscleGroupCode.FOREARMS), EquipmentTypeCode.CABLE),
        ex(1043, "Cable Crunch", MuscleGroupCode.ABS, listOf(MuscleGroupCode.CORE), EquipmentTypeCode.CABLE),
        ex(1044, "Pull-Up", MuscleGroupCode.LATS, listOf(MuscleGroupCode.BICEPS), EquipmentTypeCode.BODYWEIGHT),
        ex(1045, "Chin-Up", MuscleGroupCode.BICEPS, listOf(MuscleGroupCode.LATS), EquipmentTypeCode.BODYWEIGHT),
        ex(1046, "Dip", MuscleGroupCode.TRICEPS, listOf(MuscleGroupCode.CHEST), EquipmentTypeCode.BODYWEIGHT),
        ex(1047, "Push-Up", MuscleGroupCode.CHEST, listOf(MuscleGroupCode.TRICEPS), EquipmentTypeCode.BODYWEIGHT),
        ex(1048, "Plank", MuscleGroupCode.CORE, listOf(MuscleGroupCode.ABS), EquipmentTypeCode.BODYWEIGHT),
        ex(1049, "Kettlebell Swing", MuscleGroupCode.GLUTES, listOf(MuscleGroupCode.HAMSTRINGS, MuscleGroupCode.CORE), EquipmentTypeCode.KETTLEBELL),
        ex(1050, "EZ-Bar Curl", MuscleGroupCode.BICEPS, listOf(MuscleGroupCode.FOREARMS), EquipmentTypeCode.EZ_BAR),
    )

    private fun ex(
        id: Long,
        name: String,
        primary: String,
        secondary: List<String>,
        equipment: String,
    ): ExerciseEntity = ExerciseEntity(
        id = id,
        name = name,
        primaryMuscleGroup = primary,
        secondaryMuscleGroups = secondary,
        equipmentType = equipment,
    )
}
