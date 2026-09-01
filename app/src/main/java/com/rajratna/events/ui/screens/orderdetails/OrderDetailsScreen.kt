package com.rajratna.events.ui.screens.orderdetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
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
    orderId: String,
    onNavigateBack: () -> Unit,
    onEditOrder: (String) -> Unit,
    onNavigateToBillPreview: (String) -> Unit = {},
    viewModel: OrderDetailsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showDeliverConfirm by remember { mutableStateOf(false) }
    var showRecordReturnDialog by remember { mutableStateOf(false) }
    var showBillSheet by remember { mutableStateOf(false) }
    var returnEntries by remember { mutableStateOf<List<LocalReturnEntry>>(emptyList()) }

    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }

    LaunchedEffect(showRecordReturnDialog, state.orderItems) {
        if (showRecordReturnDialog) {
            returnEntries = state.orderItems
                .filter { !it.isCustomerOwned && it.quantity > (it.returnedQuantity + it.damagedQuantity) }
                .map { item ->
                    LocalReturnEntry(
                        orderItemId = item.id,
                        itemName = item.itemName,
                        quantity = item.quantity,
                        alreadyReturned = item.returnedQuantity,
                        pendingQuantity = item.quantity - item.returnedQuantity - item.damagedQuantity,
                        returnedNow = 0
                    )
                }
        }
    }

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
//                item {
//                    Card(
//                        shape = RoundedCornerShape(16.dp),
//                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
//                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//                    ) {
//                        Column(Modifier.padding(20.dp)) {
//                            Text("👤 Customer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
//                            Spacer(Modifier.height(12.dp))
//                            Text(order.customerName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
//                            Text(order.customerMobile, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
//                            if (order.customerAddress.isNotBlank()) {
//                                Spacer(Modifier.height(4.dp))
//                                Text(order.customerAddress, style = MaterialTheme.typography.bodyMedium)
//                            }
//                            Spacer(Modifier.height(16.dp))
//                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                                FilledTonalButton(onClick = { WhatsAppUtils.callCustomer(context, order.customerMobile) }) {
//                                    Icon(Icons.Default.Call, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Call")
//                                }
//                                FilledTonalButton(onClick = { showBillSheet = true }) {
//                                    Icon(Icons.Default.Receipt, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Bill")
//                                }
//                                if (order.balanceAmount > 0) {
//                                    FilledTonalButton(onClick = {
//                                        WhatsAppUtils.shareOnWhatsApp(context, order.customerMobile, WhatsAppUtils.generatePaymentReminder(order))
//                                    }) {
//                                        Icon(Icons.Default.NotificationsActive, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp));Text("Rem")
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {

                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {

                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        "Customer",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        WhatsAppUtils.callCustomer(
                                            context,
                                            order.customerMobile
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call")
                                }

                                Spacer(Modifier.width(8.dp))

                                FilledTonalIconButton(
                                    onClick = {
                                        showBillSheet = true
                                    }
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = "Bill")
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                order.customerName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                order.customerMobile,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (order.customerAddress.isNotBlank()) {

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    order.customerAddress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                // Dates
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("📅 Dates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text("Order", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(DateUtils.formatDate(order.orderDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
                                Column { Text("Delivery", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(DateUtils.formatDate(order.deliveryDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
                                Column { Text("Return", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(DateUtils.formatDate(order.returnDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Rental Days: ${order.rentalDays}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            if (order.notes.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text("Notes: ${order.notes}", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
                // Items
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("📦 Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            state.orderItems.forEach { item ->
                                val displayName = if (item.isCustomerOwned) "${item.itemName} (Customer Jar)" else item.itemName
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("$displayName (${item.quantity} × ${item.ratePerDay.toInt()} × ${item.rentalDays}d)", style = MaterialTheme.typography.bodyMedium)
                                    Text(item.totalAmount.toRupee(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
                // Bill Summary
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("💰 Bill Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            AmountRow("Items Total", order.itemsTotal)
                            if (order.transportRent > 0) AmountRow("Transport Rent", order.transportRent)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            AmountRow("Grand Total", order.grandTotal, isBold = true, color = MaterialTheme.colorScheme.primary)
                            AmountRow("Paid", order.grandTotal - order.balanceAmount, color = StatusCompleted)
                            AmountRow("Balance", order.balanceAmount, isBold = true, color = if (order.balanceAmount > 0) MaterialTheme.colorScheme.error else StatusCompleted)
                        }
                    }
                }
                // Payments history
                if (state.payments.isNotEmpty()) {
                    item { Text("💳 Payment History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)) }
                    items(state.payments) { payment ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(DateUtils.formatDate(payment.paymentDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(payment.paymentMethod, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(payment.amount.toRupee(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusCompleted)
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
                        val hasPendingReturns = state.orderItems.any { !it.isCustomerOwned && it.quantity > (it.returnedQuantity + it.damagedQuantity) }
                        if ((order.orderStatus == OrderStatus.DELIVERED || order.orderStatus == OrderStatus.COMPLETED) && hasPendingReturns) {
                            Button(
                                onClick = { showRecordReturnDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.KeyboardReturn, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Record Return")
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
                                when (status) {
                                    OrderStatus.CANCELLED -> showCancelConfirm = true
                                    OrderStatus.DELIVERED -> showDeliverConfirm = true
                                    OrderStatus.CONFIRMED -> {
                                        viewModel.updateStatus(status) {
                                            android.widget.Toast.makeText(context, "Order Confirmed successfully", android.widget.Toast.LENGTH_SHORT).show()
                                            showBillSheet = true
                                        }
                                    }
                                    OrderStatus.COMPLETED -> {
                                        val pendingItems = state.orderItems.filter { !it.isCustomerOwned && it.quantity > (it.returnedQuantity + it.damagedQuantity) }
                                        if (pendingItems.isNotEmpty()) {
                                            android.widget.Toast.makeText(context, "Please record item returns before completing this order.", android.widget.Toast.LENGTH_LONG).show()
                                            showRecordReturnDialog = true
                                        } else {
                                            viewModel.updateStatus(status) {
                                                android.widget.Toast.makeText(context, "Order completed successfully", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    else -> {
                                        viewModel.updateStatus(status) {}
                                    }
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
                viewModel.recordPayment(amount, method, notes) {
                    android.widget.Toast.makeText(context, "Payment recorded successfully", android.widget.Toast.LENGTH_SHORT).show()
                }
                showPaymentDialog = false
            }
        )
    }

    // Confirm Cancellation Dialog
    if (showCancelConfirm && order != null) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel Order?") },
            text = { Text("Are you sure you want to cancel this order? Cancelled orders will not count in stock or income.") },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirm = false
                        viewModel.updateStatus(OrderStatus.CANCELLED) {
                            android.widget.Toast.makeText(context, "Order cancelled successfully", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text("No")
                }
            }
        )
    }

    // Confirm Delivery Dialog
    if (showDeliverConfirm && order != null) {
        AlertDialog(
            onDismissRequest = { showDeliverConfirm = false },
            title = { Text("Mark as Delivered?") },
            text = { Text("This means items have been sent to customer and will affect stock/returns.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeliverConfirm = false
                        viewModel.updateStatus(OrderStatus.DELIVERED) {
                            android.widget.Toast.makeText(context, "Order marked as delivered", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Mark Delivered")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeliverConfirm = false }) {
                    Text("No")
                }
            }
        )
    }

    // Record Return Dialog
    if (showRecordReturnDialog && order != null) {
        AlertDialog(
            onDismissRequest = { showRecordReturnDialog = false },
            title = { Text("Record Return", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(returnEntries) { entry ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(entry.itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sent: ${entry.quantity}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Returned: ${entry.alreadyReturned}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Pending: ${entry.pendingQuantity}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Returned Now:", style = MaterialTheme.typography.bodyMedium)
                                    
                                    IconButton(
                                        onClick = {
                                            returnEntries = returnEntries.map { e ->
                                                if (e.orderItemId == entry.orderItemId) {
                                                    e.copy(returnedNow = maxOf(0, e.returnedNow - 1))
                                                } else e
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, null)
                                    }
                                    
                                    Text(
                                        text = entry.returnedNow.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    
                                    IconButton(
                                        onClick = {
                                            returnEntries = returnEntries.map { e ->
                                                if (e.orderItemId == entry.orderItemId) {
                                                    e.copy(returnedNow = minOf(e.pendingQuantity, e.returnedNow + 1))
                                                } else e
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            returnEntries = returnEntries.map { e ->
                                e.copy(returnedNow = e.pendingQuantity)
                            }
                        }
                    ) {
                        Text("Mark All")
                    }
                    Button(
                        onClick = {
                            val returnMap = returnEntries
                                .filter { it.returnedNow > 0 }
                                .associate { it.orderItemId to it.returnedNow }
                            
                            if (returnMap.isNotEmpty()) {
                                viewModel.recordReturn(returnMap) {
                                    android.widget.Toast.makeText(context, "Return recorded successfully", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            showRecordReturnDialog = false
                        },
                        enabled = returnEntries.any { it.returnedNow > 0 }
                    ) {
                        Text("Save Return")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordReturnDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bill Options Bottom Sheet
    if (showBillSheet && order != null) {
        BillOptionsSheet(
            order = order,
            orderItems = state.orderItems,
            totalPaid = order.grandTotal - order.balanceAmount,
            onDismiss = { showBillSheet = false },
            onPreviewBill = { onNavigateToBillPreview(orderId) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentDialog(maxAmount: Double, onDismiss: () -> Unit, onConfirm: (Double, String, String) -> Unit) {
    var amount by remember { mutableStateOf(maxAmount.toInt().toString()) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember { mutableStateOf("") }

//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text("Record Payment") },
//        text = {
//            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
//                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
//                Text("Payment Method", style = MaterialTheme.typography.labelMedium)
//                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                    PaymentMethod.all.forEach { m ->
//                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m, style = MaterialTheme.typography.labelSmall) })
//                    }
//                }
//                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
//            }
//        },
//        confirmButton = {
//            Button(onClick = { amount.toDoubleOrNull()?.let { onConfirm(it, method, notes) } }) { Text("Save") }
//        },
//        dismissButton = {
//            TextButton(onClick = onDismiss) { Text("Cancel") }
//        }
//    )

    AlertDialog(
        onDismissRequest = onDismiss,

        shape = RoundedCornerShape(24.dp),

        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "Record Payment",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },

        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {

                // Amount

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Amount")
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.CurrencyRupee,
                            contentDescription = null
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Payment Method

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    PaymentMethod.all.forEach { payment ->

                        val icon = when (payment) {
                            "Cash" -> Icons.Default.Payments
                            "UPI" -> Icons.Default.PhoneAndroid
                            "Bank" -> Icons.Default.AccountBalance
                            else -> Icons.Default.MoreHoriz
                        }

                        FilterChip(
                            selected = method == payment,
                            onClick = {
                                method = payment
                            },

                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },

                            label = {
                                Text(
                                    payment,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                // Notes

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Notes (Optional)")
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = null
                        )
                    },
                    minLines = 3,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },

        confirmButton = {

            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let {
                        onConfirm(it, method, notes)
                    }
                }
            ) {

                Icon(
                    Icons.Default.Check,
                    contentDescription = null
                )

                Spacer(Modifier.width(6.dp))

                Text("Save")
            }
        },

        dismissButton = {

            OutlinedButton(
                onClick = onDismiss
            ) {

                Icon(
                    Icons.Default.Close,
                    contentDescription = null
                )

                Spacer(Modifier.width(6.dp))

                Text("Cancel")
            }
        }
    )
}


data class LocalReturnEntry(
    val orderItemId: String,
    val itemName: String,
    val quantity: Int,
    val alreadyReturned: Int,
    val pendingQuantity: Int,
    val returnedNow: Int
)
