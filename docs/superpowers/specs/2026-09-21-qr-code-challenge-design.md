# Design Spec: QR Code & Barcode Scanning Alarm Challenge

## 1. Overview & Goals
This specification defines the architecture, data models, camera pipeline, user experience, and dismissal logic for the **QR Code & Barcode Scanning Challenge** in Alarmee.

Users can set an alarm that requires physically walking to a designated household location (e.g. bathroom sink, kitchen fridge) and scanning a pre-registered physical QR code or barcode to silence and dismiss the alarm.

### Key Goals
1. **Physical Displacement Guarantee**: Requires physical camera scanning to register a reference code and to dismiss the alarm, preventing bed dismissals.
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
  Extend `AlarmEntity` with payload and label fields:
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
      val targetSteps: Int = 150,
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
   - Filter chips or segmented row: `[🚶 Walk Challenge]` vs `[📷 QR / Barcode]`.
2. **Registration Enforcement (Anti-Cheat Gate)**:
   - When `QR / Barcode` is chosen:
     - If no code is registered: Displays an outlined warning card with *"No reference code registered. Scan a household barcode (e.g. toothpaste or coffee jar) to enable this alarm."*
     - The "Save Alarm" button is disabled (or validates with an explanatory snackbar).
     - Tap *"Scan Reference Code"* opens the camera scanner sheet.
   - User points the camera at a physical code.
   - Upon detection:
     - Plays haptic confirmation.
     - Modal pops up: *"Code captured! Add a label (optional): e.g. Bathroom Sink"*.
     - Confirms and saves `qrCodePayload` and `qrCodeLabel`.
   - Once registered:
     - Card turns into a clean Material 3 tonal card: *"✓ Target Item: Bathroom Sink"* with a *"Change Code"* action button.

### 4.2 Ringing Alarm Screen (`AlarmScreen`)
1. **Foreground Service Trigger**:
   - `AndroidAlarmScheduler` delivers `EXTRA_CHALLENGE_TYPE`, `EXTRA_QR_CODE_PAYLOAD`, and `EXTRA_QR_CODE_LABEL` to `AlarmForegroundService`.
   - `AlarmForegroundService` starts audio and vibration, holds the wake lock, and launches `AlarmActivity`.
   - If `challengeType == ChallengeType.QR_CODE`, the step sensor adapter is not started, preserving battery and preventing false sensor triggers.
2. **Active Ringing View**:
   - Top banner displays alarm time, label, and target item:
     - *"Scan Target: Bathroom Sink"*
   - Camera scanner runs live on screen with a scan reticle and torch button.
   - Emergency phrase button ("I AM AWAKE") and Snooze remain available.
3. **Verification & Dismissal**:
   - When a code is detected in camera frames:
     - If code matches `qrCodePayload`:
       - Viewfinder reticle flashes green with a checkmark animation.
       - Dispatches `AlarmForegroundService.ACTION_COMPLETE_CHALLENGE`.
       - Service halts audio and vibration immediately, records completion in `alarm_history`, posts completion notification, and terminates foreground mode.
       - UI displays the completion summary card.
     - If code does not match:
       - Displays a brief red notification: *"Wrong code scanned! Look for 'Bathroom Sink'"*.
       - Alarm continues ringing until the correct code is scanned.

---

## 5. Testing & Verification Plan

### 5.1 Automated Unit Tests
- `AlarmEntityTest` / `ConvertersTest`: Verifies `ChallengeType.QR_CODE` serialization/deserialization.
- `CreateEditAlarmViewModelTest`:
  - Verifies that QR challenge requires a non-empty `qrCodePayload` before saving.
  - Verifies state transitions when a reference code is scanned.
- `AlarmViewModelTest`:
  - Verifies handling of matching vs mismatching QR code scan events.
  - Verifies that a matching code dispatches service completion intent.

### 5.2 Manual Verification
- Install debug APK on physical Android device.
- Test camera permission flow (prompt, grant, deny handling).
- Register a physical barcode from a household item (e.g. soap or book).
- Trigger alarm, verify camera opens immediately, toggle flashlight on/off, scan target barcode, and verify alarm audio cuts off immediately upon recognition.
