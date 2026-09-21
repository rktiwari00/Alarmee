# QR Code & Barcode Scanning Challenge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a QR code and barcode scanning challenge in Alarmee with walk-to-target dual verification, requiring users to physically get out of bed, walk a minimum number of steps to their designated item, and scan the matching physical code to silence the alarm.

**Architecture:** Integrate CameraX and Google ML Kit Barcode Scanning for fast on-device 1D/2D scanning with a live Compose viewfinder and torch toggle. Update Room database and alarm scheduling to persist and transport the target QR payload and minimum steps. Implement dual-verification in `AlarmViewModel` ensuring both step target and QR match are satisfied before dispatching completion to `AlarmForegroundService`.

**Tech Stack:** Kotlin, Jetpack Compose, Material Design 3, CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`), Google ML Kit Barcode Scanning (`barcode-scanning`), Room Database, Coroutines, StateFlow, Hilt, JUnit4.

## Global Constraints
- Target Android SDK: 35, Min SDK: 26.
- Architecture: Unidirectional data flow (UDF) with Jetpack Compose and Material 3.
- Camera and scanning operations must run on-device, fully offline without requiring network access.
- Preserve 100% of existing background execution, wake lock retention, audio volume overrides, and gait step sensor anti-cheat mechanisms.

---

### Task 1: Dependencies, Manifest Permissions & Domain Model Updates

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/app/wakewalk/domain/model/ChallengeType.kt`
- Modify: `app/src/main/java/app/wakewalk/data/local/entity/AlarmEntity.kt`
- Modify: `app/src/main/java/app/wakewalk/data/local/WakeWalkDatabase.kt`
- Modify: `app/src/main/java/app/wakewalk/di/DatabaseModule.kt`
- Test: `app/src/test/java/app/wakewalk/data/local/AlarmEntityTest.kt`

**Interfaces:**
- Produces: `ChallengeType.QR_CODE`
- Produces: `AlarmEntity.qrCodePayload: String?`, `AlarmEntity.qrCodeLabel: String?`

- [ ] **Step 1: Write unit test for `ChallengeType.QR_CODE` and `AlarmEntity`**

Create `app/src/test/java/app/wakewalk/data/local/AlarmEntityTest.kt`:
```kotlin
package app.wakewalk.data.local

import app.wakewalk.data.local.converter.Converters
import app.wakewalk.data.local.entity.AlarmEntity
import app.wakewalk.domain.model.ChallengeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AlarmEntityTest {

    private val converters = Converters()

    @Test
    fun testQrCodeChallengeTypeConverter() {
        val converted = converters.fromChallengeType(ChallengeType.QR_CODE)
        assertEquals("QR_CODE", converted)
        val restored = converters.toChallengeType(converted)
        assertEquals(ChallengeType.QR_CODE, restored)
    }

    @Test
    fun testAlarmEntityWithQrCodePayload() {
        val entity = AlarmEntity(
            hour = 7,
            minute = 30,
            label = "Morning",
            isEnabled = true,
            repeatDaysMask = 0,
            challengeType = ChallengeType.QR_CODE,
            targetSteps = 20,
            qrCodePayload = "BARCODE_TOOTHPASTE_12345",
            qrCodeLabel = "Bathroom Sink"
        )
        assertEquals(ChallengeType.QR_CODE, entity.challengeType)
        assertEquals("BARCODE_TOOTHPASTE_12345", entity.qrCodePayload)
        assertEquals("Bathroom Sink", entity.qrCodeLabel)
        assertEquals(20, entity.targetSteps)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest --tests app.wakewalk.data.local.AlarmEntityTest }"
```
Expected: FAIL (Unresolved reference: `QR_CODE`, `qrCodePayload`, `qrCodeLabel`).

- [ ] **Step 3: Add dependencies, permissions, and entity fields**

1. Update `gradle/libs.versions.toml`:
Add to `[versions]`:
```toml
camerax = "1.3.4"
mlkitBarcode = "17.3.0"
```
Add to `[libraries]`:
```toml
androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
androidx-camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }
mlkit-barcode-scanning = { group = "com.google.mlkit", name = "barcode-scanning", version.ref = "mlkitBarcode" }
```

2. Update `app/build.gradle.kts`:
In `dependencies`:
```kotlin
    // CameraX & ML Kit Barcode Scanning
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
```

3. Update `app/src/main/AndroidManifest.xml`:
Add permissions and hardware feature:
```xml
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
```

