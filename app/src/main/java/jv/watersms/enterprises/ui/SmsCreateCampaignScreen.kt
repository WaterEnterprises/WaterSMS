package jv.watersms.enterprises.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun SmsCreateCampaignScreen(
    viewModel: SmsViewModel,
    onCampaignCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermission = granted
        if (granted) viewModel.loadDeviceContacts()
    }

    val campaignName by viewModel.campaignName.collectAsState()
    val originalMessage by viewModel.originalMessage.collectAsState()
    val minDelay by viewModel.minDelay.collectAsState()
    val maxDelay by viewModel.maxDelay.collectAsState()
    val geminiState by viewModel.geminiState.collectAsState()
    val deviceContacts by viewModel.deviceContacts.collectAsState()
    val selectedContacts by viewModel.selectedContacts.collectAsState()
    val manualInput by viewModel.manualContactsInput.collectAsState()

    var contactTabSelected by remember { mutableStateOf(0) }
    var contactSearchQuery by remember { mutableStateOf("") }

    val filteredDeviceContacts = remember(deviceContacts, contactSearchQuery) {
        if (contactSearchQuery.isBlank()) {
            deviceContacts
        } else {
            deviceContacts.filter {
                it.name.contains(contactSearchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(contactSearchQuery)
            }
        }
    }

    val defaultRegion by viewModel.defaultRegion.collectAsState()
    val manualContactsCount = remember(manualInput, defaultRegion) {
        ContactImportHelper.parseManualContacts(manualInput, defaultRegion).size
    }
    val totalSelectedCount = selectedContacts.size + manualContactsCount

    val isFormValid = campaignName.isNotBlank() &&
            originalMessage.isNotBlank() &&
            totalSelectedCount > 0 &&
            (minDelay.toIntOrNull() ?: 0) >= 1 &&
            (maxDelay.toIntOrNull() ?: 0) >= (minDelay.toIntOrNull() ?: 1)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Launch New Campaign",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Generate 20 AI anti-spam phrasings with Gemini, build a recipient dispatch list, and broadcast SMS in bulk with randomized intervals.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Campaign Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = campaignName,
                        onValueChange = { viewModel.campaignName.value = it },
                        label = { Text("Campaign Identifier") },
                        placeholder = { Text("e.g. Black Friday Dispatch") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        )
                    )

                    OutlinedTextField(
                        value = originalMessage,
                        onValueChange = { viewModel.originalMessage.value = it },
                        label = { Text("Campaign Message") },
                        placeholder = { Text("Enter the custom template text to dispatch...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Anti-Spam AI Paraphraser",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Powered by Google Gemini",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = { viewModel.generatePhrasings() },
                        enabled = originalMessage.isNotBlank() && geminiState !is GeminiState.Loading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = "AI Generate")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate 20 AI Phrasings")
                    }

                    when (val state = geminiState) {
                        is GeminiState.Idle -> {
                            Text(
                                text = "Tip: Telecom carriers monitor duplicate SMS signatures. Generating 20 unique phrasings allows the bulk sender to cycle texts randomly and evade blocking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is GeminiState.Loading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text(
                                    text = "Gemini is engineering 20 variations...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                        is GeminiState.Success -> {
                            Surface(
                                color = Color(0xFF34D399).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF34D399))
                                    Column {
                                        Text(
                                            text = "20 Variations Generated!",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399)
                                        )
                                        Text(
                                            text = "Sample: \"${state.variations.firstOrNull() ?: ""}\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        is GeminiState.Error -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                        Text(
                                            text = "AI Generation Failed",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Ensure GEMINI_API_KEY is active in AI Studio Secrets. Dispatch will proceed with fallback original message replication.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Recipients",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Selected: $totalSelectedCount",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    TabRow(
                        selectedTabIndex = contactTabSelected,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        containerColor = Color.White.copy(alpha = 0.04f),
                        indicator = {}
                    ) {
                        Tab(
                            selected = contactTabSelected == 0,
                            onClick = { contactTabSelected = 0 },
                            text = { Text("Device Contacts") }
                        )
                        Tab(
                            selected = contactTabSelected == 1,
                            onClick = { contactTabSelected = 1 },
                            text = { Text("Manual Paste") }
                        )
                    }

                    if (contactTabSelected == 0) {
                        if (!hasContactsPermission) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Permission required to fetch device phonebook.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Contacts, contentDescription = "Contacts")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Authorize Directory")
                                }
                            }
                        } else {
                            if (deviceContacts.isEmpty()) {
                                Button(
                                    onClick = { viewModel.loadDeviceContacts() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Fetch")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Load Contacts Directory")
                                }
                            } else {
                                OutlinedTextField(
                                    value = contactSearchQuery,
                                    onValueChange = { contactSearchQuery = it },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                    placeholder = { Text("Search contact name...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.selectAllContacts(true) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Select All")
                                    }
                                    Button(
                                        onClick = { viewModel.selectAllContacts(false) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Clear All")
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.05f)
                                ) {
                                    LazyColumn {
                                        items(filteredDeviceContacts) { contact ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.toggleContactSelection(contact) }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = contact.isSelected,
                                                    onCheckedChange = { viewModel.toggleContactSelection(contact) }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(contact.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                                    Text(contact.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Enter contacts as 'Name, Phone' (one per line) or raw phone numbers:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = manualInput,
                                onValueChange = { viewModel.manualContactsInput.value = it },
                                placeholder = {
                                    Text("e.g.\nJohn Doe, +15550199\nJane Smith, 555-0188\n+12025550177")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                minLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Interval Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Adds randomized time delay ranges (in seconds) between successive SMS broadcasts to emulate realistic user workflow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = minDelay,
                            onValueChange = { viewModel.minDelay.value = it },
                            label = { Text("Min Sec") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        OutlinedTextField(
                            value = maxDelay,
                            onValueChange = { viewModel.maxDelay.value = it },
                            label = { Text("Max Sec") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.createAndStartCampaign {
                        onCampaignCreated()
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Launch Bulk Campaign", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
