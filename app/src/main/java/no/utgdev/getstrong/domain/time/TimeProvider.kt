package no.utgdev.getstrong.domain.time

interface TimeProvider {
    fun nowMs(): Long
}