4. Update `app/src/main/java/app/wakewalk/domain/model/ChallengeType.kt`:
```kotlin
package app.wakewalk.domain.model

enum class ChallengeType {
    WALK,
    QR_CODE,
    DISTANCE,
    STAY_ACTIVE
}
```

5. Update `app/src/main/java/app/wakewalk/data/local/entity/AlarmEntity.kt`:
Add `qrCodePayload: String? = null` and `qrCodeLabel: String? = null`:
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

6. Update `app/src/main/java/app/wakewalk/data/local/WakeWalkDatabase.kt` and `DatabaseModule.kt`:
Increment version to `2` and configure migration / `fallbackToDestructiveMigration()` in `DatabaseModule.kt`.

- [ ] **Step 4: Run unit test to verify it passes**

Run:
```powershell
powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest --tests app.wakewalk.data.local.AlarmEntityTest }"
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/app/wakewalk/domain/model/ChallengeType.kt app/src/main/java/app/wakewalk/data/local/entity/AlarmEntity.kt app/src/main/java/app/wakewalk/data/local/WakeWalkDatabase.kt app/src/main/java/app/wakewalk/di/DatabaseModule.kt app/src/test/java/app/wakewalk/data/local/AlarmEntityTest.kt
git commit -m "feat: add CameraX, ML Kit, and QR_CODE challenge entity schema"
```

---

### Task 2: Reusable CameraX & ML Kit Scanner Component

**Files:**
- Create: `app/src/main/java/app/wakewalk/ui/components/CameraBarcodeScanner.kt`

**Interfaces:**
- Produces: `CameraBarcodeScanner(modifier: Modifier, onBarcodeDetected: (String) -> Unit, isTorchEnabled: Boolean = false, onTorchToggle: (() -> Unit)? = null, overlayContent: @Composable () -> Unit = {})`

- [ ] **Step 1: Create `CameraBarcodeScanner.kt`**

Create `app/src/main/java/app/wakewalk/ui/components/CameraBarcodeScanner.kt`:
- Wrap `PreviewView` in `AndroidView`.
- Instantiate `BarcodeScanner` from `BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build())`.
- Configure `ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()`.
- Bind analyzer: on image proxy, call `scanner.process(InputImage.fromMediaImage(mediaImage, rotationDegrees))` with completion listener to close `imageProxy`.
- When barcode detected, invoke `onBarcodeDetected(rawValue)`.
- Support torch control via `camera.cameraControl.enableTorch(isTorchEnabled)`.
- Provide modern translucent viewfinder reticle overlay with flashlight floating button.

- [ ] **Step 2: Verify project builds with CameraBarcodeScanner**

Run:
```powershell
powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat compileDebugKotlin }"
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/wakewalk/ui/components/CameraBarcodeScanner.kt
git commit -m "feat: implement CameraBarcodeScanner Compose component with torch support"
```

---

### Task 3: Alarm Creation & Registration Flow (`CreateEditAlarmScreen` & `ViewModel`)

**Files:**
- Modify: `app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmViewModel.kt`
- Modify: `app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmScreen.kt`
- Test: `app/src/test/java/app/wakewalk/ui/create/CreateEditAlarmViewModelTest.kt`

**Interfaces:**
- Consumes: `ChallengeType.QR_CODE`, `AlarmEntity.qrCodePayload`, `AlarmEntity.qrCodeLabel`
- Produces: `CreateEditAlarmUiState.challengeType`, `CreateEditAlarmUiState.qrCodePayload`, `CreateEditAlarmUiState.qrCodeLabel`
- Produces: `CreateEditAlarmViewModel.setChallengeType(type: ChallengeType)`
- Produces: `CreateEditAlarmViewModel.setQrCodeReference(payload: String, label: String?)`

- [ ] **Step 1: Write unit tests for QR Challenge validation in `CreateEditAlarmViewModelTest`**

