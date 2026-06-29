package jv.watersms.enterprises.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.i18n.phonenumbers.PhoneNumberUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSettingsScreen(
    viewModel: SmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationsPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasSmsPermission = granted }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermission = granted
        if (granted) viewModel.loadDeviceContacts()
    }

    val defaultRegion by viewModel.defaultRegion.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Manage permissions, regional formatting, AI engine, and carrier compliance guidelines.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Android System Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    PermissionStatusRow(
                        title = "SMS Dispatch Permission",
                        description = "Required to broadcast bulk message queues",
                        isGranted = hasSmsPermission,
                        onRequest = { smsPermissionLauncher.launch(android.Manifest.permission.SEND_SMS) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    PermissionStatusRow(
                        title = "Contacts Directory Permission",
                        description = "Required to import recipient lists from device storage",
                        isGranted = hasContactsPermission,
                        onRequest = { contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS) }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    PermissionStatusRow(
                        title = "Push Notifications Permission",
                        description = "Provides live update badges of ongoing send tasks",
                        isGranted = hasNotificationsPermission,
                        onRequest = {}
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Text(
                        text = "Regional Formatting",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    var expanded by remember { mutableStateOf(false) }

                    val countryList = remember {
                        val phoneUtil = PhoneNumberUtil.getInstance()
                        java.util.Locale.getISOCountries().mapNotNull { code ->
                            try {
                                val locale = java.util.Locale("", code)
                                val name = locale.displayCountry
                                val callingCode = phoneUtil.getCountryCodeForRegion(code)
                                if (callingCode != 0 && name.isNotEmpty()) {
                                    CountryInfo(code, name, callingCode)
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedBy { it.name }
                    }

                    val selectedCountry = remember(defaultRegion, countryList) {
                        countryList.find { it.code.equals(defaultRegion, ignoreCase = true) }
                    }

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.15f))
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCountry?.let {
                                        "${getFlagEmoji(it.code)} ${it.name} (+${it.callingCode})"
                                    } ?: defaultRegion,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Open",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .heightIn(max = 280.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            countryList.forEach { country ->
                                val flag = getFlagEmoji(country.code)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = flag, fontSize = 20.sp)
                                            Text(
                                                text = "${country.name} (+${country.callingCode})",
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (country.code == defaultRegion) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setDefaultRegion(country.code)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = "Used to intelligently format imported numbers into E.164 international format when they lack a '+' prefix.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Gemini AI Engine Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "To enable bulk sending without triggering telecom spam filters, Gemini generates 20 unique phrasings of your message so that each recipient receives slightly distinct text.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    "Gemini API Connected",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                                Text(
                                    "Secured via AI Studio Secrets Panel.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Compliance",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Carrier Compliance Guidelines",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    ComplianceTipRow(
                        icon = Icons.Default.Timer,
                        title = "Delay Intervals",
                        desc = "Using 5-15s random delays emulates realistic human dispatch. Never use zero-delay sending on personal SIM lines."
                    )

                    ComplianceTipRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "High Text Variety",
                        desc = "Avoid sending duplicate texts in short succession. Use the Gemini anti-spam rewriter to create distinct variations."
                    )

                    ComplianceTipRow(
                        icon = Icons.Default.Group,
                        title = "List Sanitization",
                        desc = "Double-check phone numbers are in E.164 format (e.g. +1234567890) and names are correct before blasting campaigns."
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionStatusRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Surface(
                color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Granted", tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                    Text("Active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
            }
        } else {
            Button(
                onClick = onRequest,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Enable", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ComplianceTipRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class CountryInfo(
    val code: String,
    val name: String,
    val callingCode: Int
)

fun getFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return ""
    return try {
        val firstChar = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
        String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    } catch (e: Exception) {
        "\uD83C\uDF10"
    }
}
