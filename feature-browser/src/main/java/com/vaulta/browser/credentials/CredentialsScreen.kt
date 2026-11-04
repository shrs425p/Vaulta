package com.vaulta.browser.credentials

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vaulta.crypto.PasswordStrength
import com.vaulta.storage.credentials.CredentialCategory
import com.vaulta.storage.credentials.VaultCredential

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialsScreen(
    viewModel: CredentialsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val credentials by viewModel.credentials.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val totpStateMap by viewModel.totpStateMap.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var viewingCredential by remember { mutableStateOf<VaultCredential?>(null) }
    var editingCredential by remember { mutableStateOf<VaultCredential?>(null) }
    var showEditScreen by remember { mutableStateOf(false) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val csvText = stream.bufferedReader().readText()
                    viewModel.importCsvVault(csvText)
                }
            } catch (e: Exception) {
                viewModel.copyToClipboard("Error", "CSV read error: ${e.message}")
            }
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    if (viewingCredential != null) {
        CredentialDetailScreen(
            credential = viewingCredential!!,
            viewModel = viewModel,
            onNavigateBack = { viewingCredential = null },
            onEdit = {
                editingCredential = viewingCredential
                viewingCredential = null
                showEditScreen = true
            }
        )
        return
    }

    if (showEditScreen) {
        CredentialEditScreen(
            credentialToEdit = editingCredential,
            viewModel = viewModel,
            onNavigateBack = {
                showEditScreen = false
                editingCredential = null
            },
            onSaved = {
                showEditScreen = false
                editingCredential = null
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCredential = null
                    showEditScreen = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Credential")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Input & CSV Import Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search logins, cards, notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                IconButton(
                    onClick = { csvLauncher.launch("*/*") }
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "Import CSV Vault",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("All (${credentials.size})") }
                    )
                }
                items(CredentialCategory.entries.toTypedArray()) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.selectCategory(if (selectedCategory == cat) null else cat) },
                        label = { Text(cat.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = getCategoryIcon(cat),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Credentials List
            if (credentials.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No credentials found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap + below to store encrypted passwords, or tap upload to import CSV",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(credentials, key = { it.id }) { item ->
                        val totpItemState = totpStateMap[item.id]
                        CredentialListItem(
                            credential = item,
                            totpState = totpItemState,
                            onCopy = { label, text -> viewModel.copyToClipboard(label, text) },
                            onClick = { viewingCredential = item },
                            onEdit = {
                                editingCredential = item
                                showEditScreen = true
                            },
                            onDelete = { viewModel.deleteCredential(item) },
                            calculateStrength = { pwd -> viewModel.calculateStrength(pwd) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialListItem(
    credential: VaultCredential,
    totpState: TotpItemState?,
    onCopy: (String, String) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    calculateStrength: (String) -> PasswordStrength
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(credential.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = credential.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (credential.username.isNotBlank()) {
                        Text(
                            text = credential.username,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (credential.password.isNotBlank()) {
                    val strength = calculateStrength(credential.password)
                    StrengthBadge(strength)
                }
            }

            // Quick Copy & TOTP Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (credential.username.isNotBlank()) {
                    AssistChip(
                        onClick = { onCopy("Username", credential.username) },
                        label = { Text("User", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )
                }
                if (credential.password.isNotBlank()) {
                    AssistChip(
                        onClick = { onCopy("Password", credential.password) },
                        label = { Text("Pass", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )
                }
                if (totpState != null) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onCopy("2FA Code", totpState.code) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = totpState.formattedCode,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { totpState.progress },
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StrengthBadge(strength: PasswordStrength) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when (strength) {
            PasswordStrength.VERY_WEAK, PasswordStrength.WEAK -> MaterialTheme.colorScheme.errorContainer
            PasswordStrength.FAIR -> MaterialTheme.colorScheme.tertiaryContainer
            PasswordStrength.STRONG, PasswordStrength.VERY_STRONG -> MaterialTheme.colorScheme.primaryContainer
        }
    ) {
        Text(
            text = strength.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = when (strength) {
                PasswordStrength.VERY_WEAK, PasswordStrength.WEAK -> MaterialTheme.colorScheme.onErrorContainer
                PasswordStrength.FAIR -> MaterialTheme.colorScheme.onTertiaryContainer
                PasswordStrength.STRONG, PasswordStrength.VERY_STRONG -> MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    }
}

private fun getCategoryIcon(category: CredentialCategory): ImageVector {
    return when (category) {
        CredentialCategory.LOGIN -> Icons.Default.Key
        CredentialCategory.CARD -> Icons.Default.CreditCard
        CredentialCategory.NOTE -> Icons.Default.Note
        CredentialCategory.PASSKEY -> Icons.Default.Security
    }
}
