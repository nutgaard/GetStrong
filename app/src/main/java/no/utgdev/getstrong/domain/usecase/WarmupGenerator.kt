package no.utgdev.getstrong.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import no.utgdev.getstrong.domain.model.EquipmentTypeCode

class WarmupGenerator @Inject constructor() {
    fun generate(
        workingWeightKg: Double,
        equipmentType: String,
        nonBarbellIncrementKg: Double = 1.0,
    ): List<Double> {
        if (workingWeightKg <= 0.0) return emptyList()
        val percentages = percentagesFor(workingWeightKg)
        return percentages.map { pct ->
            val raw = workingWeightKg * pct
            val rounded = roundForEquipment(raw, equipmentType, nonBarbellIncrementKg)
            maxOf(rounded, 5.0)
        }
    }

    private fun percentagesFor(workingWeightKg: Double): List<Double> =
        when {
            workingWeightKg < 20.0 -> listOf(0.55)
            workingWeightKg < 80.0 -> listOf(0.60)
            workingWeightKg < 120.0 -> listOf(0.45, 0.65, 0.80)
            else -> listOf(0.43, 0.57, 0.71, 0.86, 0.93)
        }

    private fun roundForEquipment(weightKg: Double, equipmentType: String, nonBarbellIncrementKg: Double): Double {
        val increment = if (equipmentType == EquipmentTypeCode.BARBELL) 2.5 else nonBarbellIncrementKg
        return roundToIncrement(weightKg, increment)
    }

    private fun roundToIncrement(value: Double, increment: Double): Double {
        if (increment <= 0.0) return value
        val quotient = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(increment), 10, RoundingMode.HALF_UP)
        val roundedQuotient = quotient.setScale(0, RoundingMode.HALF_UP)
        return roundedQuotient.multiply(BigDecimal.valueOf(increment)).setScale(3, RoundingMode.HALF_UP).toDouble()
    }
}
