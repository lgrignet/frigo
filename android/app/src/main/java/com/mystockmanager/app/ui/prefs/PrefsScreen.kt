package com.mystockmanager.app.ui.prefs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.mystockmanager.app.R
import com.mystockmanager.app.core.QRCodeGenerator
import com.mystockmanager.app.ui.components.QRScanner

@Composable
fun PrefsScreen(
    onLogout: () -> Unit,
    viewModel: PrefsViewModel = hiltViewModel()
) {
    val prefs by viewModel.prefs.collectAsState()
    val syncGuid by viewModel.syncGuid.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    var showQrDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showScanner = true
    }

    if (showScanner) {
        QRScanner(
            onScan = { code ->
                viewModel.updateSyncGuid(code)
                showScanner = false
            },
            onClose = { showScanner = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.tab_settings),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        PrefsSection(title = stringResource(R.string.prefs_section_general)) {
            // Language
            PrefsItem(label = stringResource(R.string.setting_lang)) {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val langs = listOf("fr", "en", "nl", "de", "es")
                    langs.forEach { lang ->
                        FilterChip(
                            selected = prefs.lang == lang,
                            onClick = { viewModel.updateLang(lang) },
                            label = { 
                                val label = when(lang) {
                                    "fr" -> stringResource(R.string.lang_fr)
                                    "en" -> stringResource(R.string.lang_en)
                                    "nl" -> stringResource(R.string.lang_nl)
                                    "de" -> stringResource(R.string.lang_de)
                                    "es" -> stringResource(R.string.lang_es)
                                    else -> lang
                                }
                                Text(label) 
                            }
                        )
                    }
                }
            }

            // Date Format
            PrefsItem(label = stringResource(R.string.setting_date_format)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.dateFormat == "european",
                        onClick = { viewModel.updateDateFormat("european") },
                        label = { Text("JJ/MM/AAAA") }
                    )
                    FilterChip(
                        selected = prefs.dateFormat == "iso",
                        onClick = { viewModel.updateDateFormat("iso") },
                        label = { Text("AAAA-MM-JJ") }
                    )
                }
            }

            // Expiry Warning Days
            PrefsItem(label = stringResource(R.string.setting_expiry_warning)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = prefs.expiryWarningDays.toFloat(),
                        onValueChange = { viewModel.updateExpiryDays(it.toInt()) },
                        valueRange = 1f..30f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${prefs.expiryWarningDays}j",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Theme Toggle
            PrefsItem(label = stringResource(R.string.setting_theme)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.theme == "dark",
                        onClick = { viewModel.updateTheme("dark") },
                        label = { Text("🌙 " + stringResource(R.string.theme_dark)) }
                    )
                    FilterChip(
                        selected = prefs.theme == "light",
                        onClick = { viewModel.updateTheme("light") },
                        label = { Text("☀️ " + stringResource(R.string.theme_light)) }
                    )
                }
            }

            // Notifications Toggle
            PrefsItem(label = "Notifications d'alerte") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (prefs.notificationsEnabled) "Activées" else "Désactivées", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    Switch(
                        checked = prefs.notificationsEnabled,
                        onCheckedChange = { viewModel.updateNotifications(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrefsSection(title = stringResource(R.string.prefs_section_sync)) {
            PrefsItem(label = stringResource(R.string.setting_sync_channel)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = syncGuid,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    )
                    
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(syncGuid))
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.btn_copy), tint = MaterialTheme.colorScheme.primary)
                    }
                    
                    IconButton(onClick = { showQrDialog = true }) {
                        Icon(Icons.Default.QrCode, contentDescription = "Show QR", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = {
                        val permissionCheckResult = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                        if (permissionCheckResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            showScanner = true
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.btn_scan), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Logout Button
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.btn_logout), fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showQrDialog && syncGuid.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text(stringResource(R.string.btn_close))
                }
            },
            title = { Text(stringResource(R.string.title_qr_code)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val bitmap = remember(syncGuid) { QRCodeGenerator.generate(syncGuid) }
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                }
            }
        )
    }
}

@Composable
fun PrefsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun PrefsItem(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
