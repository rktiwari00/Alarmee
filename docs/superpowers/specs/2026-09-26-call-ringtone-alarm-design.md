# Call Ringtone as Alarm Tone Design Specification

## Overview
WakeWalk users often find standard alarm chimes easy to sleep through, whereas incoming phone call ringtones provoke an immediate subconscious waking response. This feature sets the user's incoming phone call ringtone as the default alarm sound for all newly created alarms, while providing a clear tone selector in the alarm editor to switch between the phone call ringtone, standard alarm chime, or any custom device sound.

---

## User Experience & Requirements
1. **Zero-Configuration Default:**
   - Any newly created alarm automatically plays the device's active incoming phone call ringtone (`RingtoneManager.TYPE_RINGTONE`).
   - If the call ringtone cannot be accessed or is unavailable on the device, it seamlessly falls back to the system alarm tone.
2. **Audio Attributes & Volume:**
   - Playback strictly uses `AudioAttributes.USAGE_ALARM` so that it plays loudly through the alarm audio stream, bypassing DND and silent/vibrate call profile restrictions.
   - Continuous playback looping with `setWakeMode(PowerManager.PARTIAL_WAKE_LOCK)`.
3. **Alarm Editor Tone Selection:**
   - The Create/Edit Alarm screen displays a "Sound & Tone" card showing the name of the active tone.
   - Defaults to "Phone Ringtone (Default)".
   - Clicking opens a dialog with three choices:
     1. **Phone Ringtone (Default)** (`content://wakewalk/sound/call_ringtone`)
     2. **Standard Alarm Tone** (`content://wakewalk/sound/alarm`)
     3. **Choose from Device...** (launches native Android `RingtoneManager.ACTION_RINGTONE_PICKER` dialog).

---

## Architecture & Component Changes

### 1. Audio Pipeline (`AudioController.kt`)
* **Constants:**
  * `URI_DEFAULT_CALL_RINGTONE = "content://wakewalk/sound/call_ringtone"`
  * `URI_DEFAULT_ALARM = "content://wakewalk/sound/alarm"`
* **Resolution Pipeline in `buildCandidateUris(customSoundUri: String?)`:**
  * If `customSoundUri != null && customSoundUri != URI_DEFAULT_CALL_RINGTONE && customSoundUri != URI_DEFAULT_ALARM`:
    * Attempts `Uri.parse(customSoundUri)`.
  * If `customSoundUri == URI_DEFAULT_ALARM`:
    * Tries `TYPE_ALARM` actual and default URIs first, then falls back to `TYPE_RINGTONE`.
  * If `customSoundUri == null || customSoundUri == URI_DEFAULT_CALL_RINGTONE`:
    * Tries `RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)`
    * Tries `RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)`
    * Fallback to `RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)`
    * Fallback to `RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)`
    * Fallback to `TYPE_NOTIFICATION`
* **Sound Title Query (`getSoundTitle(soundUri: String?): String`):**
  * Returns `"Phone Ringtone (Default)"` for null or `URI_DEFAULT_CALL_RINGTONE`.
  * Returns `"Standard Alarm Sound"` for `URI_DEFAULT_ALARM`.
  * For custom content URIs, queries `RingtoneManager.getRingtone(context, Uri.parse(uri)).getTitle(context)` to retrieve the localized ringtone title.

### 2. Alarm Scheduling & Service Delivery
* **`AndroidAlarmScheduler.kt`:**
  * Adds `EXTRA_SOUND_URI = "extra_sound_uri"`.
  * In `scheduleAlarm(alarm: AlarmEntity, ...)`: passes `putExtra(EXTRA_SOUND_URI, alarm.soundUri)`.
* **`AlarmForegroundService.kt`:**
  * In `handleStartAlarm(intent: Intent)`:
    * Extracts `currentSoundUri = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_SOUND_URI)`.
    * Invokes `audioController.startAlarmAudio(customSoundUri = currentSoundUri, gradualVolume = gradualVolume)`.

### 3. Presentation & Alarm Editor
* **`CreateEditAlarmViewModel.kt`:**
  * `CreateEditAlarmUiState` holds:
    * `val soundUri: String? = null`
    * `val soundTitle: String = "Phone Ringtone (Default)"`
  * Methods:
    * `fun setSoundUri(uriString: String?)`: updates URI and computes human-readable title.
  * In `saveAlarm`: passes `soundUri` when building `AlarmEntity`.
  * In `initForAlarm`: populates `soundUri` and `soundTitle` from the saved entity.
* **`CreateEditAlarmScreen.kt`:**
  * Adds "Sound & Tone" card displaying music note icon and current sound title.
  * Tapping triggers option dialog (Phone Ringtone / Standard Alarm / Choose from Device).
  * "Choose from Device" uses `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())` with `RingtoneManager.ACTION_RINGTONE_PICKER` intent configured with `EXTRA_RINGTONE_TYPE = TYPE_RINGTONE or TYPE_ALARM`.

---

## Testing Strategy
1. **Unit Testing (`AudioControllerTest` / `CreateEditAlarmViewModelTest`):**
   * Verify default `soundUri` is null or `URI_DEFAULT_CALL_RINGTONE` with title "Phone Ringtone (Default)".
   * Verify saving an alarm preserves custom `soundUri`.
   * Verify ringtone title resolution for system tokens.
2. **Build Verification:**
   * Run `./gradlew.bat testDebugUnitTest` and `./gradlew.bat assembleDebug` to ensure all tests pass and APK builds cleanly.
