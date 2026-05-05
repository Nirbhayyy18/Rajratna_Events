package com.rajratna.events.ui.screens.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(onNavigateBack: () -> Unit, viewModel: ItemsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<Item?>(null) }

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
                    onToggleActive = { viewModel.toggleActive(item.id, it) }
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
    onToggleActive: (Boolean) -> Unit
) {
    val warningColor = MaterialTheme.colorScheme.error
    val lowStockColor = MaterialTheme.colorScheme.tertiary

    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.isActive)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            // ── Top row: Name + Rate + Controls ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "₹${item.ratePerDay.toInt()} / day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit", Modifier.size(20.dp))
                    }
                    Switch(
                        checked = item.isActive,
                        onCheckedChange = onToggleActive
                    )
                }
            }

            // ── Stock badge (if applicable) ──
            if (item.totalStock > 0 && (isOutOfStock || isLowStock)) {
                Spacer(Modifier.height(8.dp))
                val badgeColor = if (isOutOfStock) warningColor else lowStockColor
                val badgeText = if (isOutOfStock) "⚠ Out of Stock" else "⚠ Low Stock"
                Text(
                    text = badgeText,
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // ── Stock info grid ──
            if (item.totalStock > 0) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                            isOutOfStock -> warningColor
                            isLowStock -> lowStockColor
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    StockStat(label = "Rented", value = rented.toString())
                    StockStat(label = "Alert at", value = "≤${item.lowStockAlert}")
                }
            } else {
                // Stock not configured yet
                Spacer(Modifier.height(8.dp))
                Text(
                    "Stock not set — tap edit to configure",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
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
    onSave: (name: String, rate: Double, totalStock: Int, lowStockAlert: Int) -> Unit
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
