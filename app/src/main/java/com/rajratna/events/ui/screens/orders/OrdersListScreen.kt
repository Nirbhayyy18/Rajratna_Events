package com.rajratna.events.ui.screens.orders

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
import com.rajratna.events.ui.components.EmptyState
import com.rajratna.events.ui.components.StatusChip
import com.rajratna.events.util.DateUtils
import com.rajratna.events.util.WhatsAppUtils
import com.rajratna.events.util.toRupee

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOrder: (Long) -> Unit,
    onNavigateToNewOrder: () -> Unit,
    viewModel: OrdersListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                placeholder = { Text("Search by name, mobile, or bill no.") },
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

            // ── Filter Chips ────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(viewModel.filters) { filter ->
                    FilterChip(
                        selected = state.activeFilter == filter,
                        onClick = { viewModel.applyFilter(filter) },
                        label = { Text(filter, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(10.dp)
                    )
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
}

@Composable
private fun OrderCard(
    order: Order,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Bill No + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${order.billNumber}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(order.orderStatus)
                    StatusChip(order.paymentStatus)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Customer Info
            Text(
                text = order.customerName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = order.customerMobile,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Delivery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateUtils.formatShortDate(order.deliveryDate), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Return", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateUtils.formatShortDate(order.returnDate), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${order.rentalDays}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))

            // Amount + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.grandTotal.toRupee(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (order.balanceAmount > 0) {
                        Text(
                            text = "Balance: ${order.balanceAmount.toRupee()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Row {
                    IconButton(onClick = onCall) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onWhatsApp) {
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
