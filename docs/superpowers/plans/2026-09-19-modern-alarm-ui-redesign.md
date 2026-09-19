# Modern Alarm UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the modern Simpl iOS / Pinterest-inspired design across WakeWalk (Canary Yellow `#FFE500` accents, hero digital clock, 24h timeline waveform, modern alarm cards with ringtone preview, floating pill bar, dual-column curved time picker, and refined alarm challenge screen) while maintaining 100% reliable background alarm execution, step counting, and anti-cheat protection.

**Architecture:** Jetpack Compose with Material 3 design tokens. Pure Kotlin domain logic and Room database operations remain unchanged. UI layer adds custom canvas-based timeline waveform and curved time picker components, integrated with existing `HomeViewModel`, `CreateEditAlarmViewModel`, and `AlarmViewModel`.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose, Material 3, AndroidX Lifecycle, Hilt, Room, Coroutines & Flow.

## Global Constraints

- Preserve all sensor, background service, and alarm execution mechanics (`AlarmForegroundService`, `AndroidSensorAdapter`, `AudioController`).
- Set `dynamicColor = false` by default in `WakeWalkTheme` so the Canary Yellow signature brand palette is guaranteed across Android 12–15.
- Keep all unit tests passing (`./gradlew.bat testDebugUnitTest`).
- All Gradle commands executed with Process `JAVA_HOME` pointing to `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot`.

---

### Task 1: Color Tokens & Theme Foundation

**Files:**
- Modify: `app/src/main/java/app/wakewalk/ui/theme/Color.kt`
- Modify: `app/src/main/java/app/wakewalk/ui/theme/Theme.kt`

**Interfaces:**
- Produces: `CanaryYellow`, `CanaryYellowContainer`, `CharcoalDark`, `CharcoalSurface`, `CharcoalSurfaceVariant`, `OffWhiteBackground`, `OffWhiteSurface`, `NeutralTickLight`, `NeutralTickDark`

- [ ] **Step 1: Update Color.kt with modern Pinterest-inspired palette**

Update `app/src/main/java/app/wakewalk/ui/theme/Color.kt` to define:
```kotlin
package app.wakewalk.ui.theme

import androidx.compose.ui.graphics.Color

// Canary Yellow Accent
val CanaryYellow = Color(0xFFFFE500)
val CanaryYellowHover = Color(0xFFE6CE00)
val CanaryYellowContainer = Color(0xFFFFF9B8)
val CanaryYellowOnContainer = Color(0xFF242100)

// Modern Dark / OLED Obsidian Palette
val CharcoalDark = Color(0xFF111827)
val CharcoalSurface = Color(0xFF161820)
val CharcoalSurfaceVariant = Color(0xFF222530)
val DarkBackground = Color(0xFF0D0E12)
val DarkSurface = Color(0xFF161820)
val DarkSurfaceVariant = Color(0xFF222530)
val DarkPrimary = CanaryYellow
val DarkOnPrimary = Color(0xFF111827)
val DarkPrimaryContainer = Color(0xFF332D00)
val DarkOnPrimaryContainer = CanaryYellow
val DarkSecondary = Color(0xFF30D158)
val DarkOnSecondary = Color(0xFF003913)
val DarkSecondaryContainer = Color(0xFF00531E)
val DarkOnSecondaryContainer = Color(0xFF74F88B)
val DarkError = Color(0xFFFF453A)
val DarkOnError = Color(0xFF410002)

// Modern Light Palette
val OffWhiteBackground = Color(0xFFF5F6F8)
val OffWhiteSurface = Color(0xFFFFFFFF)
val LightBackground = OffWhiteBackground
val LightSurface = OffWhiteSurface
val LightSurfaceVariant = Color(0xFFE5E7EB)
val LightPrimary = Color(0xFF111827)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = CanaryYellow
val LightOnPrimaryContainer = Color(0xFF111827)
val LightSecondary = Color(0xFF1B873F)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFA6F5B9)
val LightOnSecondaryContainer = Color(0xFF00210A)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)

// Timeline & Widget Colors
val NeutralTickLight = Color(0xFFD1D5DB)
val NeutralTickDark = Color(0xFF374151)
val ActiveTickDark = Color(0xFF111827)
val ActiveTickLight = Color(0xFFFFFFFF)
```

- [ ] **Step 2: Update Theme.kt to default dynamicColor = false**

