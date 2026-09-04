package com.mystockmanager.app.ui.items

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mystockmanager.app.R
import com.mystockmanager.app.core.DateVoiceParser
import com.mystockmanager.app.core.ImageUtils
import com.mystockmanager.app.core.InputValidator
import com.mystockmanager.app.core.UnitTranslator
import com.mystockmanager.app.core.VoiceRecognitionManager
import com.mystockmanager.app.ui.theme.Accent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormScreen(
    itemId: String? = null,
    onSaveSuccess: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ItemFormViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf(context.getString(R.string.unit_piece_label)) }
    
    var storageId by remember { mutableStateOf("") }
    var domicileId by remember { mutableStateOf<String?>(null) }
    
    var shopId by remember { mutableStateOf<String?>(null) }
    var expiryDate by remember { mutableStateOf("") }
    var restockThreshold by remember { mutableStateOf("0") }
    var restockBuyQuantity by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf<String?>(null) }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var tempUri by remember { mutableStateOf<android.net.Uri?>(null) }

    var showScanner by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    val domiciles by viewModel.domiciles.collectAsState()
    val storages by viewModel.storages.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val units by viewModel.units.collectAsState()
    val itemToEdit by viewModel.itemToEdit.collectAsState()
    val isLoadingProduct by viewModel.isLoadingProduct.collectAsState()
    val dateFormatPref by viewModel.dateFormat.collectAsState()
    val langPref by viewModel.lang.collectAsState()
    val activeDomicileId by viewModel.activeDomicileId.collectAsState()

    val voiceManager = remember { VoiceRecognitionManager(context) }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_anim")
    val micAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_alpha"
    )

    val dateFormatter = remember(dateFormatPref) {
        if (dateFormatPref == "iso") DateTimeFormatter.ofPattern("yyyy-MM-dd")
        else DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    LaunchedEffect(Unit) {
        viewModel.productFoundName.collect { foundName ->
            name = foundName
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.stopListening()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, context.getString(R.string.toast_mic_permission_granted), Toast.LENGTH_SHORT).show()
        }
    }

    fun handleVoiceResult(text: String) {
        val parsedDate = DateVoiceParser.parse(text, langPref)
        if (parsedDate != null) {
            expiryDate = parsedDate.format(dateFormatter)
            Toast.makeText(context, context.getString(R.string.toast_date_understood, expiryDate), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(R.string.toast_date_not_understood, text), Toast.LENGTH_SHORT).show()
        }
    }

    if (showScanner) {
        com.mystockmanager.app.ui.components.QRScanner(
            onScan = { scannedBarcode ->
                barcode = scannedBarcode
                viewModel.onBarcodeScanned(scannedBarcode)
                showScanner = false
            },
            onClose = { showScanner = false }
        )
        return
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                if (expiryDate.isNotBlank()) {
                    LocalDate.parse(expiryDate, dateFormatter)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                } else null
            } catch (e: Exception) { null }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        expiryDate = date.format(dateFormatter)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.btn_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempUri != null) {
            photoPath = ImageUtils.saveImageToInternalStorage(context, tempUri!!)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = ImageUtils.createTempImageUri(context)
            tempUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(itemId) {
        if (itemId != null) {
            viewModel.loadItem(itemId)
        }
    }

    LaunchedEffect(itemToEdit) {
        itemToEdit?.let { item ->
            name = item.name
            quantity = item.quantity.toString()
            unit = item.unit
            barcode = item.barcode
            storageId = item.storageId
            
            val currentStor = storages.find { it.id == item.storageId }
            domicileId = currentStor?.domicileId ?: activeDomicileId
            
            shopId = item.shopId
            
            expiryDate = item.expiryDate?.let { dateStr ->
                try {
                    val date = if (dateStr.contains("-")) LocalDate.parse(dateStr) 
                               else LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    date.format(dateFormatter)
                } catch(e: Exception) { dateStr }
            } ?: ""
            
            restockThreshold = item.restockThreshold.toString()
            restockBuyQuantity = item.restockBuyQuantity.toString()
            notes = item.notes
            photoPath = item.photo
        }
    }

    LaunchedEffect(activeDomicileId, itemId) {
        if (domicileId == null && itemId == null) {
            domicileId = activeDomicileId
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            onSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null) stringResource(R.string.title_new_product) else stringResource(R.string.title_edit_product), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                navigationIcon = {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.btn_cancel), color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val uri = ImageUtils.createTempImageUri(context)
                            tempUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (photoPath != null) {
                    AsyncImage(
                        model = photoPath,
                        contentDescription = stringResource(R.string.cd_item_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.btn_add), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }

            // Barcode Button
            OutlinedButton(
                onClick = { showScanner = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (barcode == null) stringResource(R.string.btn_scan_barcode) else stringResource(R.string.label_barcode_value, barcode ?: ""))
                if (isLoadingProduct) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = InputValidator.filterAlphanumericSpace(it) },
                label = { Text(stringResource(R.string.label_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = InputValidator.filterDecimal(it) },
                    label = { Text(stringResource(R.string.label_quantity)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
                
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(1.5f)
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = InputValidator.filterAlphanumericSpace(it) },
                        label = { Text(stringResource(R.string.label_unit)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        units.forEach { unitItem ->
                            DropdownMenuItem(
                                text = { Text(UnitTranslator.getTranslatedLabel(unitItem)) },
                                onClick = {
                                    unit = unitItem.label
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Domicile selection
            Text(stringResource(R.string.label_domicile), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = domicileId == null,
                    onClick = { domicileId = null; storageId = "" },
                    label = { Text(stringResource(R.string.label_none)) },
                    shape = RoundedCornerShape(10.dp)
                )
                domiciles.forEach { dom ->
                    FilterChip(
                        selected = domicileId == dom.id,
                        onClick = { 
                            domicileId = dom.id
                            val storInDom = storages.filter { it.domicileId == dom.id }
                            if (storInDom.isNotEmpty() && storages.find { it.id == storageId }?.domicileId != dom.id) {
                                storageId = storInDom.first().id
                            }
                        },
                        label = { Text(dom.name) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Rangement selection (Filtered by domicile)
            Text(stringResource(R.string.label_storage), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            val filteredStorages = remember(domicileId, storages) {
                if (domicileId == null) storages.filter { it.domicileId == null }
                else storages.filter { it.domicileId == domicileId }
            }

            if (filteredStorages.isEmpty()) {
                Text(if (domicileId == null) stringResource(R.string.msg_no_storage_defined) else stringResource(R.string.msg_no_storage_in_domicile), fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredStorages.forEach { storage ->
                    FilterChip(
                        selected = storageId == storage.id,
                        onClick = { storageId = storage.id },
                        label = { Text("${storage.icon} ${storage.name}") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Magasin selection
            Text(stringResource(R.string.label_shop), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = shopId == null,
                    onClick = { shopId = null },
                    label = { Text(stringResource(R.string.label_none)) },
                    shape = RoundedCornerShape(10.dp)
                )
                shops.forEach { shop ->
                    FilterChip(
                        selected = shopId == shop.id,
                        onClick = { shopId = shop.id },
                        label = { Text(shop.name) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Expiry Date with Picker AND Voice
            OutlinedTextField(
                value = expiryDate,
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.label_expiry)) },
                trailingIcon = { 
                    Row {
                        IconButton(onClick = { 
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                if (isListening) {
                                    voiceManager.stopListening()
                                } else {
                                    val recognitionLang = when(langPref) {
                                        "fr" -> "fr-FR"
                                        "en" -> "en-US"
                                        "nl" -> "nl-NL"
                                        "de" -> "de-DE"
                                        "es" -> "es-ES"
                                        else -> "fr-FR"
                                    }
                                    voiceManager.startListening(
                                        language = recognitionLang,
                                        onResult = { handleVoiceResult(it) },
                                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                                        onStatusChange = { isListening = it }
                                    )
                                }
                            } else {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = stringResource(R.string.cd_dictate_date),
                                tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.graphicsLayer { alpha = if (isListening) micAlpha else 1f }
                            )
                        }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.cd_pick_date))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = restockThreshold,
                onValueChange = { restockThreshold = InputValidator.filterDigits(it) },
                label = { Text(stringResource(R.string.label_restock)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = restockBuyQuantity,
                onValueChange = { restockBuyQuantity = InputValidator.filterDecimal(it) },
                label = { Text(stringResource(R.string.label_restock_quantity)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.label_notes)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.saveItem(
                        id = itemId,
                        name = name,
                        quantity = quantity.toDoubleOrNull() ?: 1.0,
                        unit = unit,
                        barcode = barcode,
                        expiryDate = if (expiryDate.isNotBlank()) expiryDate else null,
                        storageId = storageId,
                        shopId = shopId,
                        photo = photoPath,
                        restockThreshold = restockThreshold.toIntOrNull() ?: 0,
                        restockBuyQuantity = restockBuyQuantity.toDoubleOrNull() ?: 1.0,
                        notes = notes
                    )
                },
                enabled = name.isNotBlank() && storageId.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(if (itemId == null) stringResource(R.string.btn_save) else stringResource(R.string.btn_modify), fontWeight = FontWeight.Bold)
            }
        }
    }
}