Create/Update `app/src/test/java/app/wakewalk/ui/create/CreateEditAlarmViewModelTest.kt`:
```kotlin
package app.wakewalk.ui.create

import app.wakewalk.alarm.scheduler.AlarmScheduler
import app.wakewalk.data.local.entity.AlarmEntity
import app.wakewalk.domain.model.ChallengeType
import app.wakewalk.domain.repository.AlarmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateEditAlarmViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeRepository = object : AlarmRepository {
        var insertedAlarm: AlarmEntity? = null
        override fun getAllAlarms(): Flow<List<AlarmEntity>> = flowOf(emptyList())
        override suspend fun getAlarmById(id: Long): AlarmEntity? = insertedAlarm
        override suspend fun insertAlarm(alarm: AlarmEntity): Long {
            insertedAlarm = alarm.copy(id = 1L)
            return 1L
        }
        override suspend fun updateAlarm(alarm: AlarmEntity) { insertedAlarm = alarm }
        override suspend fun deleteAlarm(alarm: AlarmEntity) {}
        override suspend fun setAlarmEnabled(id: Long, isEnabled: Boolean) {}
    }

    private val fakeScheduler = object : AlarmScheduler {
        override fun schedule(alarm: AlarmEntity) {}
        override fun cancel(alarm: AlarmEntity) {}
    }

    private lateinit var viewModel: CreateEditAlarmViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CreateEditAlarmViewModel(fakeRepository, fakeScheduler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testQrChallengeRequiresScannedReferenceCodeBeforeSaving() = runTest {
        viewModel.setChallengeType(ChallengeType.QR_CODE)
        assertFalse(viewModel.isSaveAllowed())

        viewModel.setQrCodeReference("TOOTHPASTE_UPC_987654", "Bathroom Sink")
        assertTrue(viewModel.isSaveAllowed())
    }

    @Test
    fun testSaveQrCodeAlarmPersistsTargetPayloadAndSteps() = runTest {
        viewModel.setChallengeType(ChallengeType.QR_CODE)
        viewModel.setQrCodeReference("TOOTHPASTE_UPC_987654", "Bathroom Sink")
        viewModel.setTargetSteps(25)

        var saved = false
        viewModel.saveAlarm { saved = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(saved)
        val alarm = fakeRepository.insertedAlarm
        assertEquals(ChallengeType.QR_CODE, alarm?.challengeType)
        assertEquals("TOOTHPASTE_UPC_987654", alarm?.qrCodePayload)
        assertEquals("Bathroom Sink", alarm?.qrCodeLabel)
        assertEquals(25, alarm?.targetSteps)
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run:
```powershell
powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest --tests app.wakewalk.ui.create.CreateEditAlarmViewModelTest }"
```
Expected: FAIL (`setChallengeType`, `isSaveAllowed`, `setQrCodeReference` unresolved).

- [ ] **Step 3: Implement QR code state and validation in ViewModel and Screen**

1. Update `CreateEditAlarmViewModel.kt`:
Add fields to `CreateEditAlarmUiState`:
- `challengeType: ChallengeType = ChallengeType.WALK`
- `qrCodePayload: String? = null`
- `qrCodeLabel: String? = null`
- `qrScanningDialogVisible: Boolean = false`
Add methods:
- `setChallengeType(type: ChallengeType)`
- `setQrCodeReference(payload: String, label: String?)`
- `isSaveAllowed(): Boolean`: returns `challengeType != ChallengeType.QR_CODE || !qrCodePayload.isNullOrBlank()`.
- Update `saveAlarm` to persist `challengeType`, `qrCodePayload`, `qrCodeLabel`.

2. Update `CreateEditAlarmScreen.kt`:
- Add Challenge Type Selector Chips: `[🚶 Walk Challenge]` vs `[📷 QR / Barcode Challenge]`.
- When `Walk Challenge`: show existing step count slider.
- When `QR / Barcode Challenge`:
  - Show Card with registered code info or "No code registered yet".
  - "Scan Reference Code" button opening a bottom sheet/dialog containing `CameraBarcodeScanner`.
  - On code detected: dialog prompts for label (e.g. "Bathroom Sink") and saves to draft.
  - "Minimum Steps to Target" slider with preset chips (10, 15, 25, 30 steps; default 15).
  - Enforce `saveAlarm` button enabled only when `isSaveAllowed()` is true.

- [ ] **Step 4: Run unit test to verify it passes**

Run:
```powershell
powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest --tests app.wakewalk.ui.create.CreateEditAlarmViewModelTest }"
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmViewModel.kt app/src/main/java/app/wakewalk/ui/create/CreateEditAlarmScreen.kt app/src/test/java/app/wakewalk/ui/create/CreateEditAlarmViewModelTest.kt
git commit -m "feat: add QR challenge setup and physical registration gate in CreateEditAlarm"
```

---

### Task 4: Service Pipeline & Dual-Verification in `AlarmViewModel`

**Files:**
- Modify: `app/src/main/java/app/wakewalk/alarm/scheduler/AndroidAlarmScheduler.kt`
- Modify: `app/src/main/java/app/wakewalk/alarm/service/AlarmForegroundService.kt`
- Modify: `app/src/main/java/app/wakewalk/ui/alarm/AlarmViewModel.kt`
- Test: `app/src/test/java/app/wakewalk/ui/alarm/AlarmViewModelTest.kt`

**Interfaces:**
- Produces: `AlarmForegroundService.ACTION_COMPLETE_CHALLENGE`
- Produces: `AlarmViewModel.onBarcodeScanned(scannedCode: String)`
- Produces: `AlarmUiState.qrScanError: String?`, `AlarmUiState.isQrMatchSuccess: Boolean`

- [ ] **Step 1: Write unit tests for dual-verification logic in `AlarmViewModelTest`**

Create/Update `app/src/test/java/app/wakewalk/ui/alarm/AlarmViewModelTest.kt`:
```kotlin
package app.wakewalk.ui.alarm

