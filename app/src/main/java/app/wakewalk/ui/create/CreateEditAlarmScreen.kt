package app.wakewalk.ui.create

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import app.wakewalk.domain.sound.AlarmSoundRegistry
import app.wakewalk.domain.sound.SoundCategory
import app.wakewalk.domain.sound.SoundItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.wakewalk.alarm.audio.AudioController
import app.wakewalk.domain.model.ChallengeType
import app.wakewalk.ui.components.CameraBarcodeScanner
import app.wakewalk.ui.components.RepeatDaySelector
import app.wakewalk.ui.components.StepTargetChips
import app.wakewalk.ui.components.TimePickerModal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditAlarmScreen(
    alarmId: Long,
    viewModel: CreateEditAlarmViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(alarmId) {
        viewModel.initForAlarm(alarmId)
    }

    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var showLabelPromptDialog by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var scannedTempCode by remember { mutableStateOf("") }
    var labelInput by remember { mutableStateOf("") }
    var isTorchOn by remember { mutableStateOf(false) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val title = uri?.let { RingtoneManager.getRingtone(context, it)?.getTitle(context) }
            viewModel.setSoundUri(uri?.toString(), title)
        }
    }

    val amPm = if (state.hour >= 12) "PM" else "AM"
    val displayHour = when (val h = state.hour % 12) {
        0 -> 12
        else -> h
    }
    val timeFormatted = String.format("%02d:%02d", displayHour, state.minute)
    val isSaveAllowed = viewModel.isSaveAllowed()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Alarm" else "New Alarm") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveAlarm(onNavigateBack) },
                        enabled = isSaveAllowed,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Big Time Selector Card
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeFormatted,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " $amPm",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Text(
                        text = "Tap to change time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Label Field
            OutlinedTextField(
                value = state.label,
                onValueChange = { viewModel.setLabel(it) },
                label = { Text("Alarm Label") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Repeat Days
            RepeatDaySelector(
                repeatDaysMask = state.repeatDaysMask,
                onMaskChanged = { viewModel.setRepeatDaysMask(it) }
            )

            HorizontalDivider()

            // Challenge Mode Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Wake-Up Challenge",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.challengeType == ChallengeType.WALK,
                        onClick = { viewModel.setChallengeType(ChallengeType.WALK) },
                        label = { Text("🚶 Walk Steps") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = state.challengeType == ChallengeType.QR_CODE,
                        onClick = { viewModel.setChallengeType(ChallengeType.QR_CODE) },
                        label = { Text("📷 QR / Barcode") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Challenge Specific Content
            if (state.challengeType == ChallengeType.WALK) {
                StepTargetChips(
                    selectedTarget = state.targetSteps,
                    onTargetSelected = { viewModel.setTargetSteps(it) }
                )
            } else {
                // QR / Barcode Challenge Registration & Settings
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.qrCodePayload.isNullOrBlank()) {
                        // Unregistered Gate Card
                        OutlinedCard(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Physical Code Required",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    text = "To prevent turning off alarms from bed, you must scan a physical QR code or household barcode (e.g. toothpaste in bathroom, kitchen coffee jar) before saving.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { showScannerDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scan Reference Code Now")
                                }
                            }
                        }
                    } else {
                        // Registered Target Card
                        ElevatedCard(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = state.qrCodeLabel ?: "Registered Target",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    TextButton(onClick = { showScannerDialog = true }) {
                                        Text("Re-scan")
                                    }
                                }
                                Text(
                                    text = "Code: ${state.qrCodePayload}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Walk-to-Target Step Gate (Anti-Cheat Dual Verification)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Walk to Target",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Minimum steps required before code can be scanned",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${state.targetSteps} steps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(10, 15, 25, 30).forEach { steps ->
                                FilterChip(
                                    selected = state.targetSteps == steps,
                                    onClick = { viewModel.setTargetSteps(steps) },
                                    label = { Text("$steps") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Sound & Tone Selector Card
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSoundDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Sound & Tone",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = state.soundTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Change sound",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sound & Vibration Toggles
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vibration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Pulse vibration during alarm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = state.vibrationEnabled, onCheckedChange = { viewModel.setVibrationEnabled(it) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gradual Volume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Ramp volume over 10 seconds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = state.gradualVolume, onCheckedChange = { viewModel.setGradualVolume(it) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lower Volume While Walking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Ducks volume to 30% after 10 steps. Ramps back up if walking pauses for 8s.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = state.lowerVolumeWhileWalking, onCheckedChange = { viewModel.setLowerVolumeWhileWalking(it) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Emergency Snooze", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Allows single 5-minute snooze", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = state.snoozeEnabled, onCheckedChange = { viewModel.setSnoozeEnabled(it) })
                }
            }

            // Delete Button (Edit Mode Only)
            if (state.isEditMode) {
                OutlinedButton(
                    onClick = { viewModel.deleteAlarm(onNavigateBack) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp)
                ) {
                    Text("Delete Alarm")
                }
            }
        }

        // Time Picker Modal
        if (showTimePicker) {
            TimePickerModal(
                initialHour = state.hour,
                initialMinute = state.minute,
                onConfirm = { hour, minute ->
                    viewModel.setTime(hour, minute)
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false }
            )
        }

        // Live Scanner Sheet / Dialog for Reference Code
        if (showScannerDialog) {
            Dialog(
                onDismissRequest = { showScannerDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraBarcodeScanner(
                        isTorchEnabled = isTorchOn,
                        onTorchToggle = { isTorchOn = !isTorchOn },
                        instructionText = "Scan any household QR or Barcode",
                        onBarcodeDetected = { detectedCode ->
                            scannedTempCode = detectedCode
                            labelInput = ""
                            showScannerDialog = false
                            showLabelPromptDialog = true
                        }
                    )

                    // Close Scanner Button
                    IconButton(
                        onClick = { showScannerDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(24.dp)
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }

        // Label Prompt Dialog
        if (showLabelPromptDialog) {
            AlertDialog(
                onDismissRequest = { showLabelPromptDialog = false },
                title = { Text("Code Registered!") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Scanned: $scannedTempCode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = labelInput,
                            onValueChange = { labelInput = it },
                            label = { Text("Location Name (optional)") },
                            placeholder = { Text("e.g. Bathroom Sink, Coffee Jar") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.setQrCodeReference(scannedTempCode, labelInput)
                            showLabelPromptDialog = false
                        }
                    ) {
                        Text("Save Reference")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLabelPromptDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Sound & Tone Selection Dialog
        if (showSoundDialog) {
            SoundPickerDialog(
                currentSoundUri = state.soundUri,
                currentlyPlayingUri = state.currentlyPlayingPreviewUri,
                soundRegistry = viewModel.soundRegistry,
                onSelectSound = { uri, title ->
                    viewModel.setSoundUri(uri, title)
                },
                onPreviewSound = { sound ->
                    viewModel.previewSound(sound)
                },
                onStopPreview = {
                    viewModel.stopPreview()
                },
                onLaunchDevicePicker = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE or RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        state.soundUri?.let { uriStr ->
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(uriStr))
                        }
                    }
                    ringtonePickerLauncher.launch(intent)
                },
                onDismiss = {
                    viewModel.stopPreview()
                    showSoundDialog = false
                }
            )
        }
    }
}

enum class SoundPickerTab(val title: String) {
    HARSH("⚡ Harsh"),
    SMOOTH("🌿 Smooth"),
    PHONE("📞 Phone"),
    RANDOM("🎲 Daily Random"),
    DEVICE("📱 Device")
}

@Composable
private fun SoundPickerDialog(
    currentSoundUri: String?,
    currentlyPlayingUri: String?,
    soundRegistry: AlarmSoundRegistry,
    onSelectSound: (uri: String?, title: String) -> Unit,
    onPreviewSound: (SoundItem) -> Unit,
    onStopPreview: () -> Unit,
    onLaunchDevicePicker: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(SoundPickerTab.HARSH) }

    AlertDialog(
        onDismissRequest = {
            onStopPreview()
            onDismiss()
        },
        title = {
            Column {
                Text("Alarm Sound & Tone", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Curated royalty-free loops & ringtones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SoundPickerTab.values().forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = {
                                onStopPreview()
                                selectedTab = tab
                            },
                            label = { Text(tab.title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                when (selectedTab) {
                    SoundPickerTab.HARSH -> {
                        val harshSounds = soundRegistry.getSoundsByCategory(SoundCategory.HARSH)
                        harshSounds.forEach { sound ->
                            SoundItemCard(
                                sound = sound,
                                isSelected = currentSoundUri == sound.contentUri,
                                isPlayingPreview = currentlyPlayingUri == sound.contentUri,
                                onSelect = {
                                    onStopPreview()
                                    onSelectSound(sound.contentUri, sound.title)
                                    onDismiss()
                                },
                                onTogglePreview = { onPreviewSound(sound) }
                            )
                        }
                    }
                    SoundPickerTab.SMOOTH -> {
                        val smoothSounds = soundRegistry.getSoundsByCategory(SoundCategory.SMOOTH)
                        smoothSounds.forEach { sound ->
                            SoundItemCard(
                                sound = sound,
                                isSelected = currentSoundUri == sound.contentUri,
                                isPlayingPreview = currentlyPlayingUri == sound.contentUri,
                                onSelect = {
                                    onStopPreview()
                                    onSelectSound(sound.contentUri, sound.title)
                                    onDismiss()
                                },
                                onTogglePreview = { onPreviewSound(sound) }
                            )
                        }
                    }
                    SoundPickerTab.PHONE -> {
                        val phoneSounds = soundRegistry.getSoundsByCategory(SoundCategory.PHONE_CALL)
                        phoneSounds.forEach { sound ->
                            val isDefaultSelected = (currentSoundUri == null || currentSoundUri == AlarmSoundRegistry.TOKEN_DEFAULT_CALL_RINGTONE) && sound.id == AlarmSoundRegistry.ID_PHONE_DEFAULT
                            val isStandardAlarmSelected = currentSoundUri == AlarmSoundRegistry.TOKEN_DEFAULT_ALARM && sound.id == AlarmSoundRegistry.ID_ALARM_STANDARD
                            val isCustomSelected = currentSoundUri == sound.contentUri
                            val isSelected = isDefaultSelected || isStandardAlarmSelected || isCustomSelected

                            SoundItemCard(
                                sound = sound,
                                isSelected = isSelected,
                                isPlayingPreview = currentlyPlayingUri == sound.contentUri,
                                onSelect = {
                                    onStopPreview()
                                    val uri = if (sound.id == AlarmSoundRegistry.ID_PHONE_DEFAULT) null else sound.contentUri
                                    onSelectSound(uri, sound.title)
                                    onDismiss()
                                },
                                onTogglePreview = { onPreviewSound(sound) }
                            )
                        }
                    }
                    SoundPickerTab.RANDOM -> {
                        RandomSoundCard(
                            title = "🎲 Randomize (All Categories)",
                            description = "Picks a different harsh, smooth, or phone tone every morning so you never get used to one.",
                            isSelected = currentSoundUri == AlarmSoundRegistry.TOKEN_RANDOM_ALL,
                            onSelect = {
                                onStopPreview()
                                onSelectSound(AlarmSoundRegistry.TOKEN_RANDOM_ALL, "🎲 Random (All Sounds)")
                                onDismiss()
                            }
                        )
                        RandomSoundCard(
                            title = "⚡ Randomize (Harsh Only)",
                            description = "Rotates siren, bugle, rock, and digital tones daily for an intense awakening.",
                            isSelected = currentSoundUri == AlarmSoundRegistry.TOKEN_RANDOM_HARSH,
                            onSelect = {
                                onStopPreview()
                                onSelectSound(AlarmSoundRegistry.TOKEN_RANDOM_HARSH, "🎲 Random (Harsh / Intense)")
                                onDismiss()
                            }
                        )
                        RandomSoundCard(
                            title = "🌿 Randomize (Smooth Only)",
                            description = "Rotates gentle acoustic piano, ambient chimes, and forest birds daily for a calm morning.",
                            isSelected = currentSoundUri == AlarmSoundRegistry.TOKEN_RANDOM_SMOOTH,
                            onSelect = {
                                onStopPreview()
                                onSelectSound(AlarmSoundRegistry.TOKEN_RANDOM_SMOOTH, "🎲 Random (Smooth / Gentle)")
                                onDismiss()
                            }
                        )
                    }
                    SoundPickerTab.DEVICE -> {
                        OutlinedCard(
                            shape = RoundedCornerShape(12.dp),
                            border = if (currentSoundUri != null &&
                                currentSoundUri != AlarmSoundRegistry.TOKEN_DEFAULT_CALL_RINGTONE &&
                                currentSoundUri != AlarmSoundRegistry.TOKEN_DEFAULT_ALARM &&
                                !currentSoundUri.startsWith("content://wakewalk/sound/")
                            ) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onStopPreview()
                                    onDismiss()
                                    onLaunchDevicePicker()
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "🎵 Choose from Device...",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Pick any ringtone or audio file installed on your device storage.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onStopPreview()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SoundItemCard(
    sound: SoundItem,
    isSelected: Boolean,
    isPlayingPreview: Boolean,
    onSelect: () -> Unit,
    onTogglePreview: () -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = sound.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(
                onClick = onTogglePreview,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isPlayingPreview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlayingPreview) "Stop Preview" else "Play Preview",
                    tint = if (isPlayingPreview) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RandomSoundCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
