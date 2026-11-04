package com.vaulta.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel
) {
    val isScreenProtectionEnabled by viewModel.isScreenProtectionEnabled.collectAsStateWithLifecycle()
    val isDeleteOriginalEnabled by viewModel.deleteOriginalAfterImport.collectAsStateWithLifecycle()
    val defaultSyncPolicy by viewModel.defaultSyncPolicy.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val timeoutMinutes by viewModel.autoLockTimeoutMinutes.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showTimeoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. PRIVACY & DISPLAY PROTECTION
            item {
                Text(
                    text = "DISPLAY & RECENTS PROTECTION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isScreenProtectionEnabled) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isScreenProtectionEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        headlineContent = {
                            Text("Screen Protection (FLAG_SECURE)", fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = {
                            Text(
                                text = if (isScreenProtectionEnabled)
                                    "ACTIVE • Screenshots, screen capture, and app switcher recents preview are blocked."
                                else
                                    "DISABLED • Screenshots and app switcher previews are permitted.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = isScreenProtectionEnabled,
                                onCheckedChange = { viewModel.setScreenProtectionEnabled(it) }
                            )
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isDeleteOriginalEnabled) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = if (isDeleteOriginalEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        headlineContent = {
                            Text("Always Delete Original Files", fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = {
                            Text(
                                text = if (isDeleteOriginalEnabled)
                                    "ACTIVE • Unencrypted original files are automatically deleted after encryption."
                                else
                                    "PROMPT • App prompts after each import asking whether to delete or keep the original.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = isDeleteOriginalEnabled,
                                onCheckedChange = { viewModel.setDeleteOriginalAfterImport(it) }
                            )
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        headlineContent = {
                            Text("Default Cloud Sync Policy", fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = {
                            Text(
                                text = defaultSyncPolicy.description,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            TextButton(
                                onClick = {
                                    val newPolicy = if (defaultSyncPolicy == com.vaulta.vault.preferences.DefaultSyncPolicy.SYNC_ALL) {
                                        com.vaulta.vault.preferences.DefaultSyncPolicy.SELECTIVE_MANUAL
                                    } else {
                                        com.vaulta.vault.preferences.DefaultSyncPolicy.SYNC_ALL
                                    }
                                    viewModel.setDefaultSyncPolicy(newPolicy)
                                }
                            ) {
                                Text(
                                    if (defaultSyncPolicy == com.vaulta.vault.preferences.DefaultSyncPolicy.SYNC_ALL) "Sync All" else "Selective",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                }
            }

            // 2. AUTHENTICATION & ACCESS
            item {
                Text(
                    text = "ACCESS & LOCKING CONTROLS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column {
                        // Biometrics
                        val canBiometrics = viewModel.canAuthenticateBiometrics()
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isBiometricEnabled) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = if (isBiometricEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text("Biometric Authentication", fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Text(
                                    text = if (canBiometrics) {
                                        if (isBiometricEnabled) "ENABLED • Fingerprint unlock active"
                                        else "DISABLED • Tap switch to enroll biometric unlock"
                                    } else {
                                        "Device does not have biometric hardware or enrolled prints."
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = isBiometricEnabled,
                                    enabled = canBiometrics,
                                    onCheckedChange = { checked ->
                                        if (checked && activity != null) {
                                            viewModel.enableBiometrics(activity) { success, error ->
                                                if (!success && error != null) {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(error)
                                                    }
                                                }
                                            }
                                        } else {
                                            viewModel.disableBiometrics()
                                        }
                                    }
                                )
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Auto-lock timeout
                        Surface(
                            onClick = { showTimeoutDialog = true },
                            color = Color.Transparent
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                headlineContent = {
                                    Text("Auto-Lock Inactivity Timeout", fontWeight = FontWeight.SemiBold)
                                },
                                supportingContent = {
                                    Text(
                                        text = "$timeoutMinutes minutes after leaving or backgrounding app",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 3. NETWORK PRIVACY & AIR-GAP BADGE
            item {
                Text(
                    text = "NETWORK & PERMISSIONS STATUS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.isOfflineEdition)
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    if (viewModel.isOfflineEdition) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text("Offline Air-Gapped Build", fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Text(
                                    text = "Offline Edition: 100% Air-Gapped. No network permissions requested or permitted.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    } else {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text("End-to-End Encrypted Cloud Sync", fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Text(
                                    text = if (viewModel.isSyncAvailableValue)
                                        "Zero-Knowledge Google Drive sync active. Plaintext is never transmitted."
                                    else
                                        "Google Drive account is not signed in. Tap in Settings to authenticate.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }
            }

            // 4. ZERO-KNOWLEDGE CRYPTOGRAPHY ARCHITECTURE
            item {
                Text(
                    text = "ZERO-KNOWLEDGE ARCHITECTURE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SecuritySpecRow(
                            icon = Icons.Default.Shield,
                            title = "256-bit AES-GCM Streaming AEAD",
                            subtitle = "Chunked authenticated cipher with position-bound nonces preventing chunk swapping and tampering."
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        SecuritySpecRow(
                            icon = Icons.Default.VpnKey,
                            title = "Argon2id Memory-Hard KDF",
                            subtitle = "Derives encryption keys using 64 MB of RAM and 3 iterations to thwart GPU/ASIC brute-forcing."
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        SecuritySpecRow(
                            icon = Icons.Default.Memory,
                            title = "Volatile Memory Zeroing",
                            subtitle = "Master keys are cleared with SecureMemory.wipe() as soon as the vault locks or times out."
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        SecuritySpecRow(
                            icon = Icons.Default.NoAccounts,
                            title = "Zero Telemetry Guarantee",
                            subtitle = "No analytics, no ad tracking, no crash telemetry. Operates entirely locally on your device."
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Auto-Lock Timeout Dialog
        if (showTimeoutDialog) {
            val timeouts = listOf(
                1 to "1 minute",
                5 to "5 minutes (Recommended)",
                15 to "15 minutes",
                30 to "30 minutes",
                60 to "1 hour"
            )
            var selectedOption by remember { mutableIntStateOf(timeoutMinutes) }

            AlertDialog(
                shape = RoundedCornerShape(24.dp),
                onDismissRequest = { showTimeoutDialog = false },
                title = { Text("Auto-Lock Timeout", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        timeouts.forEach { (minutes, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (selectedOption == minutes),
                                        onClick = { selectedOption = minutes }
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (selectedOption == minutes),
                                    onClick = { selectedOption = minutes }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.setAutoLockTimeout(selectedOption)
                            showTimeoutDialog = false
                        }
                    ) {
                        Text("Apply", fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimeoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SecuritySpecRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
