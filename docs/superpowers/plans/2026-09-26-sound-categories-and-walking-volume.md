# Categorized Sound Bank, Daily Randomization & Walking Volume Ducking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a categorized lightweight alarm sound bank (Harsh, Smooth, Phone Call), category-scoped daily sound randomization (anti-habituation), 100% hardcore starting volume, 10-step dynamic volume ducking to 30%, and an 8-second anti-slacking walking watchdog.

**Architecture:** 
- `AlarmSoundRegistry` encapsulates sound metadata and category resolution.
- `AudioController` handles stream maxing, smooth volume ducking, volume restoration, and sound previewing.
- `AlarmForegroundService` resolves daily randomized tokens upon ringing, invokes ducking at 10 steps, and runs an 8s inactivity watchdog.
- `CreateEditAlarmScreen` provides a grouped sound picker with live previews and random options.
- `AlarmScreen` displays dynamic lockscreen feedback badges.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android MediaPlayer, AudioManager, Room Database, StateFlow/Coroutines.

## Global Constraints
- Android SDK 26 to 35 support
- Pure Kotlin domain models and registries
- Zero third-party audio libraries (use native Android `MediaPlayer` and `AudioManager`)
- Audio assets must stay below 1 MB total to avoid APK bloat
- 100% JVM unit-tested domain and controller logic

---

### Task 1: Domain Sound Models & Registry

**Files:**
- Create: `app/src/main/java/app/wakewalk/domain/sound/SoundModels.kt`
- Create: `app/src/main/java/app/wakewalk/domain/sound/AlarmSoundRegistry.kt`
- Test: `app/src/test/java/app/wakewalk/domain/sound/AlarmSoundRegistryTest.kt`

**Interfaces:**
- Produces:
  - `enum class SoundCategory(val displayName: String) { HARSH, SMOOTH, PHONE_CALL }`
  - `data class SoundItem(val id: String, val title: String, val category: SoundCategory, val rawResName: String? = null, val contentUri: String? = null)`
  - `AlarmSoundRegistry`:
    - `fun getAllSounds(): List<SoundItem>`
    - `fun getSoundsByCategory(category: SoundCategory): List<SoundItem>`
    - `fun getRandomSound(category: SoundCategory?, excludeId: String? = null): SoundItem`
    - `fun resolveSoundUri(uriString: String?, lastSoundId: String? = null): Pair<SoundItem, String>`
    - Constants: `TOKEN_RANDOM_HARSH`, `TOKEN_RANDOM_SMOOTH`, `TOKEN_RANDOM_ALL`

- [ ] **Step 1: Write the failing unit tests for `AlarmSoundRegistry`**
  Create `app/src/test/java/app/wakewalk/domain/sound/AlarmSoundRegistryTest.kt`:
  - Test `getSoundsByCategory(HARSH)` returns only harsh sounds.
  - Test `getSoundsByCategory(SMOOTH)` returns only smooth sounds.
  - Test `getRandomSound(category)` returns a sound belonging to that category.
  - Test `resolveSoundUri(TOKEN_RANDOM_HARSH)` resolves to a harsh sound item.
  - Test `resolveSoundUri(null)` falls back to default phone ringtone.

- [ ] **Step 2: Run test to confirm failure**
  Run: `$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat testDebugUnitTest --tests "app.wakewalk.domain.sound.AlarmSoundRegistryTest"`

- [ ] **Step 3: Implement `SoundModels.kt` and `AlarmSoundRegistry.kt`**
  Implement category enums, sound items, and registry methods with deterministic fallback.

- [ ] **Step 4: Run test to confirm pass**
  Run: `$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat testDebugUnitTest --tests "app.wakewalk.domain.sound.AlarmSoundRegistryTest"`

- [ ] **Step 5: Commit changes**
  Run: `git add app/src/main/java/app/wakewalk/domain/sound/ app/src/test/java/app/wakewalk/domain/sound/; git commit -m "feat(domain): add sound models and AlarmSoundRegistry with category filtering"`

---

### Task 2: Curated Lightweight Audio Assets Generation

**Files:**
- Create: `app/src/main/res/raw/sound_harsh_siren.ogg`
- Create: `app/src/main/res/raw/sound_harsh_bugle.ogg`
- Create: `app/src/main/res/raw/sound_harsh_rock.ogg`
- Create: `app/src/main/res/raw/sound_harsh_digital.ogg`
- Create: `app/src/main/res/raw/sound_smooth_piano.ogg`
- Create: `app/src/main/res/raw/sound_smooth_ambient.ogg`
- Create: `app/src/main/res/raw/sound_smooth_chimes.ogg`
- Create: `app/src/main/res/raw/sound_phone_classic.ogg`

**Interfaces:**
- Produces: Android raw resource identifiers (`R.raw.sound_*`) for playback by `MediaPlayer`.

- [ ] **Step 1: Generate pristine, royalty-free audio waveform loops**
  Use a Python script with standard libraries / wave synthesis to synthesize harmonic, distinct audio loops (dual-tone klaxon, bugle fanfare, chord progression, acoustic piano, resonant chimes) and encode to OGG Vorbis / WAV for `res/raw/`. Keep total size under 800 KB.

- [ ] **Step 2: Verify audio files are valid and loadable by Android SDK**
  Verify file sizes and header signatures.

- [ ] **Step 3: Commit raw audio assets**
  Run: `git add app/src/main/res/raw/; git commit -m "feat(audio): add curated lightweight alarm loops for harsh, smooth, and phone categories"`

---

### Task 3: AudioController Hardcore Volume, Smooth Ducking & Sound Preview

