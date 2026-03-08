package no.utgdev.getstrong.ui.activeWorkout

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject

class ToneRestSignalPlayer @Inject constructor() : RestSignalPlayer {
    override fun playRestOverSignal() {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        try {
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 400)
        } finally {
            tone.release()
        }
    }
}
