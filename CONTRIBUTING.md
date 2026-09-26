# Contributing to WakeWalk

Thank you for your interest in contributing to **WakeWalk**! We welcome contributions ranging from bug fixes and device calibration heuristics to new wake-up challenges and localization.

---

## 🛠️ Development Setup

### Prerequisites
1. **Android Studio Ladybug** (or later)
2. **JDK 17** (e.g. Eclipse Temurin or Microsoft OpenJDK)
3. **Android SDK 35** and Platform Tools

### Cloning and Building
```bash
git clone https://github.com/rktiwari00/Alarmee.git
cd Alarmee
```

Run unit tests:
```bash
./gradlew testDebugUnitTest
```

Build a debug APK:
```bash
./gradlew assembleDebug
```
The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🏛️ Architecture & Guidelines

WakeWalk follows Clean Architecture principles with strict 4-tier separation:
1. **Domain Layer (`domain/`)**: Pure Kotlin. Zero Android dependencies. All anti-cheat logic, step accumulation, time calculation, and challenge state transitions belong here and are 100% JVM unit-tested.
2. **Persistence Layer (`data/`)**: Room Database (Alarms, ActiveSession, AlarmHistory) and DataStore (UserPreferences).
3. **Platform Layer (`platform/`)**: AlarmManager (`setAlarmClock`), Foreground Services (`AlarmForegroundService`), Audio (`USAGE_ALARM`), and Sensors.
4. **Presentation Layer (`ui/`)**: Jetpack Compose and Material 3 with OLED-friendly dark styling.

### The Prime Invariant
> **Alarm reliability takes precedence over all other features.**
> Any new UI, animation, or challenge must never compromise the ability of `AlarmForegroundService` to keep playing audio or counting steps over the lockscreen.

---

## 🧪 Testing Guidelines
- All domain rules and state machines must include unit tests under `app/src/test/`.
- Ensure `./gradlew testDebugUnitTest` passes before opening a Pull Request.

---

## 📬 Submitting a Pull Request
1. Fork the repo and create your feature branch: `git checkout -b feat/my-new-feature`
2. Commit your changes with clear semantic messages (`feat: ...`, `fix: ...`, `docs: ...`)
3. Ensure all unit tests pass
4. Open a Pull Request against `master` describing your changes
