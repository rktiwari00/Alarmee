# Call Ringtone as Alarm Tone Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set the user's incoming phone call ringtone as the default alarm tone across WakeWalk, with a tone selection card in the alarm editor allowing users to pick between call ringtone, standard alarm, or custom device ringtones.

**Architecture:** Update `AudioController` to resolve incoming phone call ringtones by default while supporting custom URIs and system alarm fallbacks; transmit `soundUri` through `AndroidAlarmScheduler` into `AlarmForegroundService`; update `CreateEditAlarmViewModel` state and `CreateEditAlarmScreen` with a tone selection card and native Android ringtone picker.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android RingtoneManager, Hilt, Room, JUnit 4, Robolectric.

## Global Constraints
- Target SDK: 35, Compile SDK: 35, Min SDK: 26.
- Zero-configuration default: newly created alarms default to phone call ringtone without requiring user setup.
- Audio playback strictly uses `AudioAttributes.USAGE_ALARM` with `setWakeMode(PowerManager.PARTIAL_WAKE_LOCK)`.
- Use Android's native `RingtoneManager.ACTION_RINGTONE_PICKER` for custom device sound selection.

---

### Task 1: AudioController Tone Resolution and Sound Title Helper

**Files:**
- Modify: `app/src/main/java/app/wakewalk/alarm/audio/AudioController.kt`
- Create: `app/src/test/java/app/wakewalk/alarm/audio/AudioControllerTest.kt`

**Interfaces:**
- Produces:
  - `AudioController.URI_DEFAULT_CALL_RINGTONE: String` ("content://wakewalk/sound/call_ringtone")
  - `AudioController.URI_DEFAULT_ALARM: String` ("content://wakewalk/sound/alarm")
  - `AudioController.getSoundTitle(uriString: String?): String`

- [x] **Step 1: Write unit tests for AudioController sound resolution and titles**

```kotlin
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
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat testDebugUnitTest --tests "app.wakewalk.alarm.audio.AudioControllerTest"`
Expected: Compilation failure due to missing constants and `getSoundTitle`.

- [x] **Step 3: Implement AudioController constants, resolution, and title query**

In `app/src/main/java/app/wakewalk/alarm/audio/AudioController.kt`:
1. Add companion object with `URI_DEFAULT_CALL_RINGTONE` and `URI_DEFAULT_ALARM`.
2. Update `buildCandidateUris`:
   - If `customSoundUri != null && customSoundUri != URI_DEFAULT_CALL_RINGTONE && customSoundUri != URI_DEFAULT_ALARM`: parse custom URI.
   - If `customSoundUri == URI_DEFAULT_ALARM`: resolve `TYPE_ALARM` first, then `TYPE_RINGTONE`.
   - Default (`customSoundUri == null || customSoundUri == URI_DEFAULT_CALL_RINGTONE`):
     - `RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)`
     - `RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)`
     - `RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)`
     - `RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)`
     - `TYPE_NOTIFICATION`
3. Add `fun getSoundTitle(uriString: String?): String`:
   - null or `URI_DEFAULT_CALL_RINGTONE` -> `"Phone Ringtone (Default)"`
   - `URI_DEFAULT_ALARM` -> `"Standard Alarm Sound"`
   - otherwise query `RingtoneManager.getRingtone(context, Uri.parse(uriString))?.getTitle(context)` or fallback to `"Custom Sound"`.

- [x] **Step 4: Run test to verify it passes**

Run: `$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat testDebugUnitTest --tests "app.wakewalk.alarm.audio.AudioControllerTest"`
Expected: PASS.

- [x] **Step 5: Commit changes**

```bash
git add app/src/main/java/app/wakewalk/alarm/audio/AudioController.kt app/src/test/java/app/wakewalk/alarm/audio/AudioControllerTest.kt
git commit -m "feat: add call ringtone resolution and sound title query to AudioController"
```

---

### Task 2: Alarm Scheduling & Service Delivery with Sound URI

