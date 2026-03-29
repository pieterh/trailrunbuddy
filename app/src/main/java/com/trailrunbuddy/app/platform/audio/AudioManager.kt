package com.trailrunbuddy.app.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager as SystemAudioManager
import android.media.SoundPool
import com.trailrunbuddy.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val ALERT_DURATION_MS = 5_000L

@Singleton
class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioPlayer {

    private val systemAudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(SystemAudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(audioAttributes)
        .build()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(audioAttributes)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var alertSoundId: Int = 0
    private var warningBeepSoundId: Int = 0
    private var loaded = false

    private var activeAlertStreamId: Int = 0
    private var activeAlertJob: Job? = null

    init {
        soundPool.setOnLoadCompleteListener { _, _, _ -> loaded = true }
        alertSoundId = soundPool.load(context, R.raw.facility_breach_alarm, 1)
        warningBeepSoundId = soundPool.load(context, R.raw.warning_beep, 1)
    }

    override fun playAlert() {
        if (!loaded) return

        // Stop any in-progress alert before starting a new one
        activeAlertJob?.cancel()
        if (activeAlertStreamId != 0) {
            soundPool.stop(activeAlertStreamId)
            activeAlertStreamId = 0
        }

        val result = systemAudioManager.requestAudioFocus(focusRequest)
        if (result == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            val streamId = soundPool.play(alertSoundId, 1f, 1f, 1, -1, 1f) // loop = -1: repeat until stopped
            activeAlertStreamId = streamId
            activeAlertJob = scope.launch {
                delay(ALERT_DURATION_MS)
                soundPool.stop(streamId)
                activeAlertStreamId = 0
                systemAudioManager.abandonAudioFocusRequest(focusRequest)
            }
        }
    }

    override fun playPreWarning() {
        if (!loaded) return
        val result = systemAudioManager.requestAudioFocus(focusRequest)
        if (result == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            soundPool.play(warningBeepSoundId, 0.7f, 0.7f, 1, 0, 1f)
            scope.launch {
                delay(2_000L)
                systemAudioManager.abandonAudioFocusRequest(focusRequest)
            }
        }
    }

    override fun release() {
        activeAlertJob?.cancel()
        if (activeAlertStreamId != 0) soundPool.stop(activeAlertStreamId)
        systemAudioManager.abandonAudioFocusRequest(focusRequest)
        scope.cancel()
        soundPool.release()
    }
}