In `app/src/main/java/app/wakewalk/ui/theme/Theme.kt`:
Set `dynamicColor: Boolean = false` by default so the signature Canary Yellow brand identity is always active.

- [ ] **Step 3: Run unit tests to verify theme compilation**

Run: `powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest }"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/app/wakewalk/ui/theme/Color.kt app/src/main/java/app/wakewalk/ui/theme/Theme.kt
git commit -m "style: add Canary Yellow design tokens and default dynamicColor to false"
```

---

### Task 2: 24-Hour Timeline Waveform & Weekday Selector Components

**Files:**
- Create: `app/src/main/java/app/wakewalk/ui/components/TimelineWaveform.kt`
- Create: `app/src/main/java/app/wakewalk/ui/components/WeekdaySelectorRow.kt`
- Modify: `app/src/main/java/app/wakewalk/ui/components/RepeatDaySelector.kt`

**Interfaces:**
- Produces:
  - `TimelineWaveform(alarms: List<AlarmEntity>, nextAlarmMinuteOfDay: Int?, modifier: Modifier)`
  - `WeekdaySelectorRow(selectedDay: DayOfWeek, onDaySelected: (DayOfWeek) -> Unit, modifier: Modifier)`

- [ ] **Step 1: Create TimelineWaveform.kt**

Implement canvas-based 24h timeline bar graph:
- 48 tick marks across the horizontal width representing 30-minute blocks (or 24 hours).
- Neutral ticks for empty intervals.
- Dark / high-contrast bold ticks for scheduled alarms.
- Glowing Canary Yellow tick marker for the next imminent alarm.

- [ ] **Step 2: Create WeekdaySelectorRow.kt**

Implement horizontal row of 7 circular weekday chips (`Mon`, `Tue`, `Wed`, `Thu`, `Fri`, `Sat`, `Sun`):
- Active day is rendered in a solid `CanaryYellow` circular container with bold black font.
- Inactive days have transparent background with medium-weight text.

- [ ] **Step 3: Update RepeatDaySelector.kt to use the Canary Yellow circular styling**

Align existing alarm creation day selection with the new circular Canary Yellow chips.

- [ ] **Step 4: Verify compilation & tests**

Run: `powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest }"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/wakewalk/ui/components/
git commit -m "feat: add 24h timeline waveform and modern weekday selector components"
```

---

### Task 3: Modern Alarm Card with Audio Preview

**Files:**
- Modify: `app/src/main/java/app/wakewalk/ui/components/AlarmCard.kt`
- Modify: `app/src/main/java/app/wakewalk/ui/home/HomeViewModel.kt`

**Interfaces:**
- Consumes: `AudioController` for safe ringtone sample previewing.
- Produces: `AlarmCard` with Play `▶` button, large digital typography, challenge badges, and toggle switch.

- [ ] **Step 1: Add sound preview state to HomeViewModel**

In `HomeViewModel.kt`, add:
- `val playingAlarmId: StateFlow<Long?>`
- `fun toggleSoundPreview(alarmId: Long)`: Starts playing default ringtone via `AudioController` or stops if already playing. Stops automatically after 5 seconds.
- Ensure audio stops when ViewModel clears.

- [ ] **Step 2: Redesign AlarmCard.kt matching Pinterest Image 4**

In `AlarmCard.kt`:
- Large digital time display (`09:50`) with superscript/small `AM` / `PM`.
- Metadata subtitle: `🚶 150 steps • Gentle Wake`.
- Play preview icon button: Circular dark button with `▶` (or `■` when playing).
- Minimalist switch toggle.
- Clean rounded card corners (24.dp).

- [ ] **Step 3: Verify with unit tests**

Run: `powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest }"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/app/wakewalk/ui/components/AlarmCard.kt app/src/main/java/app/wakewalk/ui/home/HomeViewModel.kt
git commit -m "feat: redesign AlarmCard with play preview button and modern typography"
```

---

### Task 4: Home Screen & Floating Bottom Navigation Pill

**Files:**
- Create: `app/src/main/java/app/wakewalk/ui/components/FloatingBottomPill.kt`
- Modify: `app/src/main/java/app/wakewalk/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/app/wakewalk/ui/navigation/WakeWalkNavigation.kt`

**Interfaces:**
- Produces: Modern Home screen layout with hero clock, next alarm countdown, 24h timeline, weekday chips, card list, and floating bottom pill navigation.

