package com.rajratna.events.ui.screens.returns

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajratna.events.ui.theme.*
import com.rajratna.events.util.DateUtils
import com.rajratna.events.util.WhatsAppUtils
import java.util.Calendar
import android.app.DatePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnsScreen(
    onNavigateToOrderDetails: (String) -> Unit,
    viewModel: ReturnsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Returns",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Text(
                            "Pending Returns",
                            fontWeight = if (state.selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Text(
                            "Returned",
                            fontWeight = if (state.selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (state.selectedTab) {
                    0 -> PendingReturnsTab(
                        state = state,
                        viewModel = viewModel,
                        onNavigateToOrderDetails = onNavigateToOrderDetails
                    )
                    1 -> ReturnedTab(
                        state = state,
                        viewModel = viewModel,
                        onNavigateToOrderDetails = onNavigateToOrderDetails
                    )
                }
            }
        }
    }

    // Record Return Bottom Sheet
    if (state.showRecordReturn) {
        RecordReturnSheet(
            entries = state.returnEntries,
            isSaving = state.isSaving,
            onUpdateReturnedNow = { id, value -> viewModel.updateReturnedNow(id, value) },
            onMarkAllReturned = { viewModel.markAllReturned() },
            onSave = {
                viewModel.saveReturn {
                    android.widget.Toast.makeText(context, "Return recorded successfully", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { viewModel.closeRecordReturn() }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PENDING RETURNS TAB
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PendingReturnsTab(
    state: ReturnsState,
    viewModel: ReturnsViewModel,
    onNavigateToOrderDetails: (String) -> Unit
) {
    val context = LocalContext.current

    Column {
        // Filters
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf(
                PendingFilter.ALL to "All",
                PendingFilter.DUE_TODAY to "Due Today",
                PendingFilter.OVERDUE to "Overdue",
                PendingFilter.UPCOMING to "Upcoming"
            )
            items(filters) { (filter, label) ->
                FilterChip(
                    selected = state.pendingFilter == filter && state.selectedDate == null,
                    onClick = { viewModel.setPendingFilter(filter) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(50)
                )
            }
            item {
                var showDatePicker by remember { mutableStateOf(false) }
                val dateLabel = if (state.selectedDate != null) {
                    "📅 " + DateUtils.formatShortDate(state.selectedDate)
                } else {
                    "📅 Date"
                }
                
                FilterChip(
                    selected = state.selectedDate != null,
                    onClick = { showDatePicker = true },
                    label = { Text(dateLabel, style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(50)
                )

                if (showDatePicker) {
                    val cal = Calendar.getInstance()
                    if (state.selectedDate != null) {
                        cal.timeInMillis = state.selectedDate
                    }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val c = Calendar.getInstance()
                            c.set(year, month, day, 0, 0, 0)
                            c.set(Calendar.MILLISECOND, 0)
                            viewModel.setSelectedDate(c.timeInMillis)
                            showDatePicker = false
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).apply {
                        setOnCancelListener { showDatePicker = false }
                    }.show()
                }
            }
        }

        if (state.selectedDate != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtered by Expected Return: ${DateUtils.formatDate(state.selectedDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = { viewModel.setSelectedDate(null) }) {
                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (state.filteredPendingOrders.isEmpty()) {
            EmptyReturnState(
                icon = Icons.Outlined.CheckCircle,
                message = "No pending returns"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.filteredPendingOrders) { pendingOrder ->
                    PendingReturnCard(
                        pendingOrder = pendingOrder,
                        onCall = { WhatsAppUtils.callCustomer(context, pendingOrder.order.customerMobile) },
                        onWhatsApp = {
                            val pendingItems = pendingOrder.pendingItems
                            WhatsAppUtils.shareOnWhatsApp(
                                context,
                                pendingOrder.order.customerMobile,
                                WhatsAppUtils.generateReturnReminder(pendingOrder.order, pendingItems)
                            )
                        },
                        onRecordReturn = { viewModel.openRecordReturn(pendingOrder.order.id) },
                        onOpenOrder = { onNavigateToOrderDetails(pendingOrder.order.id) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun PendingReturnCard(
    pendingOrder: PendingReturnOrder,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onRecordReturn: () -> Unit,
    onOpenOrder: () -> Unit
) {
    val order = pendingOrder.order

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Name + Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = order.customerMobile,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ReturnStatusBadge(
                    isOverdue = pendingOrder.isOverdue,
                    isDueToday = pendingOrder.isDueToday
                )
            }

            // Bill & Return Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bill #${order.billNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Return: ${DateUtils.formatDate(order.returnDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Pending Items
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "Pending Items:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            pendingOrder.pendingItems.forEach { item ->
                val pending = item.quantity - item.returnedQuantity - item.damagedQuantity
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$pending pending",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PaymentPartial
                    )
                }
            }

            // Actions
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onCall,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = TealContainer)
                ) {
                    Icon(Icons.Default.Call, "Call", Modifier.size(16.dp), tint = Teal40)
                }
                FilledTonalIconButton(
                    onClick = onWhatsApp,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = StatusCompletedBg)
                ) {
                    Icon(Icons.Default.Chat, "WhatsApp", Modifier.size(16.dp), tint = StatusCompleted)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenOrder, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Open Order", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = onRecordReturn,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AssignmentReturn, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Record Return", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// RETURNED TAB
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ReturnedTab(
    state: ReturnsState,
    viewModel: ReturnsViewModel,
    onNavigateToOrderDetails: (String) -> Unit
) {
    val context = LocalContext.current

    Column {
        // Filters
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf(
                ReturnedFilter.TODAY to "Today",
                ReturnedFilter.THIS_WEEK to "This Week",
                ReturnedFilter.THIS_MONTH to "This Month",
                ReturnedFilter.ALL to "All"
            )
            items(filters) { (filter, label) ->
                FilterChip(
                    selected = state.returnedFilter == filter && state.selectedDate == null,
                    onClick = { viewModel.setReturnedFilter(filter) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(50)
                )
            }
            item {
                var showDatePicker by remember { mutableStateOf(false) }
                val dateLabel = if (state.selectedDate != null) {
                    "📅 " + DateUtils.formatShortDate(state.selectedDate)
                } else {
                    "📅 Date"
                }
                
                FilterChip(
                    selected = state.selectedDate != null,
                    onClick = { showDatePicker = true },
                    label = { Text(dateLabel, style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(50)
                )

                if (showDatePicker) {
                    val cal = Calendar.getInstance()
                    if (state.selectedDate != null) {
                        cal.timeInMillis = state.selectedDate
                    }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val c = Calendar.getInstance()
                            c.set(year, month, day, 0, 0, 0)
                            c.set(Calendar.MILLISECOND, 0)
                            viewModel.setSelectedDate(c.timeInMillis)
                            showDatePicker = false
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).apply {
                        setOnCancelListener { showDatePicker = false }
                    }.show()
                }
            }
        }

        if (state.selectedDate != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtered by Returned Date: ${DateUtils.formatDate(state.selectedDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = { viewModel.setSelectedDate(null) }) {
                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (state.filteredReturnedOrders.isEmpty()) {
            EmptyReturnState(
                icon = Icons.Outlined.Inventory,
                message = "No returned records yet"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.filteredReturnedOrders) { returnedOrder ->
                    ReturnedCard(
                        returnedOrder = returnedOrder,
                        onOpenOrder = { onNavigateToOrderDetails(returnedOrder.order.id) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ReturnedCard(
    returnedOrder: ReturnedOrder,
    onOpenOrder: () -> Unit
) {
    val order = returnedOrder.order

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Bill #${order.billNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (returnedOrder.isFullyReturned) StatusCompletedBg else PaymentPartialBg
                ) {
                    Text(
                        text = if (returnedOrder.isFullyReturned) "FULLY RETURNED" else "PARTIALLY RETURNED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (returnedOrder.isFullyReturned) StatusCompleted else PaymentPartial,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Returned: ${DateUtils.formatDate(order.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Items
            returnedOrder.items.forEach { item ->
                if (item.returnedQuantity > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.itemName, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${item.returnedQuantity} returned",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusCompleted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpenOrder) {
                    Text("Open Order", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// RECORD RETURN BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordReturnSheet(
    entries: List<ReturnEntry>,
    isSaving: Boolean,
    onUpdateReturnedNow: (String, Int) -> Unit,
    onMarkAllReturned: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Record Return",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            entries.forEach { entry ->
                ReturnEntryRow(
                    entry = entry,
                    onValueChange = { onUpdateReturnedNow(entry.orderItemId, it) }
                )
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onMarkAllReturned,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mark All Returned")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving && entries.any { it.returnedNow > 0 }
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Return")
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun ReturnEntryRow(
    entry: ReturnEntry,
    onValueChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = entry.itemName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip("Given", entry.givenQuantity.toString())
                InfoChip("Returned", entry.alreadyReturned.toString())
                InfoChip("Pending", entry.pendingQuantity.toString())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Returned Now:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = if (entry.returnedNow == 0) "" else entry.returnedNow.toString(),
                    onValueChange = { text ->
                        val value = text.toIntOrNull() ?: 0
                        onValueChange(value.coerceIn(0, entry.pendingQuantity))
                    },
                    modifier = Modifier.width(100.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    placeholder = { Text("0") }
                )
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// COMMON COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ReturnStatusBadge(isOverdue: Boolean, isDueToday: Boolean) {
    val (label, textColor, backgroundColor) = when {
        isOverdue -> Triple("OVERDUE", PaymentUnpaid, PaymentUnpaidBg)
        isDueToday -> Triple("DUE TODAY", StatusConfirmed, StatusConfirmedBg)
        else -> Triple("UPCOMING", StatusPending, StatusPendingBg)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = backgroundColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyReturnState(
    icon: ImageVector,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
