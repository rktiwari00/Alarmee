# Design Spec: QR Code & Barcode Scanning Alarm Challenge with Walk-to-Target Verification

## 1. Overview & Goals
This specification defines the architecture, data models, camera pipeline, user experience, and dismissal logic for the **QR Code & Barcode Scanning Challenge** in Alarmee, featuring **Walk-to-Target anti-cheat verification**.

Users can set an alarm that requires physically getting out of bed, walking a minimum number of steps to their designated target location (e.g. bathroom or kitchen), and scanning a pre-registered physical QR code or barcode to silence and dismiss the alarm.

### Key Goals
1. **Physical Displacement Guarantee (Dual Verification)**: Requires BOTH physical walking displacement (minimum steps verified by the gait anti-shake engine) AND camera scanning of the registered physical QR/barcode. If the user keeps the QR code next to their bed, scanning is rejected until the required steps have been walked.
2. **Universal Format Support**: Leverages Google ML Kit to support both 2D QR codes and standard 1D product barcodes (UPC-A, EAN-13, Code 128, etc.), eliminating the need for a printer.
3. **Low-Light Usability**: Integrates a torch/flashlight toggle directly into the scanner preview for dark morning rooms.
4. **Clean Material Design 3**: Fully integrated into the native Material 3 theme and navigation without adding clutter.
5. **Fail-Safe & Audio Reliability**: Continues holding full CPU wake locks, overrides volume levels, and preserves emergency phrase dismissal ("I AM AWAKE").

---

## 2. Architecture & Data Model

### 2.1 Domain & Entity Models
- **`ChallengeType` Enum (`app.wakewalk.domain.model.ChallengeType`)**:
  ```kotlin
  enum class ChallengeType {
      WALK,
      QR_CODE,
      DISTANCE,
      STAY_ACTIVE
  }
  ```

- **`AlarmEntity` (`app.wakewalk.data.local.entity.AlarmEntity`)**:
  Extend `AlarmEntity` with payload, label, and minimum walking steps:
  ```kotlin
  @Entity(tableName = "alarms")
  data class AlarmEntity(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      val hour: Int,
      val minute: Int,
      val label: String,
      val isEnabled: Boolean,
      val repeatDaysMask: Int,
      val challengeType: ChallengeType = ChallengeType.WALK,
      val targetSteps: Int = 150, // For WALK challenge, or minimum steps required for QR_CODE challenge (default 15)
      val qrCodePayload: String? = null,
      val qrCodeLabel: String? = null,
      val soundUri: String? = null,
      val vibrationEnabled: Boolean = true,
      val gradualVolume: Boolean = true,
      val snoozeEnabled: Boolean = false,
      val snoozeDurationMinutes: Int = 5,
      val createdAtEpochMs: Long = System.currentTimeMillis(),
      val updatedAtEpochMs: Long = System.currentTimeMillis()
  )
  ```

- **Room Database Migration (`WakeWalkDatabase`)**:
  - Incremented database version: `2`.
  - Migration `MIGRATION_1_2`:
    ```sql
    ALTER TABLE alarms ADD COLUMN qrCodePayload TEXT DEFAULT NULL;
    ALTER TABLE alarms ADD COLUMN qrCodeLabel TEXT DEFAULT NULL;
    ```
  - Configured with `fallbackToDestructiveMigration()` in `DatabaseModule` for development safety.

---

## 3. CameraX & ML Kit Barcode Pipeline

### 3.1 Dependencies
- `androidx.camera:camera-camera2`
- `androidx.camera:camera-lifecycle`
- `androidx.camera:camera-view`
- `com.google.mlkit:barcode-scanning`
- `android.permission.CAMERA` in `AndroidManifest.xml`

### 3.2 CameraBarcodeScanner Component (`app.wakewalk.ui.components.CameraBarcodeScanner`)
A reusable Jetpack Compose component encapsulating:
- **`PreviewView` AndroidView wrapper**: Bound to `ProcessCameraProvider` and Android lifecycle (`LifecycleOwner`).
- **`ImageAnalysis`**:
  - Frame analyzer invoking `BarcodeScanning.getClient()`.
  - Format mask: `Barcode.FORMAT_ALL_FORMATS` (covers QR, EAN-13, UPC-A, Code 128, etc.).
  - Analysis strategy: `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` to ensure zero preview lag.
- **Torch Controller**: Toggle state (`isTorchEnabled`) calling `camera.cameraControl.enableTorch(boolean)`.
- **Viewfinder Overlay**:
  - Translucent dimmed background with a central rounded clear scanning reticle.
  - Flashlight button with active/inactive visual feedback.
  - Scanning status text.

