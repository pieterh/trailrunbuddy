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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val FOCUS_HOLD_MS = 2_000L

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

    init {
        soundPool.setOnLoadCompleteListener { _, _, _ -> loaded = true }
        alertSoundId = soundPool.load(context, R.raw.alert, 1)
        warningBeepSoundId = soundPool.load(context, R.raw.warning_beep, 1)
    }

    override fun playAlert() {
        playWithFocus(alertSoundId, 1f, 1f)
    }

    override fun playPreWarning() {
        playWithFocus(warningBeepSoundId, 0.7f, 0.7f)
    }

    override fun release() {
        systemAudioManager.abandonAudioFocusRequest(focusRequest)
        scope.cancel()
        soundPool.release()
    }

    private fun playWithFocus(soundId: Int, leftVol: Float, rightVol: Float) {
        if (!loaded) return
        val result = systemAudioManager.requestAudioFocus(focusRequest)
        if (result == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            soundPool.play(soundId, leftVol, rightVol, 1, 0, 1f)
            scope.launch {
                delay(FOCUS_HOLD_MS)
                systemAudioManager.abandonAudioFocusRequest(focusRequest)
            }
        }
    }
}
