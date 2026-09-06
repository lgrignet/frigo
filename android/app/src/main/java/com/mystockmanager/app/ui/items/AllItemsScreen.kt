package com.mystockmanager.app.ui.items

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mystockmanager.app.R
import com.mystockmanager.app.core.UnitTranslator
import com.mystockmanager.app.data.local.entities.ItemEntity
import com.mystockmanager.app.data.local.entities.StorageEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllItemsScreen(
    onEditItem: (String) -> Unit,
    onSearchRecipes: (List<String>) -> Unit,
    viewModel: AllItemsViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val warningDays by viewModel.warningDays.collectAsState()
    val activeDomicileId by viewModel.activeDomicileId.collectAsState()
    val domiciles by viewModel.domiciles.collectAsState()
    val storages by viewModel.storages.collectAsState()
    val selectedDomId by viewModel.selectedDomicileId.collectAsState()
    val selectedStorId by viewModel.selectedStorageId.collectAsState()

    val recentExpiryClears by viewModel.recentExpiryClears.collectAsState()
    val dateFormatPref by viewModel.dateFormat.collectAsState()

    var domExpanded by remember { mutableStateOf(false) }
    var storExpanded by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val addedMessage = stringResource(R.string.msg_added_to_shopping)
    val expiryClearedMessage = stringResource(R.string.msg_expiry_cleared_suffix)
    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(Unit) {
        viewModel.message.collect { productName ->
            snackbarHostState.showSnackbar("$productName : $addedMessage")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.expiryCleared.collect { cleared ->
            val result = snackbarHostState.showSnackbar(
                message = "${cleared.itemName} : $expiryClearedMessage",
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreExpiry(cleared)
            }
        }
    }

    LaunchedEffect(activeDomicileId) {
        if (selectedDomId == null && activeDomicileId != null) {
            viewModel.setFilterDomicile(activeDomicileId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (selectedItemIds.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_cancel))
                        }
                        Text(
                            stringResource(R.string.selection_count, selectedItemIds.size),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Button(
                        onClick = { onSearchRecipes(selectedItemIds.toList()) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_search_recipes))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.tab_products),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (recentExpiryClears.isNotEmpty()) {
                        IconButton(onClick = { showHistoryDialog = true }) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = stringResource(R.string.cd_expiry_history),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (showHistoryDialog) {
                val dateFormatter = remember(dateFormatPref) {
                    if (dateFormatPref == "iso") DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    else DateTimeFormatter.ofPattern("dd/MM/yyyy")
                }
                AlertDialog(
                    onDismissRequest = { showHistoryDialog = false },
                    title = { Text(stringResource(R.string.expiry_history_dialog_title)) },
                    text = {
                        if (recentExpiryClears.isEmpty()) {
                            Text(stringResource(R.string.expiry_history_empty))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                recentExpiryClears.forEach { entry ->
                                    val formattedDate = try {
                                        val date = if (entry.previousExpiryDate.contains("-")) LocalDate.parse(entry.previousExpiryDate)
                                                    else LocalDate.parse(entry.previousExpiryDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                        date.format(dateFormatter)
                                    } catch (e: Exception) { entry.previousExpiryDate }

                                    val minutesAgo = try {
                                        ChronoUnit.MINUTES.between(java.time.Instant.parse(entry.clearedAt), java.time.Instant.now())
                                    } catch (e: Exception) { 0L }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(entry.itemName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(
                                                stringResource(R.string.expiry_history_previous_date, formattedDate),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                if (minutesAgo < 1) stringResource(R.string.expiry_history_cleared_now)
                                                else stringResource(R.string.expiry_history_cleared_ago, minutesAgo.toInt()),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        TextButton(onClick = {
                                            viewModel.restoreExpiry(entry)
                                            if (recentExpiryClears.size <= 1) showHistoryDialog = false
                                        }) {
                                            Text(stringResource(R.string.btn_restore))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showHistoryDialog = false }) { Text(stringResource(R.string.btn_close)) }
                    }
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                placeholder = { Text(stringResource(R.string.msg_search_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            )

            // Filters (Dropdowns)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Domicile Dropdown
                ExposedDropdownMenuBox(
                    expanded = domExpanded,
                    onExpandedChange = { domExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    val currentDomName = if (selectedDomId == null) stringResource(R.string.filter_all_domiciles)
                    else domiciles.find { it.id == selectedDomId }?.name ?: stringResource(R.string.filter_all_domiciles)

                    OutlinedTextField(
                        value = currentDomName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_domicile), fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = domExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                    )

                    ExposedDropdownMenu(
                        expanded = domExpanded,
                        onDismissRequest = { domExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.filter_all_domiciles)) },
                            onClick = {
                                viewModel.setFilterDomicile(null)
                                domExpanded = false
                            }
                        )
                        domiciles.forEach { dom ->
                            DropdownMenuItem(
                                text = { Text(dom.name) },
                                onClick = {
                                    viewModel.setFilterDomicile(dom.id)
                                    domExpanded = false
                                }
                            )
                        }
                    }
                }

                // Storage Dropdown
                ExposedDropdownMenuBox(
                    expanded = storExpanded,
                    onExpandedChange = { storExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    val currentStorName = if (selectedStorId == null) stringResource(R.string.filter_all_storages)
                    else storages.find { it.id == selectedStorId }?.let { "${it.icon} ${it.name}" } ?: stringResource(R.string.filter_all_storages)

                    OutlinedTextField(
                        value = currentStorName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_storage), fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = storExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                    )

                    ExposedDropdownMenu(
                        expanded = storExpanded,
                        onDismissRequest = { storExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.filter_all_storages)) },
                            onClick = {
                                viewModel.setFilterStorage(null)
                                storExpanded = false
                            }
                        )
                        storages.filter { selectedDomId == null || it.domicileId == selectedDomId }.forEach { stor ->
                            DropdownMenuItem(
                                text = { Text("${stor.icon} ${stor.name}") },
                                onClick = {
                                    viewModel.setFilterStorage(stor.id)
                                    storExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isEmpty()) stringResource(R.string.msg_empty_list) else stringResource(R.string.msg_no_results),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        val itemStorage = storages.find { it.id == item.storageId }
                        val itemDomicileId = itemStorage?.domicileId
                        
                        // Permission : Modifiable si c'est le domicile actif OU si le produit n'a pas de domicile (orphelin)
                        val canEdit = itemDomicileId == null || itemDomicileId == activeDomicileId

                        ItemRow(
                            item = item,
                            warningDays = warningDays,
                            canEdit = canEdit,
                            storageName = itemStorage?.let { "${it.icon} ${it.name}" },
                            selectionMode = selectedItemIds.isNotEmpty(),
                            isSelected = selectedItemIds.contains(item.id),
                            onDelete = { viewModel.deleteItem(item) },
                            onAdjustQuantity = { delta -> viewModel.adjustQuantity(item, delta) },
                            onAddToShopping = { viewModel.addToShoppingList(item) },
                            onClick = { if (canEdit) onEditItem(item.id) },
                            onToggleSelect = { viewModel.toggleItemSelection(item.id) },
                            onLongClick = { viewModel.toggleItemSelection(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ItemRow(
    item: ItemEntity,
    warningDays: Int,
    canEdit: Boolean,
    storageName: String? = null,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onDelete: () -> Unit,
    onAdjustQuantity: (Double) -> Unit,
    onAddToShopping: () -> Unit = {},
    onClick: () -> Unit,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val borderColor = remember(item.expiryDate, warningDays) {
        val dateStr = item.expiryDate
        if (dateStr.isNullOrBlank()) return@remember Color.Transparent
        
        try {
            val expiryDate = if (dateStr.contains("-")) LocalDate.parse(dateStr) 
                            else LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val today = LocalDate.now()
            val daysUntil = ChronoUnit.DAYS.between(today, expiryDate)
            
            when {
                daysUntil < 0 -> Color(0xFFFF5252)
                daysUntil <= warningDays -> Color(0xFFFFD740)
                else -> Color.Transparent
            }
        } catch (e: Exception) {
            Color.Transparent
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = canEdit || selectionMode,
                onClick = { if (selectionMode) onToggleSelect() else onClick() },
                onLongClick = { if (!selectionMode) onLongClick() }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                canEdit -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                 else if (borderColor != Color.Transparent) BorderStroke(2.dp, borderColor)
                 else AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            // Photo or initials
            Box(modifier = Modifier.size(60.dp)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (!item.photo.isNullOrBlank()) {
                        AsyncImage(
                            model = item.photo,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📦", fontSize = 28.sp)
                        }
                    }
                }
                
                // Requestor initials badge
                if (!item.requestorInitials.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        border = BorderStroke(1.dp, Color.White)
                    ) {
                        Text(
                            text = item.requestorInitials,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    color = if (canEdit) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.quantity} ${UnitTranslator.translateLabel(item.unit)}${if (storageName != null) " • $storageName" else ""}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (canEdit) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Boutons de réglage rapide
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ).padding(horizontal = 4.dp)
                        ) {
                            IconButton(onClick = { onAdjustQuantity(-1.0) }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Remove, 
                                    contentDescription = stringResource(R.string.btn_minus), 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            IconButton(onClick = { onAdjustQuantity(1.0) }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Add, 
                                    contentDescription = stringResource(R.string.btn_plus), 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onAddToShopping,
                            modifier = Modifier.size(36.dp).background(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                        ) {
                            Icon(
                                Icons.Default.AddShoppingCart, 
                                contentDescription = stringResource(R.string.btn_add_to_shopping), 
                                tint = MaterialTheme.colorScheme.onSecondaryContainer, 
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = stringResource(R.string.btn_delete), 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = stringResource(R.string.label_read_only),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.label_read_only),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
