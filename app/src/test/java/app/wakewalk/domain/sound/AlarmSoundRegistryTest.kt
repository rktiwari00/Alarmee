package app.wakewalk.domain.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AlarmSoundRegistryTest {

    private lateinit var registry: AlarmSoundRegistry

    @Before
    fun setUp() {
        registry = AlarmSoundRegistry()
    }

    @Test
    fun getSoundsByCategory_harsh_returnsOnlyHarshSounds() {
        val harshSounds = registry.getSoundsByCategory(SoundCategory.HARSH)
        assertTrue(harshSounds.isNotEmpty())
        assertTrue(harshSounds.all { it.category == SoundCategory.HARSH })
    }

    @Test
    fun getSoundsByCategory_smooth_returnsOnlySmoothSounds() {
        val smoothSounds = registry.getSoundsByCategory(SoundCategory.SMOOTH)
        assertTrue(smoothSounds.isNotEmpty())
        assertTrue(smoothSounds.all { it.category == SoundCategory.SMOOTH })
    }

    @Test
    fun getSoundsByCategory_phoneCall_returnsOnlyPhoneCallSounds() {
        val phoneSounds = registry.getSoundsByCategory(SoundCategory.PHONE_CALL)
        assertTrue(phoneSounds.isNotEmpty())
        assertTrue(phoneSounds.all { it.category == SoundCategory.PHONE_CALL })
    }

    @Test
    fun getRandomSound_withinCategory_returnsMatchingSound() {
        val sound = registry.getRandomSound(SoundCategory.HARSH)
        assertEquals(SoundCategory.HARSH, sound.category)
    }

    @Test
    fun getRandomSound_excludeId_avoidsExcludedIdWhenMultipleAvailable() {
        val harshSounds = registry.getSoundsByCategory(SoundCategory.HARSH)
        if (harshSounds.size > 1) {
            val excludedId = harshSounds.first().id
            val picked = registry.getRandomSound(SoundCategory.HARSH, excludeId = excludedId)
            assertNotEquals(excludedId, picked.id)
        }
    }

    @Test
    fun resolveSound_randomHarshToken_resolvesToHarshSound() {
        val sound = registry.resolveSound(AlarmSoundRegistry.TOKEN_RANDOM_HARSH)
        assertEquals(SoundCategory.HARSH, sound.category)
    }

    @Test
    fun resolveSound_randomSmoothToken_resolvesToSmoothSound() {
        val sound = registry.resolveSound(AlarmSoundRegistry.TOKEN_RANDOM_SMOOTH)
        assertEquals(SoundCategory.SMOOTH, sound.category)
    }

    @Test
    fun resolveSound_randomAllToken_resolvesToValidSound() {
        val sound = registry.resolveSound(AlarmSoundRegistry.TOKEN_RANDOM_ALL)
        assertNotNull(sound)
        assertTrue(registry.getAllSounds().contains(sound))
    }

    @Test
    fun resolveSound_nullOrEmpty_resolvesToDefaultPhoneRingtone() {
        val sound = registry.resolveSound(null)
        assertEquals(AlarmSoundRegistry.ID_PHONE_DEFAULT, sound.id)
    }

    @Test
    fun resolveSound_specificId_resolvesDirectly() {
        val specific = registry.getAllSounds().first()
        val resolved = registry.resolveSound(specific.contentUri)
        assertEquals(specific.id, resolved.id)
    }
}
