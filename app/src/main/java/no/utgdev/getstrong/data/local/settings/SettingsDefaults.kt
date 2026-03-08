package no.utgdev.getstrong.data.local.settings

import no.utgdev.getstrong.domain.model.ProgressionModeCode

object SettingsDefaults {
    const val REST_DURATION_SECONDS = 180
    const val LOAD_INCREMENT_KG = 2.5
    const val DELOAD_PERCENT = 10
    const val DEFAULT_PROGRESSION_MODE = ProgressionModeCode.WEIGHT_ONLY
}