**Files:**
- Modify: `app/src/main/java/app/wakewalk/alarm/scheduler/AndroidAlarmScheduler.kt`
- Modify: `app/src/main/java/app/wakewalk/alarm/service/AlarmForegroundService.kt`

**Interfaces:**
- Consumes: `AudioController.URI_DEFAULT_CALL_RINGTONE`
- Produces: `AndroidAlarmScheduler.EXTRA_SOUND_URI = "extra_sound_uri"`

- [x] **Step 1: Update AndroidAlarmScheduler to pass EXTRA_SOUND_URI**

In `app/src/main/java/app/wakewalk/alarm/scheduler/AndroidAlarmScheduler.kt`:
1. Add `const val EXTRA_SOUND_URI = "extra_sound_uri"` to companion object.
2. In `scheduleAlarm(alarm: AlarmEntity, ...)`:
   Add `putExtra(EXTRA_SOUND_URI, alarm.soundUri)` to `triggerIntent`.

- [x] **Step 2: Update AlarmForegroundService to receive soundUri and start audio**

In `app/src/main/java/app/wakewalk/alarm/service/AlarmForegroundService.kt`:
1. Add `private var currentSoundUri: String? = null`.
2. In `handleStartAlarm(intent: Intent)`:
   Extract `currentSoundUri = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_SOUND_URI)`.
   Pass to `audioController.startAlarmAudio(customSoundUri = currentSoundUri, gradualVolume = gradualVolume)`.

- [x] **Step 3: Run existing unit tests to verify no regressions**

Run: `$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat testDebugUnitTest`
Expected: PASS.

- [x] **Step 4: Commit changes**

```bash
git add app/src/main/java/app/wakewalk/alarm/scheduler/AndroidAlarmScheduler.kt app/src/main/java/app/wakewalk/alarm/service/AlarmForegroundService.kt
git commit -m "feat: transmit soundUri from scheduler to AlarmForegroundService audio player"
```

---

### Task 3: Alarm Editor ViewModel & Sound Selection UI State

**Files:**
- Modify: `app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmViewModel.kt`
- Modify: `app/src/test/java/app/wakewalk/ui/create/CreateEditAlarmViewModelTest.kt`

**Interfaces:**
- Consumes: `AudioController.getSoundTitle`
- Produces:
  - `CreateEditAlarmUiState.soundUri: String?`
  - `CreateEditAlarmUiState.soundTitle: String`
  - `CreateEditAlarmViewModel.setSoundUri(uri: String?, title: String?)`

- [x] **Step 1: Write unit tests for sound selection state in CreateEditAlarmViewModelTest**

Add test to `CreateEditAlarmViewModelTest.kt`:
```kotlin
    @Test
    fun testDefaultSoundUriIsCallRingtone() {
        val state = viewModel.uiState.value
        assertEquals(null, state.soundUri)
        assertEquals("Phone Ringtone (Default)", state.soundTitle)
    }

    @Test
    fun testSetCustomSoundUriUpdatesStateAndPersists() = runTest {
        viewModel.setSoundUri("content://media/internal/audio/media/42", "Morning Breeze")
        assertEquals("content://media/internal/audio/media/42", viewModel.uiState.value.soundUri)
        assertEquals("Morning Breeze", viewModel.uiState.value.soundTitle)

        viewModel.saveAlarm {}
        testDispatcher.scheduler.advanceUntilIdle()

        val saved = fakeRepository.insertedAlarm
        assertEquals("content://media/internal/audio/media/42", saved?.soundUri)
    }
```

- [x] **Step 2: Run test to verify it fails**

Run: `$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat testDebugUnitTest --tests "app.wakewalk.ui.create.CreateEditAlarmViewModelTest"`
Expected: FAIL due to missing `soundTitle` and `setSoundUri`.

- [x] **Step 3: Update CreateEditAlarmViewModel and UI state**

