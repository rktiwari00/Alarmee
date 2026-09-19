package app.wakewalk.alarm.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var volumeRampJob: Job? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun startAlarmAudio(customSoundUri: String? = null, gradualVolume: Boolean = true) {
        stopAudio()

        requestAudioFocus()

        val soundUri = customSoundUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(context, soundUri)
                isLooping = true
                prepare()
                if (gradualVolume) {
                    setVolume(0.2f, 0.2f)
                } else {
                    setVolume(1.0f, 1.0f)
                }
                start()
            }

            if (gradualVolume) {
                startVolumeRamp()
            }
        } catch (_: Exception) {
            // If custom sound fails, fallback to system alarm sound
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(context, fallbackUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (_: Exception) {
                // Device audio system error
            }
        }
    }

    private fun startVolumeRamp() {
        volumeRampJob?.cancel()
        volumeRampJob = CoroutineScope(Dispatchers.Default).launch {
            // Ramp from 0.2 to 1.0 in 10 steps over 10 seconds
            for (step in 2..10) {
                delay(1000L)
                val vol = step / 10f
                try {
                    mediaPlayer?.setVolume(vol, vol)
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { /* maintain alarm */ }
                .build()
            audioFocusRequest = focusRequest
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    fun stopAudio() {
        volumeRampJob?.cancel()
        volumeRampJob = null

        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (_: Exception) {
            // Ignored on cleanup
        } finally {
            mediaPlayer = null
        }

        abandonAudioFocus()
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
