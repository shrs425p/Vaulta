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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
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
    val isStealthSyncEnabled by viewModel.isStealthSyncEnabled.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val isOffline = remember(context) { context.packageName.endsWith(".offline") }

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
                            subtitle = "v1.0.0 • Cryptography architecture & system specs",
                            onClick = onNavigateToAbout
                        )
                    }
                }
            }

            // 3. CLOUD BACKUP & SYNC (Cloud Flavor) or AIR-GAPPED PRIVACY (Offline Flavor)
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
                                Text(
                                    if (isSyncAvailable)
                                        "Uploads encrypted blobs & encrypted index. Plaintext is never transmitted."
                                    else
                                        "Google Drive account is not signed in. Tap to authenticate and sync.",
                                    style = MaterialTheme.typography.bodySmall
                                )
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
                                        "ON (Stealth) • Backups stored in Google Drive's hidden appDataFolder (invisible in Drive search)."
                                    else
                                        "OFF (Visible) • Backups stored in My Drive > Vaulta Backups folder.",
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
    }
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
