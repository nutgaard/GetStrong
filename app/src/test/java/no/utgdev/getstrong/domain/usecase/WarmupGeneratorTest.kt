package no.utgdev.getstrong.domain.usecase

import no.utgdev.getstrong.domain.model.EquipmentTypeCode
import org.junit.Assert.assertEquals
import org.junit.Test

class WarmupGeneratorTest {
    private val generator = WarmupGenerator()

    @Test
    fun lateralRaise9kgReturnsSingleWarmupAt5kg() {
        val warmups = generator.generate(
            workingWeightKg = 9.0,
            equipmentType = EquipmentTypeCode.DUMBBELL,
        )
        assertEquals(listOf(5.0), warmups)
    }

    @Test
    fun deadlift140kgReturnsExpectedBarbellWarmups() {
        val warmups = generator.generate(
            workingWeightKg = 140.0,
            equipmentType = EquipmentTypeCode.BARBELL,
        )
        assertEquals(listOf(60.0, 80.0, 100.0, 120.0, 130.0), warmups)
    }

    @Test
    fun mediumBandUsesSingle60PercentRounded() {
        val warmups = generator.generate(
            workingWeightKg = 70.0,
            equipmentType = EquipmentTypeCode.BARBELL,
        )
        assertEquals(listOf(42.5), warmups)
    }

    @Test
    fun highBandUsesThreeWarmupsWithNonBarbellIncrement() {
        val warmups = generator.generate(
            workingWeightKg = 100.0,
            equipmentType = EquipmentTypeCode.MACHINE,
            nonBarbellIncrementKg = 1.0,
        )
        assertEquals(listOf(45.0, 65.0, 80.0), warmups)
    }
}
