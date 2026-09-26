# Design Specification: Categorized Sound Bank, Daily Randomization & Dynamic Walking Volume

**Date:** 2026-09-26  
**Status:** Approved  
**Topic:** Categorized Alarm Sounds (Harsh, Smooth, Phone Call), Category-Scoped Daily Randomization, and Dynamic 10-Step Walking Volume Ducking  

---

## 1. Overview & Problem Statement

### 1.1 The Problems
1. **Auditory Habituation:** Hearing the exact same alarm sound every day causes the sleeping subconscious to adapt and ignore it.
2. **Audio Harshness While Walking:** Once a user is out of bed and walking, continuing to blast 100% volume causes ear strain and disturbs others in the household.
3. **APK Size Bloat:** Bundling uncompressed audio files can balloon the APK size by 15–20 MB.

### 1.2 The Solution
1. **Lightweight Categorized Sound Bank (< 800 KB):** Highly optimized, royalty-free audio tracks encoded in 48–64 kbps OGG Vorbis (`.ogg`), split into three distinct psychological categories:
   - **⚡ Harsh / Intense** (Emergency Siren, Reveille Bugle, Heavy Rock, Digital Pulse)
   - **🌿 Smooth / Gentle** (Acoustic Piano, Ambient Sunrise, Forest Chimes)
   - **📞 Urgent Phone Call** (Classic Phone Ring, Modern Line, Device Call Tone)
2. **🎲 Category-Scoped Daily Randomization:** The user can configure their alarm to shuffle across a specific category (e.g. *"Randomize (Harsh)"* or *"Randomize (Smooth)"*) or *"Randomize (All)"*. Each morning, a different track is selected.
3. **🚶 10-Step Dynamic Volume Ducking & Tonal Softening:**
   - Rings at **100% maximum volume** while in bed (0–9 steps).
   - At **10 verified steps**, the audio smoothly attenuates to **30% volume** (and can optionally switch to a soothing smooth tone).
   - If walking stops for **8 seconds**, volume ramps back up to **100%** to force the user back on their feet.

---

## 2. Architecture & Data Flow

```
┌────────────────────────────────────────────────────────┐
│                   AlarmSoundRegistry                   │
│   • HARSH: Siren, Bugle, Rock, Digital                 │
│   • SMOOTH: Piano, Ambient Sunrise, Forest Chimes      │
│   • PHONE_CALL: Classic Ring, Modern Line, Default     │
│   • getRandomSound(category, excludeId)                │
└──────────────────────────┬─────────────────────────────┘
                           │ Resolves Sound
                           ▼
┌────────────────────────────────────────────────────────┐
│                 AlarmForegroundService                 │
│   • Resolves randomized URI on trigger                 │
│   • Sets 100% Volume on STREAM_ALARM                   │
│   • Observes MovementValidator steps                   │
│   • 8-second Inactivity Watchdog                       │
└──────────────┬───────────────────────────┬─────────────┘
               │                           │
   Step >= 10  │                           │ Watchdog (Idle > 8s)
               ▼                           ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│  AudioController.duckVolume  │ │ AudioController.restoreVolume│
│  (Smooth fade to 30%)        │ │ (Smooth fade to 100%)        │
└──────────────────────────────┘ └──────────────────────────────┘
```

---

## 3. Detailed Component Design

### 3.1 Sound Model & Sound Registry (`app.wakewalk.domain.sound`)

```kotlin
enum class SoundCategory(val displayName: String) {
    HARSH("Harsh / Intense"),
    SMOOTH("Smooth / Gentle"),
    PHONE_CALL("Urgent Phone Call")
}

data class SoundItem(
    val id: String,
    val title: String,
    val category: SoundCategory,
    val rawResId: Int? = null,
    val isSystem: Boolean = false,
    val contentUri: String? = null
)
```

