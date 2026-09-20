package com.mystockmanager.app.ui.storages

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mystockmanager.app.R
import com.mystockmanager.app.core.InputValidator
import com.mystockmanager.app.core.UnitTranslator
import com.mystockmanager.app.data.local.entities.DomicileEntity
import com.mystockmanager.app.data.local.entities.ShopEntity
import com.mystockmanager.app.data.local.entities.StorageEntity
import com.mystockmanager.app.data.local.entities.UnitEntity
import com.mystockmanager.app.ui.components.RecipesIconButton

@Composable
fun StoragesScreen(
    onOpenRecipes: () -> Unit = {},
    viewModel: StoragesViewModel = hiltViewModel()
) {
    val domiciles by viewModel.domiciles.collectAsState()
    val storages by viewModel.storages.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val units by viewModel.units.collectAsState()
    val activeDomicileId by viewModel.activeDomicileId.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.addDefaultDataIfEmpty()
        viewModel.error.collect { msgRes ->
            snackbarHostState.showSnackbar(context.getString(msgRes))
        }
    }
    
    var selectedTab by remember { mutableIntStateOf(0) }

    var showAddDomicile by remember { mutableStateOf(false) }
    var showAddStorage by remember { mutableStateOf(false) }
    var showAddShop by remember { mutableStateOf(false) }
    var showAddUnit by remember { mutableStateOf(false) }
    
    var editingDomicile by remember { mutableStateOf<DomicileEntity?>(null) }
    var editingStorage by remember { mutableStateOf<StorageEntity?>(null) }
    var editingShop by remember { mutableStateOf<ShopEntity?>(null) }
    var editingUnit by remember { mutableStateOf<UnitEntity?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        // Scaffold imbriqué : ne pas réappliquer les marges des barres système, déjà
        // prises en charge par le Scaffold principal (sinon le titre descend trop bas).
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.manage_places_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                RecipesIconButton(onClick = onOpenRecipes)
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 0.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                val tabTitles = listOf(
                    stringResource(R.string.tab_domiciles),
                    stringResource(R.string.tab_storages), 
                    stringResource(R.string.label_shop),
                    stringResource(R.string.tab_units)
                )
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        // Domiciles Section
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.my_domiciles),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                IconButton(onClick = { 
                                    editingDomicile = null
                                    showAddDomicile = true 
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_add), tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(domiciles) { domicile ->
                                    DomicileItem(
                                        domicile = domicile, 
                                        onDelete = { viewModel.deleteDomicile(domicile) },
                                        onClick = {
                                            editingDomicile = domicile
                                            showAddDomicile = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Storages Section
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.my_storages),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                IconButton(onClick = { 
                                    editingStorage = null
                                    showAddStorage = true 
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_add), tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(storages) { storage ->
                                    val domicileName = domiciles.find { it.id == storage.domicileId }?.name ?: ""
                                    StorageCard(
                                        storage = storage,
                                        domicileName = domicileName,
                                        onDelete = { viewModel.deleteStorage(storage) },
                                        onClick = {
                                            editingStorage = storage
                                            showAddStorage = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Shops Section
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.my_shops),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                IconButton(onClick = { 
                                    editingShop = null
                                    showAddShop = true 
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_add), tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(shops) { shop ->
                                    ShopItem(
                                        shop = shop, 
                                        onDelete = { viewModel.deleteShop(shop) },
                                        onClick = {
                                            editingShop = shop
                                            showAddShop = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    3 -> {
                        // Units Section
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.tab_units),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                IconButton(onClick = { 
                                    editingUnit = null
                                    showAddUnit = true 
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_add), tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(units) { unit ->
                                    UnitItem(
                                        unit = unit, 
                                        onDelete = { viewModel.deleteUnit(unit) },
                                        onClick = {
                                            editingUnit = unit
                                            showAddUnit = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddDomicile) {
        AddDomicileDialog(
            existingDomicile = editingDomicile,
            onDismiss = { showAddDomicile = false },
            onConfirm = { name ->
                if (editingDomicile != null) {
                    viewModel.updateDomicile(editingDomicile!!.copy(name = name))
                } else {
                    viewModel.createDomicile(name)
                }
                showAddDomicile = false
            }
        )
    }

    if (showAddStorage) {
        AddStorageDialog(
            existingStorage = editingStorage,
            domiciles = domiciles,
            defaultDomicileId = activeDomicileId,
            onDismiss = { showAddStorage = false },
            onConfirm = { name, icon, type, domicileId ->
                if (editingStorage != null) {
                    viewModel.updateStorage(editingStorage!!.copy(name = name, icon = icon, type = type, domicileId = domicileId))
                } else {
                    viewModel.createStorage(name, icon, type, domicileId)
                }
                showAddStorage = false
            }
        )
    }

    if (showAddShop) {
        AddShopDialog(
            existingShop = editingShop,
            onDismiss = { showAddShop = false },
            onConfirm = { name ->
                if (editingShop != null) {
                    viewModel.updateShop(editingShop!!.copy(name = name))
                } else {
                    viewModel.createShop(name)
                }
                showAddShop = false
            }
        )
    }

    if (showAddUnit) {
        AddUnitDialog(
            existingUnit = editingUnit,
            onDismiss = { showAddUnit = false },
            onConfirm = { name, label ->
                if (editingUnit != null) {
                    viewModel.updateUnit(editingUnit!!.copy(name = name, label = label))
                } else {
                    viewModel.createUnit(name, label)
                }
                showAddUnit = false
            }
        )
    }
}

@Composable
fun DomicileItem(domicile: DomicileEntity, onDelete: () -> Unit, onClick: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(domicile.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            title = { Text(stringResource(R.string.confirm_delete_domicile_title)) },
            text = { Text(stringResource(R.string.confirm_delete_domicile_msg, domicile.name)) }
        )
    }
}

@Composable
fun AddDomicileDialog(
    existingDomicile: DomicileEntity? = null,
    onDismiss: () -> Unit, 
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(existingDomicile?.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text(if (existingDomicile == null) stringResource(R.string.btn_add) else stringResource(R.string.btn_modify)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        },
        title = { Text(if (existingDomicile == null) stringResource(R.string.title_new_domicile) else stringResource(R.string.title_edit_domicile)) },
        text = {
            OutlinedTextField(
                value = name, 
                onValueChange = { name = InputValidator.filterAlphanumericSpace(it) }, 
                label = { Text(stringResource(R.string.label_name)) }, 
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }
    )
}

@Composable
fun UnitItem(unit: UnitEntity, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { },
                    onTap = { onClick() }
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚖️", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(UnitTranslator.getTranslatedName(unit), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(UnitTranslator.getTranslatedLabel(unit), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AddUnitDialog(
    existingUnit: UnitEntity? = null,
    onDismiss: () -> Unit, 
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(existingUnit?.name ?: "") }
    var label by remember { mutableStateOf(existingUnit?.label ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && label.isNotBlank()) onConfirm(name, label) },
                enabled = name.isNotBlank() && label.isNotBlank()
            ) { Text(if (existingUnit == null) stringResource(R.string.btn_add) else stringResource(R.string.btn_modify)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        },
        title = { Text(if (existingUnit == null) stringResource(R.string.title_new_unit) else stringResource(R.string.title_edit_unit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = InputValidator.filterAlphanumericSpace(it) },
                    label = { Text(stringResource(R.string.label_unit_full_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = InputValidator.filterAlphanumericSpace(it) },
                    label = { Text(stringResource(R.string.label_unit_abbrev)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    )
}

@Composable
fun StorageCard(storage: StorageEntity, domicileName: String, onDelete: () -> Unit, onClick: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showDeleteConfirm = true },
                    onTap = { onClick() }
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
            
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(storage.icon, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(storage.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                
                // Affichage du lieu (Domicile)
                if (domicileName.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = domicileName, 
                            fontSize = 10.sp, 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = when(storage.type) {
                        "cold" -> stringResource(R.string.storage_type_frais)
                        "frozen" -> stringResource(R.string.storage_type_congele)
                        else -> stringResource(R.string.storage_type_sec)
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete()
                    showDeleteConfirm = false 
                }) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            title = { Text(stringResource(R.string.confirm_delete_storage_title)) },
            text = { Text(stringResource(R.string.confirm_delete_storage_msg, storage.name)) }
        )
    }
}

@Composable
fun ShopItem(shop: ShopEntity, onDelete: () -> Unit, onClick: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showDeleteConfirm = true },
                    onTap = { onClick() }
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏬", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(shop.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete()
                    showDeleteConfirm = false 
                }) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            title = { Text(stringResource(R.string.confirm_delete_shop_title)) },
            text = { Text(stringResource(R.string.confirm_delete_shop_msg, shop.name)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStorageDialog(
    existingStorage: StorageEntity? = null,
    domiciles: List<DomicileEntity> = emptyList(),
    defaultDomicileId: String? = null,
    onDismiss: () -> Unit, 
    onConfirm: (String, String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf(existingStorage?.name ?: "") }
    var type by remember { mutableStateOf(existingStorage?.type ?: "dry") }
    var icon by remember { mutableStateOf(existingStorage?.icon ?: "📦") }
    var domicileId by remember { mutableStateOf(existingStorage?.domicileId ?: defaultDomicileId) }
    var domExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, icon, type, domicileId) },
                enabled = name.isNotBlank()
            ) { Text(if (existingStorage == null) stringResource(R.string.btn_add) else stringResource(R.string.btn_modify)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        },
        title = { Text(if (existingStorage == null) stringResource(R.string.title_new_storage) else stringResource(R.string.title_edit_storage)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = InputValidator.filterAlphanumericSpace(it) }, 
                    label = { Text(stringResource(R.string.label_name)) }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                
                Text(stringResource(R.string.label_domicile), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ExposedDropdownMenuBox(
                    expanded = domExpanded,
                    onExpandedChange = { domExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentDomName = domiciles.find { it.id == domicileId }?.name ?: stringResource(R.string.label_none)
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
                        DropdownMenuItem(text = { Text(stringResource(R.string.label_none)) }, onClick = { domicileId = null; domExpanded = false })
                        domiciles.forEach { dom ->
                            DropdownMenuItem(text = { Text(dom.name) }, onClick = { domicileId = dom.id; domExpanded = false })
                        }
                    }
                }

                Text(stringResource(R.string.label_storage_type), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "dry", onClick = { type = "dry"; icon = "📦" }, label = { Text(stringResource(R.string.storage_type_sec)) })
                    FilterChip(selected = type == "cold", onClick = { type = "cold"; icon = "❄️" }, label = { Text(stringResource(R.string.storage_type_frais)) })
                    FilterChip(selected = type == "frozen", onClick = { type = "frozen"; icon = "🧊" }, label = { Text(stringResource(R.string.storage_type_congele)) })
                }
            }
        }
    )
}

@Composable
fun AddShopDialog(
    existingShop: ShopEntity? = null,
    onDismiss: () -> Unit, 
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(existingShop?.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text(if (existingShop == null) stringResource(R.string.btn_add) else stringResource(R.string.btn_modify)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        },
        title = { Text(if (existingShop == null) stringResource(R.string.title_new_shop) else stringResource(R.string.title_edit_shop)) },
        text = {
            OutlinedTextField(
                value = name, 
                onValueChange = { name = InputValidator.filterAlphanumericSpace(it) }, 
                label = { Text(stringResource(R.string.label_name)) }, 
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }
    )
}