**Files:**
- Modify: `app/src/main/java/app/wakewalk/alarm/audio/AudioController.kt`
- Modify: `app/src/test/java/app/wakewalk/alarm/audio/AudioControllerTest.kt`

**Interfaces:**
- Consumes: `AlarmSoundRegistry`
- Produces:
  - `fun ensureMaxAlarmVolume()`
  - `fun duckVolume(targetVolume: Float = 0.30f, durationMs: Long = 1500L)`
  - `fun restoreVolume(targetVolume: Float = 1.0f, durationMs: Long = 2000L)`
  - `val isVolumeDucked: Boolean`
  - `fun previewSound(rawResId: Int?, uri: Uri? = null, onCompletion: () -> Unit = {})`
  - `fun stopPreview()`

- [ ] **Step 1: Write unit tests for duckVolume and ensureMaxAlarmVolume**
  Update `app/src/test/java/app/wakewalk/alarm/audio/AudioControllerTest.kt` to test ducking and volume methods.

- [ ] **Step 2: Implement ducking, volume restoration, and preview in `AudioController.kt`**
  - Implement `ensureMaxAlarmVolume()` setting `STREAM_ALARM` to `getStreamMaxVolume()`.
  - Implement coroutine-based volume interpolation in `duckVolume` and `restoreVolume`.
  - Implement separate preview `MediaPlayer` instance for safe UI sampling without interfering with alarm audio.

- [ ] **Step 3: Run unit tests**
  Run: `$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat testDebugUnitTest --tests "app.wakewalk.alarm.audio.AudioControllerTest"`

- [ ] **Step 4: Commit changes**
  Run: `git add app/src/main/java/app/wakewalk/alarm/audio/ app/src/test/java/app/wakewalk/alarm/audio/; git commit -m "feat(audio): add 100% volume enforcement, smooth ducking, and sound preview to AudioController"`

---

### Task 4: Service Walking Watchdog & Dynamic Volume Ducking

**Files:**
- Modify: `app/src/main/java/app/wakewalk/alarm/service/AlarmForegroundService.kt`
- Modify: `app/src/main/java/app/wakewalk/data/local/AlarmEntity.kt`
- Modify: `app/src/main/java/app/wakewalk/data/preferences/UserPreferences.kt`

**Interfaces:**
- Consumes: `AudioController.duckVolume`, `AudioController.restoreVolume`, `AlarmSoundRegistry`
- Produces:
  - Dynamic volume state broadcasting (`isDucked`, `isPausedWarning`) to `AlarmActivity`
  - 8-second walking inactivity detection

- [ ] **Step 1: Add `lowerVolumeWhileWalking` to `AlarmEntity` and `UserPreferences`**
  Default to `true`.

- [ ] **Step 2: Update `AlarmForegroundService.kt`**
  - In `handleStartAlarm`: call `AlarmSoundRegistry.resolveSoundUri()` to resolve random tokens into a concrete sound for the day.
  - In sensor callback: when `currentSteps >= 10 && !isVolumeDucked && lowerVolumeWhileWalking`, call `audioController.duckVolume(0.30f)`.
  - Implement `startWalkingWatchdog()` coroutine loop: checks every 1000ms. If `isVolumeDucked && (now - lastStepTimestamp) >= 8000L`, calls `audioController.restoreVolume(1.0f)` and emits warning state.
  - When new authentic step arrives after pause: smoothly re-ducks volume.

- [ ] **Step 3: Run full unit test suite**
  Run: `$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat testDebugUnitTest`

- [ ] **Step 4: Commit changes**
  Run: `git add app/src/main/java/app/wakewalk/alarm/service/ app/src/main/java/app/wakewalk/data/; git commit -m "feat(service): implement 10-step volume ducking, random sound resolution, and 8s inactivity watchdog"`

---

### Task 5: UI Sound Picker with Previews & Randomization Options

**Files:**
- Modify: `app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmScreen.kt`
- Modify: `app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmViewModel.kt`
- Modify: `app/src/main/java/app/wakewalk/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/app/wakewalk/ui/alarm/AlarmScreen.kt`

**Interfaces:**
- Consumes: `AlarmSoundRegistry`, `AudioController`
- Produces:
  - Grouped Category Sound Picker Dialog with preview buttons
  - "Lower volume while walking" toggle switch
  - Live lockscreen volume status pill badge

- [ ] **Step 1: Update `CreateEditAlarmViewModel.kt`**
  Expose sound categories, selected sound title, preview playback state, and `lowerVolumeWhileWalking` toggle state.

- [ ] **Step 2: Build `SoundPickerDialog` in `CreateEditAlarmScreen.kt`**
  Display expandable/categorized sections:
  - 🎲 Daily Randomize (Harsh, Smooth, All)
  - ⚡ Harsh / Intense
  - 🌿 Smooth / Gentle
  - 📞 Urgent Phone Call
  - 🔔 System Ringtones
  With play/pause preview icons next to each tone.

- [ ] **Step 3: Update `AlarmScreen.kt` Lockscreen UI**
  Show dynamic status pill:
  - `🔊 Full Volume`
  - `🚶 30% Volume (Walking Active)`
  - `⚠️ Keep Moving! Volume ramping up...`

- [ ] **Step 4: Run full verification & build debug APK**
  Run: `$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat testDebugUnitTest`
  Run: `$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"; .\gradlew.bat assembleDebug`

- [ ] **Step 5: Commit changes and push to GitHub**
  Run: `git add -A; git commit -m "feat(ui): add categorized sound picker dialog with live previews and dynamic volume badges"; git push origin master`