In `app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmViewModel.kt`:
1. Inject `audioController: AudioController` (or context to resolve titles).
2. Add fields to `CreateEditAlarmUiState`:
   `val soundUri: String? = null`
   `val soundTitle: String = "Phone Ringtone (Default)"`
3. Add `fun setSoundUri(uri: String?, title: String? = null)`:
   Resolve title via `audioController.getSoundTitle(uri)` if title is null.
   Update `_uiState`.
4. In `initForAlarm`:
   When restoring alarm: load `alarm.soundUri` and resolve its title.
5. In `saveAlarm`:
   Pass `soundUri = state.soundUri` when creating `AlarmEntity`.

- [x] **Step 4: Run test to verify it passes**

Run: `$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat testDebugUnitTest --tests "app.wakewalk.ui.create.CreateEditAlarmViewModelTest"`
Expected: PASS.

- [x] **Step 5: Commit changes**

```bash
git add app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmViewModel.kt app/src/test/java/app/wakewalk/ui/create/CreateEditAlarmViewModelTest.kt
git commit -m "feat: add sound selection state and persistence to CreateEditAlarmViewModel"
```

---

### Task 4: Compose UI for Tone Selection & Native Ringtone Picker

**Files:**
- Modify: `app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmScreen.kt`

**Interfaces:**
- Consumes:
  - `CreateEditAlarmUiState.soundTitle`
  - `CreateEditAlarmUiState.soundUri`
  - `CreateEditAlarmViewModel.setSoundUri`

- [x] **Step 1: Add Sound & Tone card and selector dialog in CreateEditAlarmScreen**

In `app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmScreen.kt`:
1. Add state variable `var showSoundDialog by remember { mutableStateOf(false) }`.
2. Add ringtone picker activity launcher:
   ```kotlin
   val context = LocalContext.current
   val ringtonePickerLauncher = rememberLauncherForActivityResult(
       contract = ActivityResultContracts.StartActivityForResult()
   ) { result ->
       if (result.resultCode == Activity.RESULT_OK) {
           val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
               result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
           } else {
               @Suppress("DEPRECATION")
               result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
           }
           val title = uri?.let { RingtoneManager.getRingtone(context, it)?.getTitle(context) }
           viewModel.setSoundUri(uri?.toString(), title)
       }
   }
   ```
3. Add UI card for "Sound & Tone":
   - Row with `Icons.Default.MusicNote` or `NotificationsActive`.
   - Text "Sound & Tone" and subtitle showing `state.soundTitle`.
   - Chevron / clickable to open dialog.
4. Add selection dialog when `showSoundDialog == true`:
   - Option 1: "Phone Ringtone (Default)" -> `viewModel.setSoundUri(null)`
   - Option 2: "Standard Alarm Sound" -> `viewModel.setSoundUri(AudioController.URI_DEFAULT_ALARM)`
   - Option 3: "Choose from Device..." -> launches `RingtoneManager.ACTION_RINGTONE_PICKER` with `EXTRA_RINGTONE_TYPE = RingtoneManager.TYPE_RINGTONE or RingtoneManager.TYPE_ALARM`.

- [x] **Step 2: Run unit tests to verify no compilation or runtime regressions**

Run: `$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat testDebugUnitTest`
Expected: PASS.

- [x] **Step 3: Commit changes**

```bash
git add app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmScreen.kt
git commit -m "feat: add sound selection card and ringtone picker dialog in alarm editor"
```

---

### Task 5: End-to-End Verification and Build Validation

**Files:**
- Verify: Full test suite and APK assemble

- [x] **Step 1: Execute full test suite**

Run: `$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat testDebugUnitTest`
Expected: All unit tests PASS with 0 failures.

- [x] **Step 2: Assemble Debug APK**

Run: `$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL (generates `app/build/outputs/apk/debug/app-debug.apk`).

- [x] **Step 3: Commit and update documentation**

```bash
git add -A
git commit -m "chore: complete call ringtone alarm integration and verification"
```