---

## 4. User Experience & Screen Workflows

### 4.1 Create / Edit Alarm Screen (`CreateEditAlarmScreen`)
1. **Challenge Selector**:
   - Material 3 chips: `[🚶 Walk Challenge]` vs `[📷 QR / Barcode]`.
2. **Registration Enforcement (Anti-Cheat Gate)**:
   - When `QR / Barcode` is chosen:
     - If no code is registered: Displays an outlined warning card with *"No reference code registered. Scan a household barcode (e.g. toothpaste or coffee jar) to enable this alarm."*
     - The "Save Alarm" button is locked until a valid code is scanned.
     - Tap *"Scan Reference Code"* opens the camera scanner sheet.
   - User points the camera at a physical code.
   - Upon detection:
     - Plays haptic confirmation.
     - Modal pops up: *"Code captured! Add a label (optional): e.g. Bathroom Sink"*.
     - Confirms and saves `qrCodePayload` and `qrCodeLabel`.
   - Once registered:
     - Card turns into a clean Material 3 tonal card: *"✓ Target Item: Bathroom Sink"* with a *"Change Code"* action button.
3. **Walk-to-Target Step Slider**:
   - Under the QR card, displays: *"Minimum Steps to Target: [15] steps"* with quick chips: `[10]`, `[15]`, `[25]`, `[30]`.
   - Explanatory caption: *"Ensures you walk from your bed to the target before the alarm can be dismissed."*

### 4.2 Ringing Alarm Screen (`AlarmScreen`)
1. **Foreground Service Trigger**:
   - `AndroidAlarmScheduler` delivers `EXTRA_CHALLENGE_TYPE`, `EXTRA_TARGET_STEPS`, `EXTRA_QR_CODE_PAYLOAD`, and `EXTRA_QR_CODE_LABEL` to `AlarmForegroundService`.
   - `AlarmForegroundService` starts audio and vibration, holds the wake lock, and launches `AlarmActivity`.
   - `AndroidSensorAdapter` starts counting steps using our gait anti-shake filter for both `WALK` and `QR_CODE` challenges.
2. **Dual-Verification Challenge UI**:
   - Top card displays:
     - Target label: *"Walk to: **Bathroom Sink**"*
     - Step progress indicator: *"Steps: {currentSteps} / {targetSteps}"*
   - Live camera viewfinder below with reticle and flashlight toggle.
3. **Dismissal Logic**:
   - **Case 1: User scans QR before completing minimum steps**:
     - Status turns amber: *"⚠️ You're still in bed! Walk {remaining} more steps first."*
     - Alarm continues ringing.
   - **Case 2: User completes minimum steps**:
     - Status turns green: *"✓ Steps completed! Point camera at target to dismiss."*
     - Success milestone haptic triggers.
   - **Case 3: User scans matching QR after completing minimum steps**:
     - Viewfinder flashes green with checkmark.
     - `AlarmViewModel` sends `ACTION_COMPLETE_CHALLENGE`.
     - `AlarmForegroundService` terminates audio, stops vibration, marks history as `CHALLENGE_COMPLETED`, and releases the wake lock.
     - Transitions to completion summary card.
   - **Case 4: User scans wrong QR code**:
     - Status flashes red: *"Wrong code scanned! Look for 'Bathroom Sink'"*.
   - Emergency phrase button ("I AM AWAKE") and Snooze remain available at all times.

---

## 5. Testing & Verification Plan

### 5.1 Automated Unit Tests
- `AlarmEntityTest` / `ConvertersTest`: Verifies `ChallengeType.QR_CODE` serialization/deserialization.
- `CreateEditAlarmViewModelTest`:
  - Verifies that QR challenge requires a non-empty `qrCodePayload` before saving.
  - Verifies minimum steps configuration.
  - Verifies state transitions when a reference code is scanned.
- `AlarmViewModelTest`:
  - Verifies that scanning QR with steps < targetSteps rejects dismissal.
  - Verifies that scanning QR with steps >= targetSteps dispatches completion.
  - Verifies wrong code handling.

### 5.2 Manual Verification
- Install debug APK on physical Android device.
- Register a physical barcode from a household item (e.g. toothpaste in bathroom, target steps: 15).
- Trigger alarm:
  - Try scanning target barcode immediately with 0 steps -> verify rejection message *"Walk 15 more steps first"*.
  - Walk 15 steps -> verify status switches to *"Steps completed! Now scan code"*.
  - Scan target barcode -> verify instant alarm silence and completion screen.
