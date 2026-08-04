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
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mystockmanager.app.R
import com.mystockmanager.app.data.local.entities.ShopEntity
import com.mystockmanager.app.data.local.entities.StorageEntity

@Composable
fun StoragesScreen(
    viewModel: StoragesViewModel = hiltViewModel()
) {
    val storages by viewModel.storages.collectAsState()
    val shops by viewModel.shops.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }

    var showAddStorage by remember { mutableStateOf(false) }
    var showAddShop by remember { mutableStateOf(false) }
    
    var editingStorage by remember { mutableStateOf<StorageEntity?>(null) }
    var editingShop by remember { mutableStateOf<ShopEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.manage_places_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            val tabTitles = listOf(stringResource(R.string.tab_storages), stringResource(R.string.label_shop))
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
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
                            StorageCard(
                                storage = storage, 
                                onDelete = { viewModel.deleteStorage(storage) },
                                onClick = {
                                    editingStorage = storage
                                    showAddStorage = true
                                }
                            )
                        }
                    }
                }
            } else {
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
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showAddStorage) {
        AddStorageDialog(
            existingStorage = editingStorage,
            onDismiss = { showAddStorage = false },
            onConfirm = { name, icon, type ->
                if (editingStorage != null) {
                    viewModel.updateStorage(editingStorage!!.copy(name = name, icon = icon, type = type))
                } else {
                    viewModel.createStorage(name, icon, type)
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
}

@Composable
fun StorageCard(storage: StorageEntity, onDelete: () -> Unit, onClick: () -> Unit) {
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

@Composable
fun AddStorageDialog(
    existingStorage: StorageEntity? = null,
    onDismiss: () -> Unit, 
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(existingStorage?.name ?: "") }
    var type by remember { mutableStateOf(existingStorage?.type ?: "dry") }
    var icon by remember { mutableStateOf(existingStorage?.icon ?: "📦") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, icon, type) },
                enabled = name.isNotBlank()
            ) { Text(if (existingStorage == null) stringResource(R.string.btn_add) else stringResource(R.string.btn_modify)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        },
        title = { Text(if (existingStorage == null) stringResource(R.string.title_new_storage) else stringResource(R.string.title_edit_storage)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.label_name)) }, modifier = Modifier.fillMaxWidth())
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
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.label_name)) }, modifier = Modifier.fillMaxWidth())
        }
    )
}