- [ ] **Step 1: Create FloatingBottomPill.kt**

Implement a floating capsule docked at the bottom center of the screen:
- Left: Calendar/Alarm icon button.
- Center: Time duration / next alarm pill (`09:41 - 10:00` or status).
- Right: Canary Yellow or dark circular `+` button to create alarm.

- [ ] **Step 2: Redesign HomeScreen.kt**

Update `HomeScreen.kt`:
- Top "Alarm" header with subtle dropdown chevron.
- Hero digital clock (`09:50`) with large bold hour and medium-toned minute digits.
- Subtitle: *"The next alarm clock in **X min**"*.
- `TimelineWaveform` embedded beneath the countdown.
- `WeekdaySelectorRow` for quick filtering/viewing.
- Modern `AlarmCard` list.

- [ ] **Step 3: Update WakeWalkNavigation.kt**

Integrate the `FloatingBottomPill` smoothly with scaffold insets and navigation routes.

- [ ] **Step 4: Verify with unit tests**

Run: `powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest }"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/wakewalk/ui/components/FloatingBottomPill.kt app/src/main/java/app/wakewalk/ui/home/HomeScreen.kt app/src/main/java/app/wakewalk/ui/navigation/WakeWalkNavigation.kt
git commit -m "feat: integrate hero clock, timeline waveform, and floating bottom pill on HomeScreen"
```

---

### Task 5: Modern Create / Edit Alarm Screen

**Files:**
- Create: `app/src/main/java/app/wakewalk/ui/components/CurvedTimePicker.kt`
- Modify: `app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmScreen.kt`

**Interfaces:**
- Consumes: `CreateEditAlarmViewModel`.
- Produces: Dual-column curved scrolling time picker, quick setting action pills, and bottom bar with `✕` and `✔`.

- [ ] **Step 1: Create CurvedTimePicker.kt**

Implement the dual-column curved time picker from Image 3:
- Hour column (`01`–`12` / `00`–`23`) and Minute column (`00`–`59`).
- Central magnification/focus styling with subtle curved background container.

- [ ] **Step 2: Redesign CreateEditAlarmScreen.kt**

Update `CreateEditAlarmScreen.kt`:
- Top title and curved time picker.
- Three quick setting action pills:
  - `🎵 Sound`: Tone selection.
  - `🔔 Snooze / Steps`: Challenge step goal (50, 100, 150, 200).
  - `🔁 Repeat`: Weekday selection.
- Bottom bar: `✕` Cancel on left, "Choose time" in center, circular black `✔` Save button on right.

- [ ] **Step 3: Verify with unit tests**

Run: `powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest }"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/app/wakewalk/ui/components/CurvedTimePicker.kt app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmScreen.kt
git commit -m "feat: implement dual-column curved time picker and modern create alarm screen"
```

---

### Task 6: Modern Active Ringing & Step Challenge Screen

**Files:**
- Modify: `app/src/main/java/app/wakewalk/ui/alarm/AlarmScreen.kt`

**Interfaces:**
- Consumes: `AlarmViewModel`, `StepTrackingMode`.
- Produces: Modern dark OLED challenge UI with Canary Yellow progress matrix and live gait feedback.

- [ ] **Step 1: Modernize AlarmScreen.kt styling**

In `AlarmScreen.kt`:
- Minimalist circular clock dial with tick marks around the perimeter showing current time and challenge duration.
- Canary Yellow progress indicator and glowing step count.
- Gait status feedback ("Walking detected — keep moving!") with anti-cheat protection.
- Minimalist subordinate emergency bypass.

- [ ] **Step 2: Verify with unit tests**

Run: `powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest }"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/wakewalk/ui/alarm/AlarmScreen.kt
git commit -m "feat: modernize alarm challenge screen with Canary Yellow step visualization"
```

---

### Task 7: Full Verification & Debug Build

**Files:**
- Verification only

- [ ] **Step 1: Run all unit tests**

Run: `powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest }"`
Expected: 30/30 unit tests pass.

- [ ] **Step 2: Assemble debug APK**

Run: `powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat assembleDebug }"`
Expected: BUILD SUCCESSFUL with `app-debug.apk` produced.

- [ ] **Step 3: Update walkthrough artifact**

Update `walkthrough.md` with the new design screenshots, color tokens, and verification status.
