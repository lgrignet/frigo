package com.mystockmanager.app.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mystockmanager.app.R
import com.mystockmanager.app.data.local.entities.ShoppingEntity
import com.mystockmanager.app.data.local.entities.StorageEntity
import com.mystockmanager.app.ui.theme.Accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    viewModel: ShoppingViewModel = hiltViewModel()
) {
    val items by viewModel.shoppingItems.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val storages by viewModel.storages.collectAsState()
    val units by viewModel.units.collectAsState()
    val filterShopId by viewModel.filterShopId.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingEntity?>(null) }
    var itemToStore by remember { mutableStateOf<ShoppingEntity?>(null) }
    var filterExpanded by remember { mutableStateOf(false) }

    val toBuy = items.filter { !it.checked }
    val bought = items.filter { it.checked }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.tab_shopping),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            IconButton(onClick = { 
                editingItem = null
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_add), tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Shop Filter Dropdown
        ExposedDropdownMenuBox(
            expanded = filterExpanded,
            onExpandedChange = { filterExpanded = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            val currentFilterName = if (filterShopId == null) {
                stringResource(R.string.label_none)
            } else {
                shops.find { it.id == filterShopId }?.name ?: stringResource(R.string.label_none)
            }

            OutlinedTextField(
                value = currentFilterName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_shop)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filterExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            ExposedDropdownMenu(
                expanded = filterExpanded,
                onDismissRequest = { filterExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.label_none)) },
                    onClick = {
                        viewModel.setFilterShopId(null)
                        filterExpanded = false
                    }
                )
                shops.forEach { shop ->
                    DropdownMenuItem(
                        text = { Text(shop.name) },
                        onClick = {
                            viewModel.setFilterShopId(shop.id)
                            filterExpanded = false
                        }
                    )
                }
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.msg_empty_list), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (toBuy.isNotEmpty()) {
                    item {
                        SectionHeader("À acheter")
                    }
                    items(toBuy, key = { it.id }) { item ->
                        ShoppingItemRow(
                            item = item,
                            onToggle = { viewModel.toggleChecked(item) },
                            onDelete = { viewModel.deleteItem(item) },
                            onStore = { 
                                if (item.targetStorageId != null) {
                                    viewModel.moveToStock(item, item.targetStorageId)
                                } else {
                                    itemToStore = item 
                                }
                            },
                            onClick = {
                                editingItem = item
                                showAddDialog = true
                            }
                        )
                    }
                }

                if (bought.isNotEmpty()) {
                    item {
                        SectionHeader("Acheté")
                    }
                    items(bought, key = { it.id }) { item ->
                        ShoppingItemRow(
                            item = item,
                            onToggle = { viewModel.toggleChecked(item) },
                            onDelete = { viewModel.deleteItem(item) },
                            onStore = { 
                                if (item.targetStorageId != null) {
                                    viewModel.moveToStock(item, item.targetStorageId)
                                } else {
                                    itemToStore = item 
                                }
                            },
                            onClick = {
                                editingItem = item
                                showAddDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ShoppingItemDialog(
            existingItem = editingItem,
            shops = shops,
            storages = storages,
            units = units,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, qty, unit, shopId, targetStorageId ->
                if (editingItem != null) {
                    viewModel.updateItem(editingItem!!.copy(
                        name = name, 
                        quantity = qty, 
                        unit = unit, 
                        shopId = shopId,
                        targetStorageId = targetStorageId
                    ))
                } else {
                    viewModel.addItem(name, qty, unit, shopId, targetStorageId)
                }
                showAddDialog = false
            }
        )
    }

    if (itemToStore != null) {
        StorageSelectionDialog(
            storages = storages,
            onDismiss = { itemToStore = null },
            onConfirm = { storageId ->
                viewModel.moveToStock(itemToStore!!, storageId)
                itemToStore = null
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun ShoppingItemRow(
    item: ShoppingEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onStore: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggle() },
                    onLongPress = { onClick() }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (item.checked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                             else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                )
                if (item.quantity.isNotBlank()) {
                    Text(
                        text = "${item.quantity} ${item.unit}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            if (item.checked) {
                IconButton(onClick = onStore) {
                    Icon(Icons.Default.Inventory, contentDescription = "Ranger", tint = Accent)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingItemDialog(
    existingItem: ShoppingEntity? = null,
    shops: List<com.mystockmanager.app.data.local.entities.ShopEntity> = emptyList(),
    storages: List<StorageEntity> = emptyList(),
    units: List<com.mystockmanager.app.data.local.entities.UnitEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var qty by remember { mutableStateOf(existingItem?.quantity ?: "1") }
    var unit by remember { mutableStateOf(existingItem?.unit ?: "pièce(s)") }
    var shopId by remember { mutableStateOf(existingItem?.shopId) }
    var targetStorageId by remember { mutableStateOf(existingItem?.targetStorageId) }
    
    var shopExpanded by remember { mutableStateOf(false) }
    var storageExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, qty, unit, shopId, targetStorageId) },
                enabled = name.isNotBlank()
            ) { Text(if (existingItem == null) stringResource(R.string.btn_add) else stringResource(R.string.btn_modify)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        },
        title = { Text(if (existingItem == null) stringResource(R.string.title_new_shopping_item) else stringResource(R.string.title_edit_shopping_item)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.label_name)) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text(stringResource(R.string.label_quantity)) }, modifier = Modifier.weight(1f))
                    
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
                        ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            units.forEach { unitItem ->
                                DropdownMenuItem(text = { Text(unitItem.label) }, onClick = { unit = unitItem.label; unitExpanded = false })
                            }
                        }
                    }
                }

                // Shop Selection
                ExposedDropdownMenuBox(
                    expanded = shopExpanded,
                    onExpandedChange = { shopExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentShopName = shops.find { it.id == shopId }?.name ?: stringResource(R.string.label_none)
                    OutlinedTextField(
                        value = currentShopName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_shop)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shopExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(expanded = shopExpanded, onDismissRequest = { shopExpanded = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.label_none)) }, onClick = { shopId = null; shopExpanded = false })
                        shops.forEach { shop ->
                            DropdownMenuItem(text = { Text(shop.name) }, onClick = { shopId = shop.id; shopExpanded = false })
                        }
                    }
                }

                // Target Storage Selection
                ExposedDropdownMenuBox(
                    expanded = storageExpanded,
                    onExpandedChange = { storageExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentStorageName = storages.find { it.id == targetStorageId }?.let { "${it.icon} ${it.name}" } ?: stringResource(R.string.label_none)
                    OutlinedTextField(
                        value = currentStorageName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_storage)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = storageExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(expanded = storageExpanded, onDismissRequest = { storageExpanded = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.label_none)) }, onClick = { targetStorageId = null; storageExpanded = false })
                        storages.forEach { storage ->
                            DropdownMenuItem(text = { Text("${storage.icon} ${storage.name}") }, onClick = { targetStorageId = storage.id; storageExpanded = false })
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun StorageSelectionDialog(
    storages: List<StorageEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        },
        title = { Text("Où ranger ce produit ?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (storages.isEmpty()) {
                    Text(stringResource(R.string.msg_no_storage_defined), color = MaterialTheme.colorScheme.error)
                }
                storages.forEach { storage ->
                    Button(
                        onClick = { onConfirm(storage.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text("${storage.icon} ${storage.name}")
                    }
                }
            }
        }
    )
}
