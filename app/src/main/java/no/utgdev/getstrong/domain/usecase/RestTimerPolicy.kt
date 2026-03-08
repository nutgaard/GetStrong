package no.utgdev.getstrong.domain.usecase

import javax.inject.Inject
import no.utgdev.getstrong.domain.model.SessionPlannedSet
import no.utgdev.getstrong.domain.model.SessionSetType

class RestTimerPolicy @Inject constructor() {
    fun shouldStartForTransition(
        completedSet: SessionPlannedSet?,
        nextCurrentSet: SessionPlannedSet?,
    ): Boolean {
        if (completedSet == null || nextCurrentSet == null) return false
        return completedSet.setType == SessionSetType.WARMUP &&
            nextCurrentSet.setType == SessionSetType.WORK &&
            completedSet.workoutSlotId == nextCurrentSet.workoutSlotId
    }
}
