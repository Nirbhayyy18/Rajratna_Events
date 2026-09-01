package com.rajratna.events.ui.screens.items

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajratna.events.data.entity.Item
import com.rajratna.events.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ItemsScreen(onNavigateBack: () -> Unit, viewModel: ItemsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<Item?>(null) }
    var itemToDelete by remember { mutableStateOf<Item?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Items & Rates", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, "Add Item")
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.items, key = { it.id }) { item ->
                val rented = viewModel.getRented(item, state.rentedMap)
                val available = viewModel.getAvailable(item, state.rentedMap)
                val isLowStock = item.totalStock > 0 && available <= item.lowStockAlert
                val isOutOfStock = item.totalStock > 0 && available == 0

                ItemStockCard(
                    item = item,
                    rented = rented,
                    available = available,
                    isLowStock = isLowStock,
                    isOutOfStock = isOutOfStock,
                    onEdit = { editItem = item },
                    onToggleActive = { viewModel.toggleActive(item.id, it) },
                    modifier = Modifier.animateItemPlacement()
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        ItemDialog(
            title = "Add Item",
            onDismiss = { showAddDialog = false },
            onSave = { name, rate, totalStock, lowStockAlert ->
                viewModel.addItem(name, rate, totalStock, lowStockAlert)
                showAddDialog = false
            }
        )
    }
    editItem?.let { item ->
        ItemDialog(
            title = "Edit Item",
            initialName = item.name,
            initialRate = item.ratePerDay.toInt().toString(),
            initialTotalStock = item.totalStock.toString(),
            initialLowStockAlert = item.lowStockAlert.toString(),
            onDismiss = { editItem = null },
            onSave = { name, rate, totalStock, lowStockAlert ->
                viewModel.updateItem(
                    item.copy(
                        name = name,
                        ratePerDay = rate,
                        totalStock = totalStock,
                        lowStockAlert = lowStockAlert
                    )
                )
                editItem = null
            },
            onDelete = if (item.name.equals("Chair", ignoreCase = true) ||
                item.name.equals("Table", ignoreCase = true) ||
                item.name.equals("Water Jar", ignoreCase = true)
            ) null else {
                {
                    itemToDelete = item
                    editItem = null
                }
            }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Item?") },
            text = { Text("This item will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        val currentItem = item
                        itemToDelete = null
                        viewModel.deleteItem(
                            item = currentItem,
                            onSuccess = {
                                android.widget.Toast.makeText(context, "Item deleted successfully", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { message ->
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Item Card with Stock Info ─────────────────────────────────────

@Composable
private fun ItemStockCard(
    item: Item,
    rented: Int,
    available: Int,
    isLowStock: Boolean,
    isOutOfStock: Boolean,
    onEdit: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine stock status
    val stockDotColor = when {
        !item.isActive  -> MaterialTheme.colorScheme.outline
        isOutOfStock    -> StatusCancelled
        isLowStock      -> StatusPending
        else            -> StatusCompleted
    }
    val stockLabel = when {
        !item.isActive  -> "Inactive"
        isOutOfStock    -> "All Booked"
        isLowStock      -> "Low Stock ($available available)"
        item.totalStock > 0 -> "In Stock ($available available)"
        else            -> "No stock set"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.isActive)
                MaterialTheme.colorScheme.surfaceContainerLow
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            when {
                isOutOfStock -> StatusCancelledBorder.copy(alpha = 0.6f)
                isLowStock   -> StatusPendingBorder.copy(alpha = 0.6f)
                else         -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // ── Top row: Name + Rate + Controls ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    // Stock status dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(stockDotColor, CircleShape)
                    )
                    Column {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "₹${item.ratePerDay.toInt()} / day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Edit, "Edit", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Switch(
                        checked = item.isActive,
                        onCheckedChange = onToggleActive
                    )
                }
            }

            // ── Stock status label ──
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stockLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = stockDotColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ── Stock info grid ──
            if (item.totalStock > 0) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StockStat(label = "Total", value = item.totalStock.toString())
                    StockStat(
                        label = "Available",
                        value = available.toString(),
                        valueColor = when {
                            isOutOfStock -> StatusCancelled
                            isLowStock   -> StatusPending
                            else         -> StatusCompleted
                        }
                    )
                    StockStat(label = "Rented", value = rented.toString(), valueColor = if (rented > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    StockStat(label = "Alert at", value = "≤${item.lowStockAlert}")
                }
            }
        }
    }
}

@Composable
private fun StockStat(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = valueColor
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// ── Add/Edit Item Dialog ──────────────────────────────────────────

@Composable
private fun ItemDialog(
    title: String,
    initialName: String = "",
    initialRate: String = "",
    initialTotalStock: String = "",
    initialLowStockAlert: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, rate: Double, totalStock: Int, lowStockAlert: Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var rate by remember { mutableStateOf(initialRate) }
    var totalStock by remember { mutableStateOf(initialTotalStock) }
    var lowStockAlert by remember { mutableStateOf(initialLowStockAlert) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Rate per Day (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = totalStock,
                    onValueChange = { totalStock = it },
                    label = { Text("Total Stock") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("Total items you own") }
                )
                OutlinedTextField(
                    value = lowStockAlert,
                    onValueChange = { lowStockAlert = it },
                    label = { Text("Low Stock Alert Qty") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("Warn when available ≤ this") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedRate = rate.toDoubleOrNull()
                    val parsedStock = totalStock.toIntOrNull() ?: 0
                    val parsedAlert = lowStockAlert.toIntOrNull() ?: 0
                    if (name.isNotBlank() && parsedRate != null) {
                        onSave(name, parsedRate, maxOf(0, parsedStock), maxOf(0, parsedAlert))
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
