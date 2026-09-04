package com.mystockmanager.app.ui.prefs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mystockmanager.app.R
import com.mystockmanager.app.core.QRCodeGenerator
import com.mystockmanager.app.ui.components.QRScanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrefsScreen(
    onLogout: () -> Unit,
    viewModel: PrefsViewModel = hiltViewModel()
) {
    val prefs by viewModel.prefs.collectAsState()
    val domiciles by viewModel.domiciles.collectAsState()
    val syncGuid by viewModel.syncGuid.collectAsState()
    
    val firstName by viewModel.firstName.collectAsState()
    val lastName by viewModel.lastName.collectAsState()
    val emailVerified by viewModel.emailVerified.collectAsState()
    val verifyCodeError by viewModel.verifyCodeError.collectAsState()

    val clipboardManager = LocalClipboardManager.current

    var showQrDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var isChangingHousehold by remember { mutableStateOf(false) }
    var householdChangeError by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var verifyCode by remember { mutableStateOf("") }
    var langExpanded by remember { mutableStateOf(false) }
    var domExpanded by remember { mutableStateOf(false) }

    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = { showVerifyDialog = false },
            title = { Text(stringResource(R.string.verify_email_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.verify_email_dialog_subtitle), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = verifyCode,
                        onValueChange = { verifyCode = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text(stringResource(R.string.label_verification_code)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (verifyCodeError) {
                        Text(
                            text = stringResource(R.string.error_verification_code_invalid),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.verify_email_resend_link),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clickable { viewModel.resendVerificationCode() }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.verifyEmail(verifyCode) { ok ->
                        if (ok) {
                            showVerifyDialog = false
                            verifyCode = ""
                        }
                    }
                }) { Text(stringResource(R.string.btn_verify)) }
            },
            dismissButton = {
                TextButton(onClick = { showVerifyDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    if (showScanner) {
        QRScanner(
            onScan = { code ->
                showScanner = false
                isChangingHousehold = true
                householdChangeError = false
                viewModel.updateSyncGuid(code) { ok ->
                    isChangingHousehold = false
                    householdChangeError = !ok
                }
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

        // Profile Section
        PrefsSection(title = stringResource(R.string.prefs_section_profile)) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { viewModel.firstName.value = it },
                label = { Text(stringResource(R.string.label_first_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = { viewModel.lastName.value = it },
                label = { Text(stringResource(R.string.label_last_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.saveProfile() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_save_profile))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(if (emailVerified) R.string.email_verified else R.string.email_not_verified),
                    fontSize = 13.sp,
                    color = if (emailVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!emailVerified) {
                    TextButton(onClick = { showVerifyDialog = true }) {
                        Text(stringResource(R.string.btn_verify))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrefsSection(title = stringResource(R.string.prefs_section_general)) {
            // Active Domicile
            PrefsItem(label = stringResource(R.string.setting_active_domicile)) {
                ExposedDropdownMenuBox(
                    expanded = domExpanded,
                    onExpandedChange = { domExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentDomName = domiciles.find { it.id == prefs.activeDomicileId }?.name ?: stringResource(R.string.label_none)
                    OutlinedTextField(
                        value = currentDomName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = domExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(expanded = domExpanded, onDismissRequest = { domExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.label_none)) },
                            onClick = { viewModel.updateActiveDomicile(null); domExpanded = false }
                        )
                        domiciles.forEach { dom ->
                            DropdownMenuItem(
                                text = { Text(dom.name) },
                                onClick = { viewModel.updateActiveDomicile(dom.id); domExpanded = false }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Language
            PrefsItem(label = stringResource(R.string.setting_lang)) {
                ExposedDropdownMenuBox(
                    expanded = langExpanded,
                    onExpandedChange = { langExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentLangName = when(prefs.lang) {
                        "fr" -> stringResource(R.string.lang_fr)
                        "en" -> stringResource(R.string.lang_en)
                        "nl" -> stringResource(R.string.lang_nl)
                        "de" -> stringResource(R.string.lang_de)
                        "es" -> stringResource(R.string.lang_es)
                        else -> prefs.lang
                    }

                    OutlinedTextField(
                        value = currentLangName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false }
                    ) {
                        val langs = listOf("fr", "en", "nl", "de", "es")
                        langs.forEach { lang ->
                            val label = when(lang) {
                                "fr" -> stringResource(R.string.lang_fr)
                                "en" -> stringResource(R.string.lang_en)
                                "nl" -> stringResource(R.string.lang_nl)
                                "de" -> stringResource(R.string.lang_de)
                                "es" -> stringResource(R.string.lang_es)
                                else -> lang
                            }
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.updateLang(lang)
                                    langExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }

            // Date Format
            PrefsItem(label = stringResource(R.string.setting_date_format)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.dateFormat == "european",
                        onClick = { viewModel.updateDateFormat("european") },
                        label = { Text(stringResource(R.string.date_format_european_label)) }
                    )
                    FilterChip(
                        selected = prefs.dateFormat == "iso",
                        onClick = { viewModel.updateDateFormat("iso") },
                        label = { Text(stringResource(R.string.date_format_iso_label)) }
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
            PrefsItem(label = stringResource(R.string.setting_notifications)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (prefs.notificationsEnabled) stringResource(R.string.state_enabled) else stringResource(R.string.state_disabled), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
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
                        Icon(Icons.Default.QrCode, contentDescription = stringResource(R.string.cd_show_qr), tint = MaterialTheme.colorScheme.primary)
                    }

                    if (isChangingHousehold) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { showScanner = true }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.btn_scan), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (householdChangeError) {
                    Text(
                        text = stringResource(R.string.error_household_change_failed),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
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
                        contentDescription = stringResource(R.string.cd_qr_code),
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
