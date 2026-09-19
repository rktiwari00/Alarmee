# WakeWalk — Walk to Dismiss Alarm System Design Specification

**Date:** 2026-09-19  
**Status:** Approved for Implementation  
**Target Platform:** Android (minSdk 26, compileSdk/targetSdk 35)  
**Language/Framework:** Kotlin, Jetpack Compose, Material 3, Android Architecture Components, Hilt, Room, DataStore  

---

## 0. Prime Engineering Invariant

> **Alarm reliability takes precedence over all other features. A failure of the UI must never terminate the active alarm session, audio, sensor processing, or challenge state.**
> 
> The `AlarmForegroundService` owns the active alarm session; `AlarmActivity` is strictly a presentation client.

---

## 1. Executive Summary & Core Product Principle

**WakeWalk** is an Android alarm application engineered to solve the "half-asleep dismiss" problem: users wake up, dismiss or snooze their alarm unconsciously, and fall back asleep. WakeWalk requires the user to **physically get out of bed and walk a configurable number of steps before the alarm can be dismissed**.

### Core Product Principle
> **The user doesn't simply dismiss the alarm. They earn the right to dismiss it by getting out of bed.**

The normal alarm ringing flow provides **no dismiss button**. The only ways to stop the alarm are:
1. Complete the walking challenge (e.g. 150 validated steps).
2. Use an intentionally constrained, deliberate Emergency Dismissal bypass (typing "I AM AWAKE" or a 5-second continuous long-press).
3. Optional single emergency snooze (5 minutes), if explicitly enabled on that alarm.

Android emergency dialer and system-level power controls are never blocked or interfered with.

---

## 2. High-Level Architecture

The system is structured across four clearly decoupled layers with strict unidirectional data flow:

```text
UI Layer (Jetpack Compose & Material 3)
├── MainActivity (Navigation Compose: Home, Create/Edit, Statistics, Settings, Onboarding)
└── AlarmActivity (Dedicated lockscreen Activity: setShowWhenLocked, setTurnScreenOn)
       │ (Observes StateFlow only; pure presentation client)
       ▼
Session & Domain Layer (Pure Kotlin, Zero Android/DI dependencies)
├── WakeSession (sessionId, alarmId, targetSteps, initialSensorSteps, currentSteps, status)
├── ChallengeEngine (Evaluates candidate steps and emergency dismissal attempts)
├── StepAccumulator (Tracks raw sensor inputs and handles hardware counter resets)
├── MovementValidator & MotionClassifier (Anti-cheat cadence & variance confidence heuristics)
└── EmergencyDismissalPolicy (Validates typed phrase or 5s hold attempt)
       ▲
       │ (Owns and drives the active session)
Platform & Service Layer (Android OS Integration)
├── AlarmManager (setAlarmClock() with AlarmClockInfo)
├── AlarmReceiver (Acquires short-lived partial WakeLock, starts AlarmForegroundService)
├── AlarmForegroundService (Host for active alarm session; FGS type: health)
│    ├── AudioController (MediaPlayer with AudioAttributes.USAGE_ALARM + Audio Focus)
│    ├── VibrationController (Vibrator/VibratorManager cadence patterns)
│    ├── AndroidSensorAdapter (Hardware Step Counter/Detector with Accelerometer fallback)
│    └── NotificationController (High-importance channel, Full-Screen Intent fallback)
└── System Receivers (BootReceiver, TimeChangeReceiver for alarm reconciliation)
       ▲
       ▼
Data Layer (Persistence & DI)
├── WakeWalkDatabase (Room)
│    ├── alarms (AlarmEntity: hour, minute, repeatDaysMask, targetSteps, audio/vibe settings)
│    ├── active_session (ActiveSessionEntity: Singleton row for durable session recovery)
│    └── alarm_history (AlarmHistoryEntity: Atomic historical log with correlated sessionId)
├── UserPreferencesRepository (DataStore: emergency method, theme, default step target)
└── Hilt Dependency Injection (DatabaseModule, RepositoryModule, AppModule)
```

---

## 3. Detailed Component Specifications

### 3.1 Domain & Challenge Engine (Pure Kotlin)

The domain engine contains **zero Android framework dependencies**, enabling comprehensive unit testing on standard JVM.

#### Data Models
- `StepInput`:
  - `CounterUpdate(val timestampNs: Long, val cumulativeSteps: Long)`: Handled via `StepAccumulator` calculating `max(0, cumulativeSteps - baseline)`.
  - `DetectorStep(val timestampNs: Long)`: Discrete hardware step event.
  - `AccelerometerCandidate(val timestampNs: Long, val motion: MotionSnapshot)`: Fallback candidate when hardware step sensors are unavailable.
