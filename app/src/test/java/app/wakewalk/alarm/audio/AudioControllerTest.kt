package app.wakewalk.alarm.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioControllerTest {

    private lateinit var context: Context
    private lateinit var audioController: AudioController

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        audioController = AudioController(context)
    }

    @Test
    fun testDefaultSoundTitleIsPhoneRingtone() {
        val titleNull = audioController.getSoundTitle(null)
        assertEquals("Phone Ringtone (Default)", titleNull)

        val titleToken = audioController.getSoundTitle(AudioController.URI_DEFAULT_CALL_RINGTONE)
        assertEquals("Phone Ringtone (Default)", titleToken)
    }

    @Test
    fun testStandardAlarmTitle() {
        val title = audioController.getSoundTitle(AudioController.URI_DEFAULT_ALARM)
        assertEquals("Standard Alarm Sound", title)
    }

    @Test
    fun testBuildCandidateUrisIncludesRingtonesByDefault() {
        val uris = audioController.buildCandidateUris(null)
        assertNotNull(uris)
    }

    @Test
    fun testEnsureMaxAlarmVolumeSetsStreamMax() {
        audioController.ensureMaxAlarmVolume()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
        val currentVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
        assertEquals(maxVol, currentVol)
    }

    @Test
    fun testRandomTokenSoundTitles() {
        val harshTitle = audioController.getSoundTitle(app.wakewalk.domain.sound.AlarmSoundRegistry.TOKEN_RANDOM_HARSH)
        assertEquals("🎲 Random (Harsh / Intense)", harshTitle)

        val smoothTitle = audioController.getSoundTitle(app.wakewalk.domain.sound.AlarmSoundRegistry.TOKEN_RANDOM_SMOOTH)
        assertEquals("🎲 Random (Smooth / Gentle)", smoothTitle)

        val allTitle = audioController.getSoundTitle(app.wakewalk.domain.sound.AlarmSoundRegistry.TOKEN_RANDOM_ALL)
        assertEquals("🎲 Random (All Sounds)", allTitle)
    }

    @Test
    fun testDuckAndRestoreVolumeFlags() {
        audioController.duckVolume(0.3f, durationMs = 10L)
        assertTrue(audioController.isVolumeDucked)

        audioController.restoreVolume(1.0f, durationMs = 10L)
        assertFalse(audioController.isVolumeDucked)
    }
}

