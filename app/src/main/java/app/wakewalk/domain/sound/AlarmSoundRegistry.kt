package app.wakewalk.domain.sound

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class AlarmSoundRegistry @Inject constructor() {

    companion object {
        const val TOKEN_RANDOM_HARSH = "content://wakewalk/sound/random_harsh"
        const val TOKEN_RANDOM_SMOOTH = "content://wakewalk/sound/random_smooth"
        const val TOKEN_RANDOM_ALL = "content://wakewalk/sound/random_all"
        const val TOKEN_DEFAULT_CALL_RINGTONE = "content://wakewalk/sound/call_ringtone"
        const val TOKEN_DEFAULT_ALARM = "content://wakewalk/sound/alarm"

        const val ID_PHONE_DEFAULT = "sound_phone_default"
        const val ID_ALARM_STANDARD = "sound_alarm_standard"
    }

    private val sounds: List<SoundItem> = listOf(
        // Harsh / Intense
        SoundItem(
            id = "sound_harsh_siren",
            title = "Emergency Siren",
            category = SoundCategory.HARSH,
            rawResName = "sound_harsh_siren",
            contentUri = "content://wakewalk/sound/harsh_siren"
        ),
        SoundItem(
            id = "sound_harsh_bugle",
            title = "Military Bugle (Reveille)",
            category = SoundCategory.HARSH,
            rawResName = "sound_harsh_bugle",
            contentUri = "content://wakewalk/sound/harsh_bugle"
        ),
        SoundItem(
            id = "sound_harsh_rock",
            title = "Heavy Rock Riff",
            category = SoundCategory.HARSH,
            rawResName = "sound_harsh_rock",
            contentUri = "content://wakewalk/sound/harsh_rock"
        ),
        SoundItem(
            id = "sound_harsh_digital",
            title = "Pulsing Digital Alarm",
            category = SoundCategory.HARSH,
            rawResName = "sound_harsh_digital",
            contentUri = "content://wakewalk/sound/harsh_digital"
        ),

        // Smooth / Gentle
        SoundItem(
            id = "sound_smooth_piano",
            title = "Acoustic Piano Sunrise",
            category = SoundCategory.SMOOTH,
            rawResName = "sound_smooth_piano",
            contentUri = "content://wakewalk/sound/smooth_piano"
        ),
        SoundItem(
            id = "sound_smooth_ambient",
            title = "Ambient Chimes & Harp",
            category = SoundCategory.SMOOTH,
            rawResName = "sound_smooth_ambient",
            contentUri = "content://wakewalk/sound/smooth_ambient"
        ),
        SoundItem(
            id = "sound_smooth_chimes",
            title = "Morning Forest Birds",
            category = SoundCategory.SMOOTH,
            rawResName = "sound_smooth_chimes",
            contentUri = "content://wakewalk/sound/smooth_chimes"
        ),

        // Urgent Phone Call
        SoundItem(
            id = ID_PHONE_DEFAULT,
            title = "Phone Ringtone (Default)",
            category = SoundCategory.PHONE_CALL,
            contentUri = TOKEN_DEFAULT_CALL_RINGTONE
        ),
        SoundItem(
            id = "sound_phone_classic",
            title = "Classic Office Bell",
            category = SoundCategory.PHONE_CALL,
            rawResName = "sound_phone_classic",
            contentUri = "content://wakewalk/sound/phone_classic"
        ),
        SoundItem(
            id = ID_ALARM_STANDARD,
            title = "Standard Alarm Sound",
            category = SoundCategory.PHONE_CALL,
            contentUri = TOKEN_DEFAULT_ALARM
        )
    )

    fun getAllSounds(): List<SoundItem> = sounds

    fun getSoundsByCategory(category: SoundCategory): List<SoundItem> {
        return sounds.filter { it.category == category }
    }

    fun getRandomSound(category: SoundCategory? = null, excludeId: String? = null): SoundItem {
        val pool = (if (category != null) getSoundsByCategory(category) else sounds)
        val filtered = if (excludeId != null && pool.size > 1) {
            pool.filter { it.id != excludeId }
        } else {
            pool
        }
        return filtered[Random.nextInt(filtered.size)]
    }

    fun resolveSound(uriString: String?, lastSoundId: String? = null): SoundItem {
        if (uriString.isNullOrBlank()) {
            return sounds.first { it.id == ID_PHONE_DEFAULT }
        }

        return when (uriString) {
            TOKEN_RANDOM_HARSH -> getRandomSound(SoundCategory.HARSH, excludeId = lastSoundId)
            TOKEN_RANDOM_SMOOTH -> getRandomSound(SoundCategory.SMOOTH, excludeId = lastSoundId)
            TOKEN_RANDOM_ALL -> getRandomSound(category = null, excludeId = lastSoundId)
            TOKEN_DEFAULT_CALL_RINGTONE -> sounds.first { it.id == ID_PHONE_DEFAULT }
            TOKEN_DEFAULT_ALARM -> sounds.first { it.id == ID_ALARM_STANDARD }
            else -> sounds.find { it.contentUri == uriString } ?: sounds.first { it.id == ID_PHONE_DEFAULT }
        }
    }
}