- `MotionState`: `WALKING`, `STATIONARY`, `SHAKING`, `UNKNOWN`
- `MotionAssessment`: `state: MotionState`, `confidence: Float (0.0 .. 1.0)`, `timestampNs: Long`
- `StepValidationResult`: `accepted: Boolean`, `reason: RejectionReason?`, `confidence: Float`
  - `RejectionReason`: `TOO_FAST`, `SUSPICIOUS_MOTION`, `STATIONARY`, `INSUFFICIENT_EVIDENCE`, `DUPLICATE_EVENT`
- `WakeSession`:
  - `sessionId: String`
  - `alarmId: Long`
  - `targetSteps: Int`
  - `initialSensorSteps: Long`
  - `currentSteps: Int`
  - `status: AlarmStatus` (`SCHEDULED`, `TRIGGERED`, `RINGING`, `CHALLENGE_ACTIVE`, `CHALLENGE_COMPLETED`, `SNOOZED`, `MISSED`, `DISMISSED`, `EMERGENCY_DISMISSED`)
  - `startedAtEpochMs: Long` (Wall clock for history and display)
  - `startedRealtimeNs: Long` (Monotonic timestamp for durations and timer thresholds)
  - `completedAtEpochMs: Long?`
  - `trackingMode: StepTrackingMode` (`HARDWARE_COUNTER`, `HARDWARE_DETECTOR`, `ACCELEROMETER_FALLBACK`, `UNAVAILABLE`)

#### Anti-Cheat Movement Validation Heuristics
`MovementValidationConfig`:
- `minStepIntervalMs: Long = 320L` (Rejects unnatural high-frequency shaking > 3.1 Hz)
- `maxAcceptedStepIntervalMs: Long = 2500L` (Used in fallback classifier to detect pauses vs continuous gait; never rejects a valid future step after a pause)
- `minimumWalkingConfidence: Float = 0.55f`
- `shakeConfidenceThreshold: Float = 0.70f`
- Differentiates cyclical acceleration oscillation (~0.7G to 1.8G) against multi-axis chaotic spikes (> 3.0G across X/Y/Z) to distinguish genuine walking from phone shaking or vibrating surfaces.
- Operates comfortably within standard Android motion sensor sampling rates (< 200 Hz), requiring **no** `HIGH_SAMPLING_RATE_SENSORS` permission.

#### Emergency Dismissal Policy
- `EmergencyDismissalAttempt`:
  - `PhraseConfirmed(val input: String)`: Case-insensitive match against `"I AM AWAKE"`.
  - `LongPressCompleted(val durationMs: Long)`: Verified `>= 5000ms`.

---

### 3.2 Android System Integration & Lifecycle

