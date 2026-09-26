# Design Specification: 100% Hardcore Volume & Dynamic Walking Volume Ducking

**Date:** 2026-09-26  
**Status:** Approved  
**Topic:** Hardcore 100% Alarm Volume with Dynamic Walking Ducking and Anti-Slacking Watchdog  

---

## 1. Overview & Goals

When an alarm triggers in WakeWalk, deep sleepers need maximum sound intensity to overcome sleep inertia. However, once the user gets out of bed and actively walks, continuing to blast the audio at full volume causes ear discomfort and unnecessarily wakes roommates or family members.

This feature introduces:
1. **100% Hardcore Volume on Start:** The alarm guarantees the system `STREAM_ALARM` is elevated to device maximum (100%) when ringing starts.
2. **Dynamic Volume Ducking:** Once the user completes **10 verified steps**, the alarm audio smoothly ducks from 100% down to a gentle **30% volume** over 1.5 seconds.
3. **Anti-Slacking Watchdog:** If the user stops walking before reaching their full step target (e.g. they sit down or return to bed) and no authentic steps are detected for **8 seconds**, the audio volume ramps back up to **100%** with a clear warning: *"Walking paused — volume ramping up!"*. Resuming walking immediately restores the lowered volume.
4. **Configurability:** Enabled by default, with an opt-out toggle in Settings and Alarm Creation.

---

## 2. Architecture & Components

```
┌─────────────────────────────────────────────────────────────┐
│                 AlarmForegroundService                      │
│   • Tracks currentSteps, lastStepTimestampMs                │
│   • Runs Active Walking Watchdog (checks every 1s)          │
│   • Emits VolumeState (FULL, DUCKED, RESTORING)             │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
               ▼                               ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│       AudioController        │ │        AlarmActivity         │
│   • ensureMaxAlarmVolume()   │ │   • Visual indicator banner  │
│   • duckVolume(0.30f)        │ │   • "🚶 Volume lowered"      │
│   • restoreVolume(1.0f)      │ │   • "⚠️ Keep moving!"        │
│   • Smooth coroutine ramping │ │                              │
└──────────────────────────────┘ └──────────────────────────────┘
```

---

## 3. Detailed Component Specifications

### 3.1 AudioController (`app.wakewalk.alarm.audio.AudioController`)
- **`ensureMaxAlarmVolume()`:**
  ```kotlin
  val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
  audioManager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
  ```
  Guarantees the system alarm channel is operating at true 100% maximum capacity.
- **`duckVolume(targetVolume = 0.30f, durationMs = 1500L)`:**
  Smoothly interpolates MediaPlayer volume from current level down to `targetVolume` in 10 micro-steps. Cancels any active restore/ramp jobs.
- **`restoreVolume(targetVolume = 1.0f, durationMs = 2000L)`:**
  Smoothly ramps MediaPlayer volume from current level back up to 1.0f in fine increments.
- **`isDucked: Boolean`:**
  Exposes whether audio is currently attenuated.

### 3.2 AlarmForegroundService (`app.wakewalk.alarm.service.AlarmForegroundService`)
- **Walking State Properties:**
  - `private var isVolumeDucked = false`
  - `private var lastStepEpochMs = 0L`
  - `private var walkingWatchdogJob: Job? = null`
- **On Step Accepted:**
  - Updates `lastStepEpochMs = System.currentTimeMillis()`.
  - If `lowerVolumeWhileWalking == true` and `currentSteps >= 10`:
    - If `!isVolumeDucked`, triggers `audioController.duckVolume(0.30f)` and sets `isVolumeDucked = true`.
    - Updates notification / UI state to inform the user that volume is lowered.
- **Active Movement Watchdog:**
  - Launches a coroutine loop running every `1000ms`:
    ```kotlin
    while (isActive && isRinging) {
        delay(1000L)
        val now = System.currentTimeMillis()
        if (isVolumeDucked && (now - lastStepEpochMs) >= 8000L) {
            isVolumeDucked = false
            audioController.restoreVolume(1.0f)
            notifyWalkingPaused()
        }
    }
    ```

### 3.3 Persistence & Settings (`UserPreferences` & `AlarmEntity`)
- Add `lowerVolumeWhileWalking: Boolean = true` to `UserPreferences`.
- Add `lowerVolumeWhileWalking: Boolean = true` to `AlarmEntity`.
- Add UI toggle in `CreateEditAlarmScreen.kt` and `SettingsScreen.kt`:
  - Title: *"Lower volume while walking"*
  - Description: *"Ducks to 30% volume after 10 steps; ramps back up if you stop moving."*

### 3.4 Presentation UI (`AlarmScreen.kt`)
- When `isVolumeDucked == true`:
  - Displays a pill badge: `🟢 30% Volume (Walking Active)`
- When walking pauses (>4s):
  - Displays a warning badge: `⚠️ Keep moving to keep volume low!`

---

## 4. Error Handling & Edge Cases

1. **Volume Key Interception:** While alarm is active, hardware volume keys are suppressed or re-elevated to prevent premature manual muting.
2. **Fallback Ringtone Mode:** If `MediaPlayer` fails and fallback `Ringtone` is active, volume is handled via `STREAM_ALARM` scaling.
3. **Alarm Completion:** When the final target step is reached, `stopAudio()` immediately terminates all playback and releases focus.
4. **Emergency Dismissal:** In emergency mode ("I AM AWAKE" or 5s hold), audio is instantly silenced.

---

## 5. Verification Plan

1. **Unit Tests (`AudioControllerTest.kt`):**
   - Verify `ensureMaxAlarmVolume()` calls `audioManager.setStreamVolume` with max volume.
   - Verify `duckVolume()` and `restoreVolume()` adjust player volume without exceptions.
2. **Unit Tests (`AlarmForegroundServiceTest` / `ChallengeEngineTest`):**
   - Verify step milestone transitions (0 → 9 steps: volume 100%, 10 steps: ducking triggered).
   - Verify 8s inactivity watchdog restores full volume.
3. **Build & Integration Test:**
   - Execute `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` to confirm clean compilation and test passes.