import app.wakewalk.domain.model.AlarmStatus
import app.wakewalk.domain.model.ChallengeType
import app.wakewalk.domain.model.StepTrackingMode
import app.wakewalk.domain.model.WakeSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmViewModelTest {

    @Test
    fun testQrScanRejectedIfStepsBelowTarget() {
        val targetCode = "TARGET_BARCODE_BATHROOM"
        val state = AlarmUiState(
            session = WakeSession(
                sessionId = "s1",
                alarmId = 1L,
                targetSteps = 15,
                initialSensorSteps = 0L,
                currentSteps = 5,
                status = AlarmStatus.CHALLENGE_ACTIVE,
                startedAtEpochMs = 1000L,
                startedRealtimeNs = 1000L,
                trackingMode = StepTrackingMode.HARDWARE_STEP_DETECTOR
            ),
            targetSteps = 15,
            completedSteps = 5,
            targetQrPayload = targetCode
        )

        val eval = AlarmViewModel.evaluateQrCodeScan(
            scannedCode = targetCode,
            targetPayload = state.targetQrPayload,
            currentSteps = state.completedSteps,
            targetSteps = state.targetSteps
        )

        assertFalse(eval.isComplete)
        assertEquals("You're still in bed! Walk 10 more steps before scanning.", eval.errorMessage)
    }

    @Test
    fun testQrScanAcceptedWhenStepsReachedAndCodeMatches() {
        val targetCode = "TARGET_BARCODE_BATHROOM"
        val eval = AlarmViewModel.evaluateQrCodeScan(
            scannedCode = targetCode,
            targetPayload = targetCode,
            currentSteps = 15,
            targetSteps = 15
        )

        assertTrue(eval.isComplete)
        assertEquals(null, eval.errorMessage)
    }

    @Test
    fun testQrScanRejectedWhenCodeMismatches() {
        val eval = AlarmViewModel.evaluateQrCodeScan(
            scannedCode = "WRONG_BARCODE",
            targetPayload = "TARGET_BARCODE_BATHROOM",
            currentSteps = 20,
            targetSteps = 15
        )

        assertFalse(eval.isComplete)
        assertEquals("Wrong code scanned! Look for your registered item.", eval.errorMessage)
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run:
```powershell
powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest --tests app.wakewalk.ui.alarm.AlarmViewModelTest }"
```
Expected: FAIL (`evaluateQrCodeScan`, `targetQrPayload` unresolved).

- [ ] **Step 3: Implement scheduler extra passing, service action, and ViewModel evaluation**

1. Update `AndroidAlarmScheduler.kt`:
Add intent extras:
- `EXTRA_CHALLENGE_TYPE = "app.wakewalk.extra.CHALLENGE_TYPE"`
- `EXTRA_QR_PAYLOAD = "app.wakewalk.extra.QR_PAYLOAD"`
- `EXTRA_QR_LABEL = "app.wakewalk.extra.QR_LABEL"`
Populate them from `alarm.challengeType.name`, `alarm.qrCodePayload`, `alarm.qrCodeLabel`.

2. Update `AlarmForegroundService.kt`:
- Add `const val ACTION_COMPLETE_CHALLENGE = "app.wakewalk.action.COMPLETE_CHALLENGE"`
- In `onStartCommand`: handle `ACTION_COMPLETE_CHALLENGE` by invoking `completeSession(AlarmStatus.CHALLENGE_COMPLETED)`.
- Forward `challengeType`, `targetSteps`, `qrCodePayload`, `qrCodeLabel` to `AlarmActivity`.

3. Update `AlarmViewModel.kt`:
- Add state fields: `challengeType`, `targetQrPayload`, `targetQrLabel`, `qrScanFeedback: String?`, `isTorchEnabled: Boolean`.
- Implement `evaluateQrCodeScan` pure companion method for easy testability and robustness.
- Implement `onBarcodeScanned(scanned: String)`:
  Calls `evaluateQrCodeScan`. If complete, sends `AlarmForegroundService.ACTION_COMPLETE_CHALLENGE` and transitions state to completed.
- Implement `toggleTorch()`.

- [ ] **Step 4: Run unit test to verify it passes**

Run:
```powershell
powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest --tests app.wakewalk.ui.alarm.AlarmViewModelTest }"
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/wakewalk/alarm/scheduler/AndroidAlarmScheduler.kt app/src/main/java/app/wakewalk/alarm/service/AlarmForegroundService.kt app/src/main/java/app/wakewalk/ui/alarm/AlarmViewModel.kt app/src/test/java/app/wakewalk/ui/alarm/AlarmViewModelTest.kt
git commit -m "feat: implement dual-verification logic and service completion action"
```

---

### Task 5: Ringing Alarm Screen UI Integration (`AlarmScreen.kt`)

**Files:**
- Modify: `app/src/main/java/app/wakewalk/ui/alarm/AlarmScreen.kt`
- Modify: `app/src/main/java/app/wakewalk/ui/alarm/AlarmActivity.kt`

**Interfaces:**
- Consumes: `CameraBarcodeScanner`, `AlarmViewModel.onBarcodeScanned`, `AlarmViewModel.toggleTorch`

- [ ] **Step 1: Update `AlarmActivity.kt` to extract intent extras**

In `AlarmActivity.kt`, extract `EXTRA_CHALLENGE_TYPE`, `EXTRA_QR_PAYLOAD`, and `EXTRA_QR_LABEL` from the launching intent or service extras and pass them to `AlarmViewModel` or `AlarmScreen`.

- [ ] **Step 2: Update `AlarmScreen.kt` for QR Challenge**

- If `challengeType == ChallengeType.WALK`: Render the existing step circle counter.
- If `challengeType == ChallengeType.QR_CODE`:
  - Show top status card:
    - Title: `"Walk to: ${uiState.targetQrLabel ?: "Target Code"}"`
    - Step count status: `"Steps: ${uiState.completedSteps} / ${uiState.targetSteps}"`
    - Dynamic instruction banner:
      - If `completedSteps < targetSteps`: `"Walk to your target item to unlock scanning (${targetSteps - completedSteps} steps remaining)"`
      - If `completedSteps >= targetSteps`: `"✓ Step requirement met! Scan your target barcode to dismiss."`
  - Render `CameraBarcodeScanner`:
    - Full viewfinder with scanning reticle.
    - Floating torch toggle button in upper right of viewfinder.
    - Error feedback banner (e.g. amber for still-in-bed, red for wrong code).
    - Success flash on match.
  - Retain bottom emergency dismissal button ("I AM AWAKE") and snooze button.

- [ ] **Step 3: Compile and verify project builds cleanly**

Run:
```powershell
powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat compileDebugKotlin }"
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/app/wakewalk/ui/alarm/AlarmScreen.kt app/src/main/java/app/wakewalk/ui/alarm/AlarmActivity.kt
git commit -m "feat: integrate QR scanner and walk-to-target UI into AlarmScreen"
```

---

### Task 6: Full Verification, Test Suite & Debug APK Assembly

**Files:**
- Verify all unit test suites and APK build.

- [ ] **Step 1: Run all unit tests**

Run:
```powershell
powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat testDebugUnitTest }"
```
Expected: All unit tests pass.

- [ ] **Step 2: Assemble Debug APK**

Run:
```powershell
powershell -Command "& { [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot', 'Process'); ./gradlew.bat assembleDebug }"
```
Expected: BUILD SUCCESSFUL and `app/build/outputs/apk/debug/app-debug.apk` generated.

- [ ] **Step 3: Commit and update documentation**

Update `walkthrough.md` and commit.
```bash
git add -A
git commit -m "chore: complete verification for QR code challenge with walk-to-target verification"
```