#### AlarmManager Scheduling & Version Strategy
- Uses `alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTimeEpochMs, showPendingIntent), alarmPendingIntent)`.
- Delivers precise alarm-clock scheduling capable of waking the device from deep Doze.
- `AlarmClockInfo` provides system with alarm-clock metadata and a `showIntent`; system UI may surface alarm information according to device/system behavior (the app does not depend on a specific OEM's icon placement).
- **Exact Alarm Strategy:**
  - API 33+: `<uses-permission android:name="android.permission.USE_EXACT_ALARM" />` (Standard for alarm-clock apps, pre-granted).
  - API 31–32 fallback: Handles `SCHEDULE_EXACT_ALARM` where required by platform compatibility.

#### WakeLock & 5-Second Service Promotion Deadline
- `AlarmReceiver.onReceive()` acquires a `PARTIAL_WAKE_LOCK` with a maximum safety timeout of 10 seconds.
- It immediately calls `ContextCompat.startForegroundService()`.
- `AlarmForegroundService.onCreate() / onStartCommand()` immediately calls `ServiceCompat.startForeground()` with a high-priority notification well within the mandatory Android 5-second deadline.
- The receiver wake lock is released immediately once the service promotes. The service does **not** hold an indefinite wake lock.

#### Android 14/15 Foreground Service Type & Permissions
- Declares `android:foregroundServiceType="health"` in `AndroidManifest.xml`.
- Declares `android.permission.FOREGROUND_SERVICE` and `android.permission.FOREGROUND_SERVICE_HEALTH`.
- Runtime prerequisite: `android.permission.ACTIVITY_RECOGNITION` requested during onboarding and settings diagnostics.
- *Policy Note:* Verification that the final Play-distributed use of the health foreground-service type aligns with Google's active foreground service policies is noted for release preparation.

#### Audio & Vibration Subsystem
- Audio uses `MediaPlayer` with `AudioAttributes`:
  - `usage = AudioAttributes.USAGE_ALARM`
  - `contentType = AudioAttributes.CONTENT_TYPE_SONIFICATION`
- Requests Audio Focus with `AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(...)`.
- Loops alarm sound continuously until the `ChallengeEngine` reaches completion or verified emergency dismissal.
- Gradual volume ramp: Optional 10-second volume increase from 20% to 100%.
- Vibration uses `Vibrator` / `VibratorManager` with repeating waveform pulses.
- Distinct milestone haptics on 25%, 50%, 75% steps, and triumphant vibration on 100% completion.

#### Full-Screen Intent (FSI) & Fallback Handling
- On Android 14+ (API 34+), checks `notificationManager.canUseFullScreenIntent`.
- If true: Launches `AlarmActivity` via full-screen intent over the lock screen (`setShowWhenLocked(true)`, `setTurnScreenOn(true)`).
- If false or denied: Posts a high-importance alarm notification with a prominent "Tap to Open Challenge" action, and provides settings guidance inside the main app.
- **Critical rule:** Alarm correctness does **not** depend on `AlarmActivity` being visible. The service independently handles audio, sensor validation, step accumulation, and completion.

#### Sensor Lifecycle & Screen-Off / Pocket Walking
- Sensor listeners (`TYPE_STEP_COUNTER`, `TYPE_STEP_DETECTOR`, `TYPE_ACCELEROMETER`) are registered exclusively when `AlarmForegroundService` starts an active session.
- If the user turns off the screen or puts the phone in their pocket while walking, the foreground service keeps sensor collection active and audio playing.
- `SensorAdapter.stop()` is fully idempotent and called immediately upon session completion or service destruction, preventing battery drain.

---

### 3.3 Data Layer & Persistence

#### Room Database (`WakeWalkDatabase`)

1. **`alarms` (`AlarmEntity`)**:
   - `id: Long` (PK, auto-generated)
   - `hour: Int`, `minute: Int`
   - `label: String`
   - `isEnabled: Boolean`
   - `repeatDaysMask: Int` (7-bit mask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64)
   - `challengeType: ChallengeType` (Room TypeConverter)
   - `targetSteps: Int` (50–2000)
   - `soundUri: String?`
   - `vibrationEnabled: Boolean`
   - `gradualVolume: Boolean`
   - `snoozeEnabled: Boolean`
   - `snoozeDurationMinutes: Int` (default 5)
   - `createdAtEpochMs: Long`, `updatedAtEpochMs: Long`

2. **`active_session` (`ActiveSessionEntity`)**:
   - `singletonId: Int = 1` (Enforces one active challenge session at a time)
   - `sessionId: String`
   - `alarmId: Long`
   - `targetSteps: Int`
   - `initialSensorSteps: Long`
   - `currentSteps: Int`
   - `startedAtEpochMs: Long`
   - `startedRealtimeNs: Long`
   - `status: AlarmStatus` (Room TypeConverter)
   - `lastUpdatedEpochMs: Long`

   *Session Durability & Collision Policy:*
   - Every accepted step and state transition is serialized and persisted to Room.
   - If the alarm service/process is recreated while a session remains active, the persisted `ActiveSessionEntity` enables deterministic reconstruction without resetting challenge progress. (The application does not assume Android will automatically recreate the service after arbitrary process termination).
   - **One-Active-Session Collision Policy:** Only one active `WakeSession` is permitted. If another alarm fires while a `WakeSession` is active: do not overwrite the active session; record the new alarm execution appropriately; avoid starting a second challenge/audio engine.

3. **`alarm_history` (`AlarmHistoryEntity`)**:
   - `id: Long` (PK, auto-generated)
   - `sessionId: String` (Correlation identifier)
   - `alarmId: Long`
   - `scheduledTimeEpochMs: Long`
   - `triggeredAtEpochMs: Long?`
   - `completedAtEpochMs: Long?`
   - `status: AlarmStatus` (`COMPLETED`, `EMERGENCY_DISMISSED`, `SNOOZED`, `MISSED`)
   - `targetSteps: Int`
   - `completedSteps: Int`
   - `durationSeconds: Long?`
   - `emergencyDismissed: Boolean`

   *Idempotent Atomic Completion Transaction:*
   ```kotlin
   // Guarded against double teardown from concurrent step completion and emergency action
   suspend fun completeSession(historyEntity: AlarmHistoryEntity) {
       db.withTransaction {
           alarmHistoryDao.insert(historyEntity)
           activeSessionDao.clearActiveSession()
       }
   }
   ```
   Followed by `AlarmSessionCoordinator`: stop sensors (idempotent), stop vibration, stop audio, abandon audio focus, post completion notification/state, and stop foreground service.

#### DataStore (`UserPreferencesRepository`)
- `emergencyDismissalMethod`: `TYPING_PHRASE` (default) vs `LONG_PRESS_5S`
- `defaultStepTarget`: 150
- `theme`: `SYSTEM` (default), `LIGHT`, `DARK`
- `onboardingCompleted`: Boolean

#### Reconciliation & Source of Truth
- Room is the authoritative source of truth.
- `AlarmScheduler.reconcileAlarms()` runs on app startup, system boot (`BOOT_COMPLETED`), and time changes (`TIME_SET`, `TIMEZONE_CHANGED`) to verify and repair AlarmManager schedules against enabled database rows.

---

### 3.4 User Interface & Jetpack Compose Screens

#### 1. Navigation Architecture
- **`MainActivity`:** Hosts `NavHost` containing:
  - `OnboardingScreen`: 4-step introductory flow explaining physical walking requirement, permissions, and initial target configuration.
  - `HomeScreen`: Greeting, active streak banner, next scheduled alarm countdown pill, list of alarm cards with toggles, and FAB to add alarm.
  - `CreateEditAlarmScreen`: Time picker, label, 7-chip repeat days selector, quick step target chips (50, 100, 150, 200, 250, 300, 500) + custom slider, sound, vibration, and snooze options.
  - `StatisticsScreen`: Weekly overview (distinguishing COMPLETED from EMERGENCY_DISMISSED and MISSED), streak indicator, average completion duration, and history list.
  - `SettingsScreen`: Emergency dismissal method selector, theme selector, privacy commitment, and Permission & Hardware Diagnostics (distinguishing permission granted from hardware sensor availability).
- **`AlarmActivity` (Dedicated Lockscreen Alarm UI):**
  - Strictly a presentation client observing `WakeSessionRepository`.
  - Back navigation, drawers, and swipe gestures are disabled.
  - High-contrast OLED dark theme with vibrant amber/emerald accents.
  - Giant digital time and alarm label.
  - Large step progress: `83 / 150` with segmented milestone indicator (`0 ●──── 50 ●──── 100 ●──── 150 ●`) and smooth progress bar.
  - Dynamic status text: "Keep walking...", with anti-cheat feedback only when suspicious movement is detected.
  - Sensor-unavailable notice when hardware sensors are absent and basic fallback is active.
  - Visually subordinate "Emergency Stop" button opening the safety dialog.
  - On completion: Service stops audio and records history immediately; Activity displays result card ("✓ You're awake! 153 steps in 1m 48s. Good morning ☀️") and finishes.

---

## 4. Verification & Testing Strategy

1. **Pure Domain Unit Tests (JVM):**
   - `StepAccumulatorTest`: Baseline calculation for `TYPE_STEP_COUNTER`, counter resets, discrete `TYPE_STEP_DETECTOR` increments.
   - `MovementValidatorTest`: Human walking frequency vs rapid shaking (>3.1 Hz) rejection, stationary rejection, variance evaluation.
   - `ChallengeEngineTest`: Step progression, session completion, emergency phrase verification, long-press timer validation.
   - `AlarmSchedulerTest`: Next alarm calculation across midnight, weekdays, weekends, leap days, and daylight saving shifts.
2. **Persistence Unit Tests (Room with In-Memory Database):**
   - Transactional atomic completion test (history insert + active session delete).
   - Session recovery test: Step 73 / 150 persisted → service interrupted → service recreated → resumes at 73 / 150.
   - Crash/race test: Step 149 / 150 completion transaction with simulated concurrent emergency action → exactly one history record, zero active session rows.
3. **Android Lifecycle & Service Tests:**
   - Verify `startForeground()` called within 5 seconds.
   - Verify sensor unregistration on session termination (zero leak).
   - Verify AlarmReceiver short wake lock release.
4. **UI Tests (Compose Rule):**
   - Alarm creation flow and time picker formatting.
   - Alarm screen step counter animations and emergency dialog behavior.
