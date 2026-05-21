package com.rajratna.events.ui.screens.orders

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajratna.events.data.entity.Order
import com.rajratna.events.data.entity.OrderStatus
import com.rajratna.events.data.entity.PaymentStatusType
import com.rajratna.events.ui.components.EmptyState
import com.rajratna.events.ui.components.StatusChip
import com.rajratna.events.util.DateUtils
import com.rajratna.events.util.WhatsAppUtils
import com.rajratna.events.util.toRupee
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrdersListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOrder: (Long) -> Unit,
    onNavigateToNewOrder: () -> Unit,
    viewModel: OrdersListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var customStart by remember { mutableLongStateOf(System.currentTimeMillis()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orders", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewOrder,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Order")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Search Bar ──────────────────────────────
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search name, mobile, bill no.") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp)
            )

            // ── Date Filter Chips + Calendar + Advanced ─
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(viewModel.dateFilters) { filter ->
                    FilterChip(
                        selected = state.dateFilter == filter,
                        onClick = { viewModel.selectDateFilter(filter) },
                        label = { Text(filter, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                // Calendar chip
                item {
                    FilterChip(
                        selected = state.dateFilter == "Custom",
                        onClick = { showStartPicker = true },
                        label = { Text("📅", style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                // Advanced filter chip
                item {
                    FilterChip(
                        selected = viewModel.hasActiveAdvancedFilters,
                        onClick = { viewModel.toggleAdvancedFilters() },
                        label = {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // ── Active filters indicator ────────────────
            if (viewModel.hasActiveAdvancedFilters || state.dateFilter == "Custom") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val parts = mutableListOf<String>()
                    if (state.dateFilter == "Custom" && state.customDateStart != null) {
                        parts.add("${DateUtils.formatShortDate(state.customDateStart!!)} - ${DateUtils.formatShortDate(state.customDateEnd!!)}")
                    }
                    if (state.statusFilter != null) parts.add(state.statusFilter!!)
                    if (state.paymentFilter != null) parts.add(state.paymentFilter!!)
                    if (state.customerFilter.isNotBlank()) parts.add("Customer: ${state.customerFilter}")

                    Text(
                        text = "Filters: ${parts.joinToString(" · ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearAllFilters() }) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // ── Orders List ─────────────────────────────
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.orders.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Receipt,
                    title = "No orders found",
                    message = "Create your first order with the + button"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.orders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            onClick = { onNavigateToOrder(order.id) },
                            onCall = { WhatsAppUtils.callCustomer(context, order.customerMobile) },
                            onWhatsApp = {
                                WhatsAppUtils.shareOnWhatsApp(
                                    context, order.customerMobile,
                                    WhatsAppUtils.generatePaymentReminder(order)
                                )
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── Advanced Filters Bottom Sheet ────────────────────
    if (state.showAdvancedFilters) {
        AdvancedFiltersSheet(
            statusFilter = state.statusFilter,
            paymentFilter = state.paymentFilter,
            customerFilter = state.customerFilter,
            onStatusChange = { viewModel.setStatusFilter(it) },
            onPaymentChange = { viewModel.setPaymentFilter(it) },
            onCustomerChange = { viewModel.setCustomerFilter(it) },
            onApply = { viewModel.applyAdvancedFilters() },
            onClear = { viewModel.clearAdvancedFilters() },
            onDismiss = { viewModel.toggleAdvancedFilters() }
        )
    }

    // ── Custom Date Pickers ────────────────────────────────
    if (showStartPicker) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val c = Calendar.getInstance()
                c.set(year, month, day, 0, 0, 0)
                c.set(Calendar.MILLISECOND, 0)
                customStart = c.timeInMillis
                showStartPicker = false
                showEndPicker = true
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Select Start Date")
            setOnCancelListener { showStartPicker = false }
        }.show()
    }

    if (showEndPicker) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val c = Calendar.getInstance()
                c.set(year, month, day, 23, 59, 59)
                c.set(Calendar.MILLISECOND, 999)
                val end = c.timeInMillis + 1
                showEndPicker = false
                viewModel.selectCustomDate(customStart, end)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Select End Date")
            setOnCancelListener { showEndPicker = false }
        }.show()
    }
}

// ═══════════════════════════════════════════════════════════
// ADVANCED FILTERS BOTTOM SHEET
// ═══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AdvancedFiltersSheet(
    statusFilter: String?,
    paymentFilter: String?,
    customerFilter: String,
    onStatusChange: (String?) -> Unit,
    onPaymentChange: (String?) -> Unit,
    onCustomerChange: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Advanced Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Status filter
            Text("Order Status", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val statuses = listOf(null to "All") + OrderStatus.all.map { it to it }
                statuses.forEach { (value, label) ->
                    FilterChip(
                        selected = statusFilter == value,
                        onClick = { onStatusChange(value) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Payment filter
            Text("Payment Status", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val payments = listOf(null to "All") + PaymentStatusType.all.map { it to it }
                payments.forEach { (value, label) ->
                    FilterChip(
                        selected = paymentFilter == value,
                        onClick = { onPaymentChange(value) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Customer filter
            Text("Customer", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = customerFilter,
                onValueChange = onCustomerChange,
                placeholder = { Text("Search by customer name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (customerFilter.isNotBlank()) {
                        IconButton(onClick = { onCustomerChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            )

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear Filters")
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply Filters")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ORDER CARD (kept from original)
// ═══════════════════════════════════════════════════════════

@Composable
private fun OrderCard(
    order: Order,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Bill No + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${order.billNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(order.orderStatus)
                    StatusChip(order.paymentStatus)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Customer Info
            Text(
                text = order.customerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = order.customerMobile,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column {
                    Text("Delivery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateUtils.formatShortDate(order.deliveryDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Return", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateUtils.formatShortDate(order.returnDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${order.rentalDays}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            // Amount + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.grandTotal.toRupee(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (order.balanceAmount > 0) {
                        Text(
                            text = "Balance: ${order.balanceAmount.toRupee()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = onCall,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onWhatsApp,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = com.rajratna.events.ui.theme.StatusCompletedBg)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "WhatsApp",
                            tint = com.rajratna.events.ui.theme.StatusCompleted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
