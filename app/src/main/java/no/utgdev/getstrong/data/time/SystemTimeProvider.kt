package no.utgdev.getstrong.data.time

import javax.inject.Inject
import no.utgdev.getstrong.domain.time.TimeProvider

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowMs(): Long = System.currentTimeMillis()
}