Special symbolic tokens stored in `AlarmEntity.customSoundUri`:
- `content://wakewalk/sound/random_harsh` → Randomizes within `HARSH` category
- `content://wakewalk/sound/random_smooth` → Randomizes within `SMOOTH` category
- `content://wakewalk/sound/random_all` → Randomizes across all categories
- `content://wakewalk/sound/call_ringtone` → Device Phone Call Ringtone
- `content://wakewalk/sound/alarm` → Standard System Alarm

### 3.2 Curated Audio Assets (`app/src/main/res/raw/`)
High-efficiency, royalty-free audio tracks (15–25s seamless loops, mono/stereo 48–64 kbps OGG):
- `sound_harsh_siren.ogg` (~110 KB)
- `sound_harsh_bugle.ogg` (~95 KB)
- `sound_harsh_rock.ogg` (~120 KB)
- `sound_harsh_digital.ogg` (~85 KB)
- `sound_smooth_piano.ogg` (~105 KB)
- `sound_smooth_ambient.ogg` (~90 KB)
- `sound_smooth_chimes.ogg` (~85 KB)
- `sound_phone_classic.ogg` (~90 KB)
*Total bundle size:* **~780 KB** (less than 1 MB, keeping APK download fast and lean).

### 3.3 AudioController Enhancement (`AudioController.kt`)
- **`ensureMaxAlarmVolume()`**: Raises `STREAM_ALARM` to `getStreamMaxVolume()`.
- **`duckVolume(target = 0.30f, durationMs = 1500L)`**: Smooth linear/cosine interpolation of MediaPlayer volume.
- **`restoreVolume(target = 1.0f, durationMs = 2000L)`**: Smooth ramp up to full volume.
- **`previewSound(uri: Uri)`** & **`stopPreview()`**: Allows instant sound sampling in the alarm creation screen.

### 3.4 AlarmForegroundService & Anti-Slacking Watchdog
- **Trigger Resolution:** When an alarm rings, `resolveSoundUri(alarm.customSoundUri)` checks if the URI is a random token (`random_harsh`, `random_smooth`, `random_all`). If so, picks a random sound different from `lastPlayedSoundId`.
- **Ducking Condition:**
  ```kotlin
  if (currentSteps >= 10 && !isVolumeDucked && lowerVolumeWhileWalking) {
      isVolumeDucked = true
      audioController.duckVolume(0.30f)
  }
  ```
- **Watchdog Loop:** Every 1 second, checks `now - lastStepTimestampMs`. If `isVolumeDucked && (now - lastStepTimestampMs) > 8000`, restores volume to `1.0f` and updates UI state to `Walking Paused`.

### 3.5 UI & Presentation (`CreateEditAlarmScreen.kt` & `AlarmScreen.kt`)
- **Grouped Sound Picker Dialog / Sheet**:
  - 🎲 **Daily Randomize**:
    - *Shuffle (Harsh / Intense)*
    - *Shuffle (Smooth / Gentle)*
    - *Shuffle (All Sounds)*
  - ⚡ **Harsh / Intense**: Siren, Bugle, Rock, Digital
  - 🌿 **Smooth / Gentle**: Piano, Ambient Sunrise, Chimes
  - 📞 **Urgent Phone Call**: Phone Ringtone (Default), Classic Bell
  - 🔔 **Device Ringtones**: Native Android list
- Each item has a play/pause preview icon.
- **Lockscreen AlarmScreen**:
  - Displays dynamic volume badge:
    - Initial: `🔊 100% Hardcore Volume`
    - At 10 steps: `🚶 Volume Lowered (30%) — Keep Walking!`
    - If paused > 5s: `⚠️ Paused! Volume ramping up in 3s...`

---

## 4. Testing & Verification

1. **Unit Tests:**
   - `AlarmSoundRegistryTest`: Verify category queries and randomization distribution.
   - `AudioControllerTest`: Verify ducking, restore, and volume calculations.
   - `AlarmForegroundServiceTest`: Verify random URI resolution and watchdog timing.
2. **Resource Integrity:**
   - Verify all `.ogg` files load cleanly in Android `MediaPlayer` without codec errors.
3. **Full Test Suite & Build:**
   - Execute `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` to guarantee clean build.
