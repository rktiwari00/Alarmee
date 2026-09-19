# Modern Alarm UI Redesign Specification (Option A)

## 1. Overview & Visual Identity

This specification defines the complete UI redesign of WakeWalk inspired by the modern aesthetic from the Simpl iOS Alarm design system:
- **Canary Yellow Accent**: `#FFE500` / `#FFD600` for active indicators, current alarm markers, and progress highlights.
- **Surfaces & Background**:
  - Light mode: Crisp off-white (`#F5F6F8`), pure white cards (`#FFFFFF`), neutral light borders (`#E5E7EB`).
  - Dark mode: Deep obsidian / charcoal (`#121316`), elevated dark cards (`#1C1E24`), subtle borders (`#2A2D36`).
- **Typography**: High-contrast, bold digital time displays (SF Pro / modern sans-serif aesthetic), bold hours with medium-weighted minutes.
- **Core Architecture Integrity**: Preserves 100% of WakeWalk's pure-Kotlin domain logic, `AlarmForegroundService`, `AndroidSensorAdapter`, Room relational persistence, and anti-cheat algorithms.

---

## 2. Design System & Tokens

### 2.1 Color Tokens (`Color.kt` & `Theme.kt`)
- `CanaryYellow`: `Color(0xFFFFE500)`
- `CanaryYellowHover`: `Color(0xFFE6CE00)`
- `CanaryYellowContainer`: `Color(0xFFFFF9B8)`
- `CharcoalDark`: `Color(0xFF111827)`
- `CharcoalSurface`: `Color(0xFF1A1C23)`
- `CharcoalSurfaceVariant`: `Color(0xFF262933)`
- `OffWhiteBackground`: `Color(0xFFF6F7F9)`
- `OffWhiteSurface`: `Color(0xFFFFFFFF)`
- `NeutralTickLight`: `Color(0xFFD1D5DB)`
- `NeutralTickDark`: `Color(0xFF374151)`
- `dynamicColor = false` by default in `WakeWalkTheme` to preserve the signature Canary Yellow brand palette across all Android 12+ devices.

---

## 3. Screen Specifications

### 3.1 Screen 1: Home Screen (`HomeScreen.kt`)
1. **Header & Hero Time Display**:
   - Header title "Alarm" with a dropdown chevron.
   - Prominent large digital clock showing the current time or next alarm (`09:50`) where hour digits are bold and minute digits are medium-toned.
   - Dynamic countdown subtitle: *"The next alarm clock in **19 min**"* or *"in **7h 25m**"*.
2. **24-Hour Timeline / Waveform View (`TimelineWaveform.kt`)**:
   - A horizontal activity canvas / bar chart depicting the 24 hours of the day.
   - Vertical tick lines representing hour and half-hour intervals.
   - Active scheduled alarms rendered as distinct dark tick marks.
   - The next imminent alarm highlighted with a glowing Canary Yellow marker line.
3. **Weekday Chips Row (`WeekdaySelectorRow.kt`)**:
   - `Mon`, `Tue`, `Wed`, `Thu`, `Fri`, `Sat`, `Sun` row.
   - The current/active selected day displayed as a vibrant Canary Yellow circular chip with bold black text.
4. **Modern Alarm Cards (`AlarmCard.kt`)**:
   - Large digital time (`09:50 AM`).
   - Challenge subtitle: `🚶 150 Steps • Walk Challenge • Gentle Wake`.
   - Dedicated circular dark **Play (`▶`) button** allowing the user to immediately preview the alarm tone and vibration, toggling to Stop (`■`) while playing.
   - Clean switch toggle for enabled/disabled state.
   - Long-press to delete with confirmation dialog.
5. **Floating Bottom Capsule Bar (`FloatingBottomPill.kt`)**:
   - Replaces traditional full-width bottom bar with an elevated rounded capsule floating above the screen bottom.
   - Left: Quick access icon (Alarms / Stats).
   - Center: Time duration indicator (`9:41 - 10:00`) or active session pill.
   - Right: High-contrast `+` button to open the Create Alarm flow.

---

### 3.2 Screen 2: Create / Edit Alarm Screen (`CreateEditAlarmScreen.kt`)
1. **Dual-Column Curved Time Picker**:
   - Large scrolling hour (`01`–`12` / `00`–`23`) and minute (`00`–`59`) columns with centered focus indicators.
   - Smooth gesture scrolling and haptic feedback.
2. **Quick Setting Action Pills**:
   - `🚶 Challenge`: Target steps selector (50, 100, 150, 200, 300 steps).
   - `🎵 Sound`: Ringtone selection with quick sample preview.
   - `🔁 Repeat`: Weekday selector matching the circular Canary Yellow design.
   - `⚡ Gentle Wake`: Toggle for gradual volume ramping.
3. **Bottom Action Bar**:
   - Minimalist `✕` cancel button on left.
   - "Choose time" / "Set Alarm" label in center.
   - Prominent circular black `✔` confirm button on right.

---

### 3.3 Screen 3: Active Ringing & Step Challenge (`AlarmScreen.kt`)
1. **Minimalist Geometric Clock Face**:
   - Clean circular clock dial with tick marks around the perimeter showing real-time progression.
2. **Canary Yellow Step Matrix / Progress Ring**:
   - Visual progress indicator rendered in Canary Yellow against deep OLED obsidian.
   - High-contrast live step counter (`42 / 150 STEPS`).
3. **Gait Status & Anti-Cheat Feedback**:
   - Live guidance: *"Walking detected — keep moving!"*
   - Subtle sensor status badge if accelerometer fallback is engaged.
4. **Subordinate Emergency Bypass**:
   - Secondary button for genuine emergencies (phrase verification or 5-second sustained hold).

---

## 4. Verification Plan

1. **Unit Tests**:
   - Run `./gradlew.bat testDebugUnitTest` to verify all 30 domain, persistence, viewmodel, and scheduler unit tests pass without regressions.
2. **Build Verification**:
   - Run `./gradlew.bat assembleDebug` to ensure all Compose components compile cleanly into the debug APK.
3. **Visual & Interaction Verification**:
   - Verify theme rendering in both Light and Dark modes.
   - Verify ringtone preview in alarm card plays and stops cleanly without interfering with background services.
   - Verify floating bottom pill coordinates smoothly with Scaffold padding and system navigation bars.
