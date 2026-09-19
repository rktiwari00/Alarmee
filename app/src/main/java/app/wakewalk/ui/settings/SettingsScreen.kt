package app.wakewalk.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wakewalk.data.preferences.UserPreferences
import app.wakewalk.ui.components.StepTargetChips

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val preferences by viewModel.preferencesFlow.collectAsState()
    val diagnostics by viewModel.diagnosticStatus.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshDiagnostics() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Diagnostics"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Emergency Dismissal Method
            item {
                SectionHeader(title = "Emergency Dismissal Method")
                Text(
                    text = "A deliberate safety fallback for genuine emergencies. Normal wake-up requires walking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        EmergencyMethodOption(
                            title = "Type \"I AM AWAKE\"",
                            subtitle = "Case-insensitive typing test before emergency stop is enabled.",
                            selected = preferences.emergencyDismissalMethod == UserPreferences.METHOD_TYPING_PHRASE,
                            onSelect = { viewModel.setEmergencyMethod(UserPreferences.METHOD_TYPING_PHRASE) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        EmergencyMethodOption(
                            title = "Hold for 5 seconds",
                            subtitle = "Continuous unbroken touch on emergency button for 5 full seconds.",
                            selected = preferences.emergencyDismissalMethod == UserPreferences.METHOD_LONG_PRESS,
                            onSelect = { viewModel.setEmergencyMethod(UserPreferences.METHOD_LONG_PRESS) }
                        )
                    }
                }
            }

            // Default Step Target
            item {
                SectionHeader(title = "Default Step Target")
                Text(
                    text = "Suggested steps when creating a new alarm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                StepTargetChips(
                    selectedTarget = preferences.defaultStepTarget,
                    onTargetSelected = { viewModel.setDefaultStepTarget(it) }
                )
            }

            // Theme Selection
            item {
                SectionHeader(title = "Appearance")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        UserPreferences.THEME_SYSTEM to "System",
                        UserPreferences.THEME_DARK to "Dark / OLED",
                        UserPreferences.THEME_LIGHT to "Light"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = preferences.theme == key,
                            onClick = { viewModel.setTheme(key) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Diagnostics Dashboard
            item {
                SectionHeader(title = "Hardware & Reliability Diagnostics")
                Text(
                    text = "Verifies permissions and sensor status required for reliable wake-up.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DiagnosticRow(
                            label = "Exact Alarm Permission",
                            detail = if (diagnostics.exactAlarmsAllowed) "Granted (setAlarmClock)" else "Missing exact alarm capability",
                            ok = diagnostics.exactAlarmsAllowed,
                            onFix = if (!diagnostics.exactAlarmsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            } else null
                        )

                        DiagnosticRow(
                            label = "Notifications & Full-Screen Intent",
                            detail = if (diagnostics.notificationsEnabled) "Enabled" else "Disabled — alarms cannot show over lockscreen",
                            ok = diagnostics.notificationsEnabled,
                            onFix = if (!diagnostics.notificationsEnabled) {
                                {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            } else null
                        )

                        DiagnosticRow(
                            label = "Step Sensor",
                            detail = if (diagnostics.hardwareStepSensorAvailable) "Hardware Step Sensor Available" else "Accelerometer fallback active",
                            ok = diagnostics.hardwareStepSensorAvailable,
                            warningOnly = !diagnostics.hardwareStepSensorAvailable
                        )

                        DiagnosticRow(
                            label = "Battery Optimization",
                            detail = if (diagnostics.batteryOptimizationIgnored) "Unrestricted (optimal reliability)" else "Optimized (recommended: set to Unrestricted)",
                            ok = diagnostics.batteryOptimizationIgnored,
                            warningOnly = true,
                            onFix = if (!diagnostics.batteryOptimizationIgnored) {
                                {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            } else null
                        )
                    }
                }
            }

            // Privacy Commitment
            item {
                SectionHeader(title = "Privacy & Security")
                OutlinedCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "100% On-Device",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "WakeWalk has no remote servers, no tracking, and no external telemetry. Your alarms, movement sensors, and wake data remain strictly inside your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // App Version Footer
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WakeWalk v1.0.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Built for disciplined mornings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun EmergencyMethodOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    detail: String,
    ok: Boolean,
    warningOnly: Boolean = false,
    onFix: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when {
            ok -> Icons.Default.CheckCircle
            warningOnly -> Icons.Default.Warning
            else -> Icons.Default.Error
        }
        val tint = when {
            ok -> MaterialTheme.colorScheme.primary
            warningOnly -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onFix != null) {
            TextButton(onClick = onFix) {
                Text(text = "Settings", fontSize = 12.sp)
            }
        }
    }
}
