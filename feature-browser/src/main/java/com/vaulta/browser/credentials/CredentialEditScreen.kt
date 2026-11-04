package com.vaulta.browser.credentials

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaulta.storage.credentials.CredentialCategory
import com.vaulta.storage.credentials.CredentialTemplate
import com.vaulta.storage.credentials.VaultCredential

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialEditScreen(
    credentialToEdit: VaultCredential? = null,
    viewModel: CredentialsViewModel,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    var title by remember { mutableStateOf(credentialToEdit?.title.orEmpty()) }
    var username by remember { mutableStateOf(credentialToEdit?.username.orEmpty()) }
    var password by remember { mutableStateOf(credentialToEdit?.password.orEmpty()) }
    var url by remember { mutableStateOf(credentialToEdit?.url.orEmpty()) }
    var totpSecret by remember { mutableStateOf(credentialToEdit?.totpSecret.orEmpty()) }
    var notes by remember { mutableStateOf(credentialToEdit?.notes.orEmpty()) }
    var category by remember { mutableStateOf(credentialToEdit?.category ?: CredentialCategory.LOGIN) }
    var templateType by remember { mutableStateOf(credentialToEdit?.templateType ?: CredentialTemplate.LOGIN) }
    var selectedColorTag by remember { mutableStateOf(credentialToEdit?.colorTag) }

    var showGeneratorSheet by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    val analysis by remember(password) {
        derivedStateOf { viewModel.analyzePassword(password) }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (credentialToEdit == null) "Add Credential" else "Edit Credential",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                val savedCred = (credentialToEdit ?: VaultCredential(title = title)).copy(
                                    title = title.trim(),
                                    username = username.trim(),
                                    password = password,
                                    url = url.trim(),
                                    totpSecret = totpSecret.trim(),
                                    notes = notes.trim(),
                                    category = category,
                                    templateType = templateType,
                                    colorTag = selectedColorTag
                                )
                                viewModel.saveCredential(savedCred)
                                onSaved()
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = if (title.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Template & Category Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = templateType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Template") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            CredentialTemplate.entries.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.displayName) },
                                    onClick = {
                                        templateType = t
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Title Field
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Username / Identity
            item {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username / Email") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Password with Live Shannon Entropy Meter & Generator Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Visibility"
                                        )
                                    }
                                    IconButton(onClick = { showGeneratorSheet = true }) {
                                        Icon(Icons.Default.Casino, contentDescription = "Password Generator", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (password.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { analysis.strength.score },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = when (analysis.strength) {
                                    com.vaulta.crypto.PasswordStrength.VERY_WEAK, com.vaulta.crypto.PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
                                    com.vaulta.crypto.PasswordStrength.FAIR -> MaterialTheme.colorScheme.tertiary
                                    com.vaulta.crypto.PasswordStrength.STRONG, com.vaulta.crypto.PasswordStrength.VERY_STRONG -> MaterialTheme.colorScheme.primary
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Entropy: ${analysis.entropyBits} bits (${analysis.strength.label})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Crack time: ${analysis.crackTimeDisplay}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2FA TOTP Secret Key
            item {
                OutlinedTextField(
                    value = totpSecret,
                    onValueChange = { totpSecret = it.uppercase().replace(" ", "") },
                    label = { Text("2FA TOTP Secret Key (Base32)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Website URL
            item {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Website / URL") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Notes
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Generator Bottom Sheet
        if (showGeneratorSheet) {
            ModalBottomSheet(
                onDismissRequest = { showGeneratorSheet = false },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                var length by remember { mutableFloatStateOf(18f) }
                var incUpper by remember { mutableStateOf(true) }
                var incLower by remember { mutableStateOf(true) }
                var incDigits by remember { mutableStateOf(true) }
                var incSymbols by remember { mutableStateOf(true) }
                var isDicewareMode by remember { mutableStateOf(false) }

                var generatedPassword by remember {
                    mutableStateOf(
                        if (isDicewareMode) viewModel.generatePassphrase(4)
                        else viewModel.generatePassword(length.toInt(), incUpper, incLower, incDigits, incSymbols)
                    )
                }

                val genAnalysis = remember(generatedPassword) {
                    viewModel.analyzePassword(generatedPassword)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Password & Passphrase Generator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Mode Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = !isDicewareMode,
                            onClick = {
                                isDicewareMode = false
                                generatedPassword = viewModel.generatePassword(length.toInt(), incUpper, incLower, incDigits, incSymbols)
                            },
                            label = { Text("Random Password") }
                        )
                        FilterChip(
                            selected = isDicewareMode,
                            onClick = {
                                isDicewareMode = true
                                generatedPassword = viewModel.generatePassphrase(4)
                            },
                            label = { Text("Diceware Passphrase") }
                        )
                    }

                    // Generated Preview Box
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = generatedPassword,
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${genAnalysis.entropyBits} bits • ${genAnalysis.strength.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                generatedPassword = if (isDicewareMode) viewModel.generatePassphrase(4)
                                else viewModel.generatePassword(length.toInt(), incUpper, incLower, incDigits, incSymbols)
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                            }
                        }
                    }

                    if (!isDicewareMode) {
                        Column {
                            Text("Length: ${length.toInt()} characters", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = length,
                                onValueChange = {
                                    length = it
                                    generatedPassword = viewModel.generatePassword(length.toInt(), incUpper, incLower, incDigits, incSymbols)
                                },
                                valueRange = 8f..64f,
                                steps = 56
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Uppercase Letters (A-Z)")
                            Switch(checked = incUpper, onCheckedChange = { incUpper = it; generatedPassword = viewModel.generatePassword(length.toInt(), incUpper, incLower, incDigits, incSymbols) })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Digits (0-9)")
                            Switch(checked = incDigits, onCheckedChange = { incDigits = it; generatedPassword = viewModel.generatePassword(length.toInt(), incUpper, incLower, incDigits, incSymbols) })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Symbols (!@#$)")
                            Switch(checked = incSymbols, onCheckedChange = { incSymbols = it; generatedPassword = viewModel.generatePassword(length.toInt(), incUpper, incLower, incDigits, incSymbols) })
                        }
                    }

                    Button(
                        onClick = {
                            password = generatedPassword
                            showGeneratorSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Use This Password", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
