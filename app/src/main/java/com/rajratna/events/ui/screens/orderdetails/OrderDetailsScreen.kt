package com.rajratna.events.ui.screens.orderdetails

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajratna.events.data.entity.*
import com.rajratna.events.ui.components.*
import com.rajratna.events.util.*
import com.rajratna.events.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    orderId: Long,
    onNavigateBack: () -> Unit,
    onEditOrder: (Long) -> Unit,
    viewModel: OrderDetailsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPaymentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }

    val order = state.order

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order #${order?.billNumber ?: ""}", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (order != null) {
                        IconButton(onClick = { onEditOrder(orderId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading || order == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status chips
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(order.orderStatus)
                        StatusChip(order.paymentStatus)
                    }
                }
                // Customer info
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("👤 Customer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text(order.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(order.customerMobile, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (order.customerAddress.isNotBlank()) Text(order.customerAddress, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = { WhatsAppUtils.callCustomer(context, order.customerMobile) }) {
                                    Icon(Icons.Default.Call, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Call")
                                }
                                FilledTonalButton(onClick = {
                                    WhatsAppUtils.shareOnWhatsApp(context, order.customerMobile, WhatsAppUtils.generateBillMessage(order, state.orderItems))
                                }) {
                                    Icon(Icons.Default.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Bill")
                                }
                                if (order.balanceAmount > 0) {
                                    FilledTonalButton(onClick = {
                                        WhatsAppUtils.shareOnWhatsApp(context, order.customerMobile, WhatsAppUtils.generatePaymentReminder(order))
                                    }) {
                                        Icon(Icons.Default.NotificationsActive, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Remind")
                                    }
                                }
                            }
                        }
                    }
                }
                // Dates
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("📅 Dates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text("Order", style = MaterialTheme.typography.labelSmall); Text(DateUtils.formatDate(order.orderDate)) }
                                Column { Text("Delivery", style = MaterialTheme.typography.labelSmall); Text(DateUtils.formatDate(order.deliveryDate)) }
                                Column { Text("Return", style = MaterialTheme.typography.labelSmall); Text(DateUtils.formatDate(order.returnDate)) }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Rental Days: ${order.rentalDays}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            if (order.notes.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text("Notes: ${order.notes}", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                // Items
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("📦 Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            state.orderItems.forEach { item ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${item.itemName} (${item.quantity} × ${item.ratePerDay.toInt()} × ${item.rentalDays}d)")
                                    Text(item.totalAmount.toRupee(), fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
                // Bill Summary
                item {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("💰 Bill Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            AmountRow("Items Total", order.itemsTotal)
                            if (order.transportRent > 0) AmountRow("Transport Rent", order.transportRent)
                            HorizontalDivider()
                            AmountRow("Grand Total", order.grandTotal, isBold = true, color = MaterialTheme.colorScheme.primary)
                            AmountRow("Paid", order.grandTotal - order.balanceAmount, color = StatusCompleted)
                            AmountRow("Balance", order.balanceAmount, isBold = true, color = if (order.balanceAmount > 0) MaterialTheme.colorScheme.error else StatusCompleted)
                        }
                    }
                }
                // Payments history
                if (state.payments.isNotEmpty()) {
                    item { Text("💳 Payment History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp)) }
                    items(state.payments) { payment ->
                        Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(DateUtils.formatDate(payment.paymentDate), style = MaterialTheme.typography.bodySmall)
                                    Text(payment.paymentMethod, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(payment.amount.toRupee(), fontWeight = FontWeight.Bold, color = StatusCompleted)
                            }
                        }
                    }
                }
                // Action buttons
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (order.balanceAmount > 0) {
                            Button(onClick = { showPaymentDialog = true }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Payment, null); Spacer(Modifier.width(8.dp)); Text("Record Payment")
                            }
                        }
                        val statusActions = buildList {
                            if (order.orderStatus == OrderStatus.PENDING) add(OrderStatus.CONFIRMED to "Confirm Order")
                            if (order.orderStatus == OrderStatus.CONFIRMED) add(OrderStatus.DELIVERED to "Mark Delivered")
                            if (order.orderStatus == OrderStatus.DELIVERED) add(OrderStatus.COMPLETED to "Mark Completed")
                            if (order.orderStatus != OrderStatus.CANCELLED && order.orderStatus != OrderStatus.COMPLETED) add(OrderStatus.CANCELLED to "Cancel Order")
                        }
                        statusActions.forEach { (status, label) ->
                            OutlinedButton(onClick = {
                                viewModel.updateStatus(status)
                                if (status == OrderStatus.CONFIRMED) {
                                    WhatsAppUtils.shareOnWhatsApp(context, order.customerMobile, WhatsAppUtils.generateOrderConfirmation(order))
                                }
                            }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                                Text(label)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    // Payment Dialog
    if (showPaymentDialog && order != null) {
        PaymentDialog(
            maxAmount = order.balanceAmount,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, method, notes ->
                viewModel.recordPayment(amount, method, notes)
                showPaymentDialog = false
            }
        )
    }
}

@Composable
private fun PaymentDialog(maxAmount: Double, onDismiss: () -> Unit, onConfirm: (Double, String, String) -> Unit) {
    var amount by remember { mutableStateOf(maxAmount.toInt().toString()) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Text("Payment Method", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethod.all.forEach { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(onClick = { amount.toDoubleOrNull()?.let { onConfirm(it, method, notes) } }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
