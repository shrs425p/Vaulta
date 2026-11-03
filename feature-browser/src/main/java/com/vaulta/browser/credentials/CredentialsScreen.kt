package com.vaulta.browser.credentials

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val credentials by viewModel.credentials.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val totpSeconds by viewModel.totpRemainingSeconds.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var editingCredential by remember { mutableStateOf<VaultCredential?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCredential = VaultCredential(title = "")
                    showEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search logins, cards, notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

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
                            text = "Tap + below to store encrypted passwords or notes",
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
                        CredentialListItem(
                            credential = item,
                            totpSeconds = totpSeconds,
                            onCopy = { label, text -> viewModel.copyToClipboard(label, text) },
                            onEdit = {
                                editingCredential = item
                                showEditDialog = true
                            },
                            onDelete = { viewModel.deleteCredential(item) },
                            generateTotp = { secret -> viewModel.generateTotpCode(secret) },
                            calculateStrength = { pwd -> viewModel.calculateStrength(pwd) }
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog && editingCredential != null) {
        CredentialEditDialog(
            credential = editingCredential!!,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.saveCredential(updated)
                showEditDialog = false
            },
            onGeneratePassword = { length -> viewModel.generatePassword(length) },
            onGeneratePassphrase = { words -> viewModel.generatePassphrase(words) },
            calculateStrength = { pwd -> viewModel.calculateStrength(pwd) }
        )
    }
}

@Composable
private fun CredentialListItem(
    credential: VaultCredential,
    totpSeconds: Int,
    onCopy: (String, String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    generateTotp: (String) -> String,
    calculateStrength: (String) -> PasswordStrength
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
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

            // Quick Copy Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (credential.username.isNotBlank()) {
                    AssistChip(
                        onClick = { onCopy("Username", credential.username) },
                        label = { Text("Copy User", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
                if (credential.password.isNotBlank()) {
                    AssistChip(
                        onClick = { onCopy("Password", credential.password) },
                        label = { Text("Copy Password", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
                if (credential.totpSecret.isNotBlank()) {
                    val totpCode = generateTotp(credential.totpSecret)
                    if (totpCode.isNotBlank()) {
                        AssistChip(
                            onClick = { onCopy("2FA Code", totpCode) },
                            label = { Text("$totpCode (${totpSeconds}s)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    if (credential.url.isNotBlank()) {
                        Text("URL: ${credential.url}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (credential.notes.isNotBlank()) {
                        Text("Notes: ${credential.notes}", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onEdit) {
                            Text("Edit")
                        }
                        TextButton(onClick = onDelete) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StrengthBadge(strength: PasswordStrength) {
    val color = when (strength) {
        PasswordStrength.WEAK -> Color(0xFFE53935)
        PasswordStrength.FAIR -> Color(0xFFFB8C00)
        PasswordStrength.STRONG -> Color(0xFF43A047)
        PasswordStrength.VERY_STRONG -> Color(0xFF1E88E5)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = strength.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CredentialEditDialog(
    credential: VaultCredential,
    onDismiss: () -> Unit,
    onSave: (VaultCredential) -> Unit,
    onGeneratePassword: (Int) -> String,
    onGeneratePassphrase: (Int) -> String,
    calculateStrength: (String) -> PasswordStrength
) {
    var category by remember { mutableStateOf(credential.category) }
    var title by remember { mutableStateOf(credential.title) }
    var username by remember { mutableStateOf(credential.username) }
    var password by remember { mutableStateOf(credential.password) }
    var url by remember { mutableStateOf(credential.url) }
    var totpSecret by remember { mutableStateOf(credential.totpSecret) }
    var notes by remember { mutableStateOf(credential.notes) }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (credential.title.isBlank()) "New Credential" else "Edit Credential") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Account Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username / Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password / Secret") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                            IconButton(onClick = { password = onGeneratePassword(16) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Generate Password")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = totpSecret,
                    onValueChange = { totpSecret = it },
                    label = { Text("2FA / TOTP Secret Key (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Website URL (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            credential.copy(
                                category = category,
                                title = title.trim(),
                                username = username.trim(),
                                password = password,
                                url = url.trim(),
                                totpSecret = totpSecret.trim(),
                                notes = notes.trim(),
                                modifiedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getCategoryIcon(category: CredentialCategory): ImageVector {
    return when (category) {
        CredentialCategory.LOGIN -> Icons.Default.Key
        CredentialCategory.CARD -> Icons.Default.CreditCard
        CredentialCategory.NOTE -> Icons.Default.Note
        CredentialCategory.PASSKEY -> Icons.Default.Security
    }
}
