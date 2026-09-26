# WakeWalk — Walk to Dismiss Alarm (Android)

[![Website](https://img.shields.io/badge/Website-Visit%20Official%20Site-6366F1?style=for-the-badge&logo=googlechrome&logoColor=white)](https://rktiwari00.github.io/Alarmee/)
[![Latest Release](https://img.shields.io/github/v/release/rktiwari00/Alarmee?style=for-the-badge&color=10B981)](https://github.com/rktiwari00/Alarmee/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-Donate-yellow?style=for-the-badge&logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/rktiwari00)

**🌐 Official Website & Live Simulator:** [https://rktiwari00.github.io/Alarmee/](https://rktiwari00.github.io/Alarmee/)

WakeWalk is a modern, reliable Android alarm application designed for disciplined mornings. When an alarm rings, it requires the user to physically get out of bed and walk a designated number of steps (e.g., 150 steps) or scan a registered household barcode before it can be dismissed. The ringing screen has **no dismiss button**.

---

## 🏛️ Architecture Overview

WakeWalk is engineered following **Clean Architecture** principles and Android modern architecture guidelines with a strict 4-tier separation:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation (UI)                        │
│    Jetpack Compose • Material 3 • OLED Dark Theme           │
│    MainActivity (Home, Create/Edit, Stats, Settings)        │
│    AlarmActivity (Dedicated Lockscreen Client, No Nav)      │
└──────────────────────────────┬──────────────────────────────┘
                               │ Observes Flows / Dispatches Intents
┌──────────────────────────────▼──────────────────────────────┐
│                  Platform & Services Layer                  │
│    AlarmManager (setAlarmClock) • AlarmReceiver             │
│    AlarmForegroundService (Health FGS, owns session/sensors)│
│    AudioController (USAGE_ALARM) • VibrationController       │
│    AndroidSensorAdapter (Hardware Counter/Detector/Fallback)│
└──────────────────────────────┬──────────────────────────────┘
                               │ Implements / Uses
┌──────────────────────────────▼──────────────────────────────┐
│                    Persistence Layer                        │
│    Room Database (WakeWalkDatabase)                         │
│      ├── AlarmEntity & AlarmDao                             │
│      ├── ActiveSessionEntity & ActiveSessionDao (Singleton) │
│      └── AlarmHistoryEntity & AlarmHistoryDao               │
│    Preferences DataStore (UserPreferencesRepository)        │
└──────────────────────────────┬──────────────────────────────┘
                               │ Implements Domain Interfaces
┌──────────────────────────────▼──────────────────────────────┐
│                   Domain Layer (Pure Kotlin)                │
│    Zero Android Dependencies • 100% JVM Testable            │
│    WakeSession • StepAccumulator • AlarmTimeCalculator      │
│    MovementValidator • ValidationConfig • ChallengeEngine   │
│    EmergencyDismissalPolicy                                 │
└─────────────────────────────────────────────────────────────┘
```

### The Prime Invariant
> **Alarm reliability takes precedence over all other features.**
> UI crashes, Activity recreation, or configuration changes must NEVER terminate an active ringing alarm, audio playback, sensor processing, or challenge state. The `AlarmForegroundService` owns the lifecycle of the alarm session; `AlarmActivity` is strictly a presentation observer.

---

## ⚡ Android 12–15 Reliability & Permissions

WakeWalk implements precise compliance with Android 12 through Android 15 platform requirements:

| Capability / Requirement | Implementation Details |
|---|---|
| **Exact Alarm Scheduling** | Declares `android.permission.USE_EXACT_ALARM` and invokes `AlarmManager.setAlarmClock()`. Bypasses Doze mode and displays the user-visible alarm icon in the system status bar and lockscreen. |
| **Foreground Service (FGS)** | Declares `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_HEALTH`. Promotes to foreground via `ServiceCompat.startForeground()` immediately upon creation (< 5 seconds) with high-importance notification. |
| **Full-Screen Intent (FSI)** | Uses `USE_FULL_SCREEN_INTENT` on the ringing notification channel, launching `AlarmActivity` over the lockscreen when the screen is off or locked. |
| **Lockscreen Wakeup** | `AlarmActivity` configures `setShowWhenLocked(true)`, `setTurnScreenOn(true)`, and `FLAG_KEEP_SCREEN_ON`. Locks orientation to portrait and intercepts the back gesture. |
| **Process Death vs Force-Stop** | In normal process death or service recreation, the singleton `ActiveSessionEntity` in Room preserves exact step progress and timestamp state. (Under user-initiated force-stop, Android intentionally holds apps in a stopped state until manual launch). |
| **Reboot & Timezone Rescheduling** | `BootReceiver` and `TimeChangeReceiver` reconcile and reschedule all active alarms using `AlarmTimeCalculator`. Receivers never initiate active foreground ringing sessions on boot. |

---

## 🚶 Step Detection & Anti-Cheat Validation

### 1. Multi-Tier Sensor Pipeline
1. **Hardware Step Counter (`TYPE_STEP_COUNTER`):** Tracks cumulative steps since device boot. `StepAccumulator` calibrates against the initial baseline and cleanly handles device reboots or counter rollovers.
2. **Hardware Step Detector (`TYPE_STEP_DETECTOR`):** Detects discrete steps on devices lacking a hardware step counter.
3. **Accelerometer Fallback (`TYPE_ACCELEROMETER`):** Evaluates multi-axis 3D acceleration vectors when hardware step sensors are absent.

### 2. Anti-Cheat Validation (Tunable Heuristics)
All validation thresholds in `ValidationConfig` represent **initial tunable heuristics** for differentiating authentic human walking gait from rapid shaking or bed vibrations, and are open for calibration:
- **Interval Check:** Rejects candidate steps arriving faster than `minStepIntervalMs = 320ms` (initial heuristic flagging cadence > 3.1 Hz).
- **Pause Handling:** `maxAcceptedStepIntervalMs = 2500ms` maintains cadence state without penalizing or discarding steps after a user pauses.
- **Magnitude & Variance:** Cyclical acceleration oscillations (~0.7G to 1.8G) are accepted; chaotic multi-axis spikes (> 3.2G) or stationary resting vibration are flagged as suspicious.
- **Sampling Rate:** Operates within standard Android motion sensor sampling rates (< 200 Hz), requiring no high-frequency sensor permissions.

---

## 🚨 Emergency Dismissal Safety Fallback

WakeWalk provides an intentional safety mechanism for medical emergencies, injuries, or unexpected situations while preventing half-asleep accidental dismissals:

1. **Type "I AM AWAKE" (Default):**
   - Requires typing the exact phrase (case-insensitive).
   - The "Stop Alarm" button remains disabled until the phrase is fully verified.
2. **5-Second Continuous Long Press (Configurable):**
   - Requires continuous uninterrupted touch for 5.0 seconds.
   - Releasing the button before 5 seconds resets the progress timer to zero.
3. **Safety Guarantee:**
   - Power button, volume keys, and system Emergency SOS calls remain completely unobstructed at all times.

---

## 🧪 Testing & Verification

The project includes an extensive suite of unit tests verifying all critical layers:

### 1. Run Domain & Persistence Unit Tests
```bash
./gradlew testDebugUnitTest
```
- `StepAccumulatorTest`: Baseline subtraction, delta calculation, and counter rollover.
- `MovementValidatorTest`: Gait cadence, rapid shaking rejection, and confidence filtering.
- `ChallengeEngineTest`: State transitions (`RINGING` → `CHALLENGE_ACTIVE` → `CHALLENGE_COMPLETED`), emergency dismissal, and snooze.
- `AlarmTimeCalculatorTest`: One-time alarms, everyday repeat, weekday/weekend masks, and midnight rollover.
- `RoomDatabaseTest`: Entity mapping, TypeConverters, singleton active session, and atomic completion transactions.
- `WakeSessionRepositoryTest`: Recovery from Room, session updates, and cleanup.
- `StatisticsRepositoryTest`: Streak calculation and aggregate metrics.

### 2. Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be available at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Hardware Diagnostics Dashboard

Under the in-app **Settings** tab, WakeWalk includes a live diagnostics dashboard that inspects:
- **Exact Alarm Capability:** Confirms `AlarmManager.canScheduleExactAlarms()` status.
- **Notification & FSI Status:** Validates notification channel settings for lockscreen display.
- **Motion Sensors:** Detects whether hardware step sensors or accelerometer fallback is active.
- **Battery Optimization:** Checks whether the app is excluded from aggressive background battery restrictions.

---

## 🔒 Privacy Commitment

- **100% On-Device:** WakeWalk collects zero analytics, has no user tracking, and transmits no data to external servers.
- **Local Storage:** All alarms, historical stats, and preferences are stored exclusively in local Room and DataStore databases.

---

## 📄 License

This project is open-source under the [MIT License](LICENSE).
