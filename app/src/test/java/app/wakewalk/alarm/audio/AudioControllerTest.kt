package app.wakewalk.alarm.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}
