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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mystockmanager.app.R
import com.mystockmanager.app.core.ImageUtils

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
    var photoPath by remember { mutableStateOf<String?>(null) }
    var tempUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val storages by viewModel.storages.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val itemToEdit by viewModel.itemToEdit.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempUri != null) {
            photoPath = ImageUtils.saveImageToInternalStorage(context, tempUri!!)
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
            storageId = item.storageId
            shopId = item.shopId
            expiryDate = item.expiryDate ?: ""
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
                        val uri = ImageUtils.createTempImageUri(context)
                        tempUri = uri
                        cameraLauncher.launch(uri)
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
                
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text(stringResource(R.string.label_unit)) },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(14.dp)
                )
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

            OutlinedTextField(
                value = expiryDate,
                onValueChange = { expiryDate = it },
                label = { Text(stringResource(R.string.label_expiry) + " " + stringResource(R.string.label_date_format_hint)) },
                modifier = Modifier.fillMaxWidth(),
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
                        expiryDate = expiryDate,
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
