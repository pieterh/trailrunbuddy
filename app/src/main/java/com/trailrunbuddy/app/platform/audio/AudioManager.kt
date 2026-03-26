package com.trailrunbuddy.app.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.trailrunbuddy.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioPlayer {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var alertSoundId: Int = 0
    private var warningBeepSoundId: Int = 0
    private var loaded = false

    init {
        soundPool.setOnLoadCompleteListener { _, _, _ -> loaded = true }
        alertSoundId = soundPool.load(context, R.raw.alert, 1)
        warningBeepSoundId = soundPool.load(context, R.raw.warning_beep, 1)
    }

    override fun playAlert() {
        if (loaded) soundPool.play(alertSoundId, 1f, 1f, 1, 0, 1f)
    }

    override fun playPreWarning() {
        if (loaded) soundPool.play(warningBeepSoundId, 0.7f, 0.7f, 1, 0, 1f)
    }

    override fun release() {
        soundPool.release()
    }
}
