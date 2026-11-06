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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLockVault: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isScreenProtectionEnabled by viewModel.isScreenProtectionEnabled.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val currentThemeStyle by viewModel.themeStyle.collectAsStateWithLifecycle()
    val stats by viewModel.vaultStats.collectAsStateWithLifecycle()
    val isSyncAvailable by viewModel.isSyncAvailable.collectAsStateWithLifecycle()
    val signedInEmail by viewModel.signedInEmail.collectAsStateWithLifecycle()
    val isStealthSyncEnabled by viewModel.isStealthSyncEnabled.collectAsStateWithLifecycle()
    val cloudStorageStats by viewModel.cloudStorageStats.collectAsStateWithLifecycle()
    val isFetchingCloudStats by viewModel.isFetchingCloudStats.collectAsStateWithLifecycle()
    var showCloudInspectorDialog by remember { mutableStateOf(false) }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isOffline = remember(context) { context.packageName.endsWith(".offline") }
    val appVersionName = remember(context) { getInstalledAppVersionName(context) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.data)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val savedUri = viewModel.getSavedKeyFileUri()
    val keyFileName = savedUri?.let { viewModel.getKeyFileName(it) }

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SettingsUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearState()
            }
            is SettingsUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.lockVault()
                            onLockVault()
                        }
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = MaterialTheme.colorScheme.error
                        )
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
            // 1. ACTIVE VAULT STATUS HEADER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Encrypted Vault Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${stats?.fileCount ?: 0} encrypted files • ${stats?.folderCount ?: 0} folders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. SETTINGS NAVIGATION MENU
            item {
                Text(
                    text = "VAULT PREFERENCES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column {
                        // 1. Theme & UI
                        SettingsNavRow(
                            icon = Icons.Default.Palette,
                            title = "Theme & UI",
                            subtitle = "${currentThemeStyle.displayName} • ${currentThemeMode.label}",
                            onClick = onNavigateToTheme
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 2. Privacy & Security
                        SettingsNavRow(
                            icon = Icons.Default.Security,
                            title = "Privacy & Security",
                            subtitle = "Screen protection (${if (isScreenProtectionEnabled) "ON" else "OFF"}), Biometrics, Auto-Lock",
                            onClick = onNavigateToSecurity
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 3. Vault Credentials
                        SettingsNavRow(
                            icon = Icons.Default.Key,
                            title = "Vault Credentials",
                            subtitle = "${keyFileName ?: "vault.key"} • Change master password",
                            onClick = onNavigateToCredentials
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 4. About Vaulta
                        SettingsNavRow(
                            icon = Icons.Default.Info,
                            title = "About Vaulta",
                            subtitle = "v$appVersionName • Cryptography architecture & system specs",
                            onClick = onNavigateToAbout
                        )
                    }
                }
            }

            // 3. CLOUD BACKUP & SYNC (Cloud edition) or AIR-GAPPED PRIVACY (Offline edition)
            item {
                if (!viewModel.isOfflineEdition) {
                    Text(
                        text = "CLOUD BACKUP & SYNC",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
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
                                Text("Google Drive Sync", fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Column(modifier = Modifier.padding(top = 2.dp)) {
                                    if (isSyncAvailable) {
                                        if (!signedInEmail.isNullOrBlank()) {
                                            Text(
                                                text = "Connected: $signedInEmail",
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }
                                        Text(
                                            text = if (isStealthSyncEnabled)
                                                "Active Space: Google Drive AppData (Hidden folder)\nBackups are encrypted and hidden from Google Drive web."
                                            else
                                                "Active Space: My Drive > Vaulta Backups (Visible folder)\nBackups are encrypted in your Google Drive folder.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            "Google Drive account is not signed in. Tap Sign In to backup and sync your encrypted vault.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                if (uiState is SettingsUiState.Processing) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Column(horizontalAlignment = Alignment.End) {
                                        OutlinedButton(
                                            onClick = {
                                                if (isSyncAvailable) {
                                                    viewModel.syncNow()
                                                } else {
                                                    val intent = viewModel.getSignInIntent(context)
                                                    if (intent != null) {
                                                        googleSignInLauncher.launch(intent)
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(if (isSyncAvailable) "Sync Now" else "Sign In")
                                        }

                                        if (isSyncAvailable) {
                                            androidx.compose.material3.TextButton(
                                                onClick = { viewModel.signOut() }
                                            ) {
                                                Text("Sign Out", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Stealth Cloud Backup Toggle
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isStealthSyncEnabled) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = if (isStealthSyncEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text("Stealth Cloud Backup", fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Text(
                                    text = if (isStealthSyncEnabled)
                                        "ON (Stealth) • Backups stored in Google Drive's hidden appDataFolder (invisible in Drive web & search). Toggling moves backups automatically."
                                    else
                                        "OFF (Visible) • Backups stored in My Drive > 'Vaulta Backups' folder. Toggling moves backups automatically.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = isStealthSyncEnabled,
                                    onCheckedChange = { viewModel.setStealthSyncEnabled(it) },
                                    enabled = isSyncAvailable && uiState !is SettingsUiState.Processing
                                )
                            }
                        )

                        if (isSyncAvailable) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                            // Cloud Storage Quota & Inspector
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                headlineContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Cloud Backup Storage", fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (isFetchingCloudStats) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        }
                                    }
                                },
                                supportingContent = {
                                    Column(modifier = Modifier.padding(top = 2.dp)) {
                                        val stats = cloudStorageStats
                                        if (stats != null) {
                                            Text(
                                                text = "${stats.totalFiles} files in cloud • ${formatBytes(stats.totalSizeBytes)} stored",
                                                fontWeight = FontWeight.Medium,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = if (stats.isStealthSpace)
                                                    "Stored in Google Drive's hidden appDataFolder."
                                                else
                                                    "Stored in My Drive > 'Vaulta Backups' folder.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Text(
                                                text = "Tap refresh to calculate storage used on Google Drive.",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.fetchCloudStats()
                                                    showCloudInspectorDialog = true
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Inspect Files", fontSize = 11.sp)
                                            }
                                            OutlinedButton(
                                                onClick = { showWipeConfirmDialog = true },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Wipe Cloud Backup", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.fetchCloudStats() }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh Cloud Stats",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "NETWORK & PRIVACY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        )
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
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                Text("Air-Gapped Edition", fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Text(
                                    "Offline Edition: 100% Air-Gapped. No network permissions requested or permitted.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }
            }

            // 4. LOCK VAULT ACTION
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        viewModel.lockVault()
                        onLockVault()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Lock Vault Immediately", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showCloudInspectorDialog) {
            val stats = cloudStorageStats
            AlertDialog(
                onDismissRequest = { showCloudInspectorDialog = false },
                title = {
                    Text(
                        text = "Google Drive Backups",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (stats == null || stats.blobs.isEmpty()) {
                            Text(
                                text = if (isFetchingCloudStats) "Querying Google Drive..." else "No remote backups found in active space.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Active Space: ${if (stats.isStealthSpace) "Hidden appDataFolder" else "My Drive > Vaulta Backups"}\nTotal: ${stats.totalFiles} items • ${formatBytes(stats.totalSizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(modifier = Modifier.height(260.dp)) {
                                items(stats.blobs.size) { index ->
                                    val blob = stats.blobs[index]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = blob.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${formatBytes(blob.sizeBytes)} • Encrypted Blob",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteRemoteBlob(blob.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete from Cloud",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCloudInspectorDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showWipeConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showWipeConfirmDialog = false },
                title = { Text("Wipe Cloud Backups?") },
                text = {
                    Text("This will permanently delete all encrypted files, blobs, and indexes stored on Google Drive in your active storage space.\n\nYour local files on this phone will remain completely safe.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showWipeConfirmDialog = false
                            viewModel.wipeCloudStorage()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Wipe Cloud Storage")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWipeConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(java.util.Locale.US, "%.1f %s", value, units[digitGroups])
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
