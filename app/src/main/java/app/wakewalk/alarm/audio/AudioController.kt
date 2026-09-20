package app.wakewalk.alarm.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
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
    private var fallbackRingtone: Ringtone? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var volumeRampJob: Job? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun startAlarmAudio(customSoundUri: String? = null, gradualVolume: Boolean = true) {
        stopAudio()

        // 1. Ensure stream is audible
        ensureAlarmVolumeAudible()

        // 2. Request audio focus
        requestAudioFocus()

        // 3. Resolve potential sound URIs in priority order
        val candidateUris = buildCandidateUris(customSoundUri)

        // 4. Try MediaPlayer first
        var playedSuccessfully = false
        for (uri in candidateUris) {
            try {
                Log.d("WakeWalk", "AudioController: Trying to play URI with MediaPlayer: $uri")
                val player = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(context, uri)
                    isLooping = true
                    prepare()
                    if (gradualVolume) {
                        setVolume(0.2f, 0.2f)
                    } else {
                        setVolume(1.0f, 1.0f)
                    }
                    start()
                }
                mediaPlayer = player
                playedSuccessfully = true
                Log.d("WakeWalk", "AudioController: Successfully started MediaPlayer with URI: $uri")
                break
            } catch (e: Exception) {
                Log.w("WakeWalk", "AudioController: MediaPlayer failed for URI: $uri", e)
            }
        }

        // 5. If MediaPlayer failed for all URIs, fallback to framework Ringtone
        if (!playedSuccessfully) {
            Log.w("WakeWalk", "AudioController: MediaPlayer failed for all URIs. Attempting Ringtone fallback.")
            for (uri in candidateUris) {
                try {
                    val ringtone = RingtoneManager.getRingtone(context, uri)
                    if (ringtone != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ringtone.isLooping = true
                        }
                        ringtone.audioAttributes = audioAttributes
                        ringtone.play()
                        fallbackRingtone = ringtone
                        playedSuccessfully = true
                        Log.d("WakeWalk", "AudioController: Successfully playing fallback Ringtone with URI: $uri")
                        break
                    }
                } catch (e: Exception) {
                    Log.w("WakeWalk", "AudioController: Ringtone fallback failed for URI: $uri", e)
                }
            }
        }

        // 6. If playing with gradual volume on MediaPlayer, start ramping
        if (playedSuccessfully && gradualVolume && mediaPlayer != null) {
            startVolumeRamp()
        }
    }

    private fun ensureAlarmVolumeAudible() {
        try {
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            Log.d("WakeWalk", "AudioController: Current STREAM_ALARM volume is $currentVol / $maxVol")
            if (currentVol <= 0 && maxVol > 0) {
                val targetVol = (maxVol * 0.75f).toInt().coerceAtLeast(1)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
                Log.d("WakeWalk", "AudioController: Adjusted STREAM_ALARM volume from 0 to $targetVol")
            }
        } catch (e: Exception) {
            Log.w("WakeWalk", "AudioController: Could not inspect or adjust alarm stream volume", e)
        }
    }

    private fun buildCandidateUris(customSoundUri: String?): List<Uri> {
        val list = mutableListOf<Uri>()

        // Custom URI if provided
        customSoundUri?.let {
            try {
                list.add(Uri.parse(it))
            } catch (_: Exception) {}
        }

        // Actual default alarm URI (resolves symbolic URI to actual media content URI)
        try {
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)?.let {
                list.add(it)
            }
        } catch (_: Exception) {}

        // Symbolic default alarm URI
        try {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let {
                list.add(it)
            }
        } catch (_: Exception) {}

        // Actual ringtone URI
        try {
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)?.let {
                list.add(it)
            }
        } catch (_: Exception) {}

        // Symbolic ringtone URI
        try {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.let {
                list.add(it)
            }
        } catch (_: Exception) {}

        // Actual notification URI
        try {
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)?.let {
                list.add(it)
            }
        } catch (_: Exception) {}

        // Symbolic notification URI
        try {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.let {
                list.add(it)
            }
        } catch (_: Exception) {}

        return list.distinct()
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

        try {
            fallbackRingtone?.let { ringtone ->
                if (ringtone.isPlaying) {
                    ringtone.stop()
                }
            }
        } catch (_: Exception) {
            // Ignored on cleanup
        } finally {
            fallbackRingtone = null
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
