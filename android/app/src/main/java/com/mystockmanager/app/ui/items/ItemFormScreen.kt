package com.mystockmanager.app.ui.items

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mystockmanager.app.core.ImageUtils
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
    var unit by remember { mutableStateOf("pièce(s)") }
    var storageId by remember { mutableStateOf("") }
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

    val storages by viewModel.storages.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val units by viewModel.units.collectAsState()
    val itemToEdit by viewModel.itemToEdit.collectAsState()
    val isLoadingProduct by viewModel.isLoadingProduct.collectAsState()
    val dateFormatPref by viewModel.dateFormat.collectAsState()

    val dateFormatter = remember(dateFormatPref) {
        if (dateFormatPref == "iso") DateTimeFormatter.ofPattern("yyyy-MM-dd")
        else DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    LaunchedEffect(Unit) {
        viewModel.productFoundName.collect { foundName ->
            name = foundName
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
            shopId = item.shopId
            
            // Format existing date to preferred format
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

    LaunchedEffect(storages) {
        if (storageId.isEmpty() && storages.isNotEmpty() && itemId == null) {
            storageId = storages.first().id
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
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val uri = ImageUtils.createTempImageUri(context)
                            tempUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (photoPath != null) {
                    AsyncImage(
                        model = photoPath,
                        contentDescription = "Photo du produit",
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
                Text(if (barcode == null) "Scanner un code-barres" else "Code : $barcode")
                if (isLoadingProduct) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.label_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
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
                        onValueChange = { unit = it },
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
                                text = { Text(unitItem.label) },
                                onClick = {
                                    unit = unitItem.label
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Rangement selection
            Text(stringResource(R.string.label_storage), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            if (storages.isEmpty()) {
                Text(stringResource(R.string.msg_no_storage_defined), fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                storages.forEach { storage ->
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

            // Expiry Date with Picker
            OutlinedTextField(
                value = expiryDate,
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.label_expiry)) },
                trailingIcon = { 
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Choisir une date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = restockThreshold,
                onValueChange = { restockThreshold = it },
                label = { Text(stringResource(R.string.label_restock)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = restockBuyQuantity,
                onValueChange = { restockBuyQuantity = it },
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
