package com.rajratna.events.ui.screens.customerdetails

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajratna.events.data.entity.PaymentStatusType
import com.rajratna.events.data.repository.JarEntry
import com.rajratna.events.ui.components.*
import com.rajratna.events.ui.theme.*
import com.rajratna.events.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    customerId: String,
    onNavigateBack: () -> Unit,
    onNavigateToOrder: (String) -> Unit,
    onNavigateToNewOrder: () -> Unit,
    viewModel: CustomerDetailsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(customerId) { viewModel.loadCustomer(customerId) }
    val customer = state.customer
    val orders = state.orders
    val activeOrders = orders.filter { it.orderStatus != "Cancelled" }
    val jarStats = state.jarStats

    // Show action messages
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "Customer", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNewOrder, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, "New Order")
            }
        }
    ) { padding ->
        if (state.isLoading || customer == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Customer info card
//                item {
//                    Card(shape = RoundedCornerShape(14.dp)) {
//                        Column(Modifier.padding(16.dp)) {
//                            Text(customer.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
//                            Text(customer.mobileNumber, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
//                            if (customer.address.isNotBlank()) Text(customer.address, style = MaterialTheme.typography.bodyMedium)
//                            Spacer(Modifier.height(12.dp))
//                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                                FilledTonalButton(onClick = { WhatsAppUtils.callCustomer(context, customer.mobileNumber) }) { Icon(Icons.Default.Call, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Call") }
//                                FilledTonalButton(onClick = { WhatsAppUtils.shareOnWhatsApp(context, customer.mobileNumber, "Hello ${customer.name}") }) { Icon(Icons.Default.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("WhatsApp") }
//                            }
//                        }
//                    }
//                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // Avatar
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = customer.name.first().uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            // Customer Details
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {

                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = customer.mobileNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (customer.address.isNotBlank()) {
                                    Text(
                                        text = customer.address,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            // Call Button
                            FilledTonalIconButton(
                                onClick = {
                                    WhatsAppUtils.callCustomer(
                                        context,
                                        customer.mobileNumber
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "Call Customer"
                                )
                            }
                        }
                    }
                }

                // ── Jar Summary Card ────────────────────────────────
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = TealContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Jar Summary - This Month",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Teal40
                            )
                            Spacer(Modifier.height(12.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                JarSummaryItem("Jars", "${jarStats.thisMonthJarCount}", Teal40)
                                JarSummaryItem("Amount", jarStats.thisMonthJarAmount.toRupee(), Teal40)
                                JarSummaryItem("Paid", jarStats.thisMonthPaid.toRupee(), StatusCompleted)
                                JarSummaryItem("Balance", jarStats.pendingBalance.toRupee(), if (jarStats.pendingBalance > 0) PaymentUnpaid else StatusCompleted)
                            }

                            if (jarStats.pendingReturnJars > 0) {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = Teal40.copy(alpha = 0.2f))
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.SwapHoriz, null, tint = StatusPending, modifier = Modifier.size(18.dp))
                                    Text(
                                        "Pending Return: ${jarStats.pendingReturnJars} jars",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StatusPending
                                    )
                                }
                            }
                        }
                    }
                }

//                // ── Quick Action Buttons ─────────────────────────────
//                item {
//                    Row(
//                        Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        // + Jar
//                        Button(
//                            onClick = { viewModel.openQuickJar() },
//                            modifier = Modifier.weight(1f).height(44.dp),
//                            shape = RoundedCornerShape(12.dp),
//                            colors = ButtonDefaults.buttonColors(containerColor = Teal40)
//                        ) {
//                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
//                            Spacer(Modifier.width(4.dp))
//                            Text("Jar", fontWeight = FontWeight.Bold)
//                        }
//
//                        // Return Jar
//                        if (jarStats.pendingReturnJars > 0) {
//                            OutlinedButton(
//                                onClick = { viewModel.openReturnJar() },
//                                modifier = Modifier.weight(1f).height(44.dp),
//                                shape = RoundedCornerShape(12.dp)
//                            ) {
//                                Icon(Icons.Default.AssignmentReturn, null, Modifier.size(18.dp))
//                                Spacer(Modifier.width(4.dp))
//                                Text("Return")
//                            }
//                        }
//
//                        // Record Payment
//                        if (jarStats.pendingBalance > 0) {
//                            OutlinedButton(
//                                onClick = { viewModel.openRecordPayment() },
//                                modifier = Modifier.weight(1f).height(44.dp),
//                                shape = RoundedCornerShape(12.dp)
//                            ) {
//                                Icon(Icons.Default.Payment, null, Modifier.size(18.dp))
//                                Spacer(Modifier.width(4.dp))
//                                Text("Pay")
//                            }
//                        }
//
//                        // WhatsApp Summary
//                        OutlinedButton(
//                            onClick = {
//                                val message = WhatsAppUtils.generateCustomerJarSummary(
//                                    customer.name,
//                                    jarStats.thisMonthJarCount,
//                                    jarStats.thisMonthJarAmount,
//                                    jarStats.totalPaid,
//                                    jarStats.pendingBalance,
//                                    jarStats.pendingReturnJars
//                                )
//                                WhatsAppUtils.shareOnWhatsApp(context, customer.mobileNumber, message)
//                            },
//                            modifier = Modifier.height(44.dp),
//                            shape = RoundedCornerShape(12.dp),
//                            contentPadding = PaddingValues(horizontal = 12.dp)
//                        ) {
//                            Icon(Icons.Default.Chat, null, Modifier.size(18.dp))
//                        }
//                    }
//                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        val buttonModifier = Modifier
                            .weight(1f)
                            .height(48.dp)

                        // + Jar
                        Button(
                            onClick = { viewModel.openQuickJar() },
                            modifier = buttonModifier,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal40),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "Jar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        // Return Jar
                        if (jarStats.pendingReturnJars > 0) {
                            OutlinedButton(
                                onClick = { viewModel.openReturnJar() },
                                modifier = buttonModifier,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.AssignmentReturn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "Return",
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // Payment
                        if (jarStats.pendingBalance > 0) {
                            OutlinedButton(
                                onClick = { viewModel.openRecordPayment() },
                                modifier = buttonModifier,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Payment,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "Pay",
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // WhatsApp
                        OutlinedButton(
                            onClick = {
                                val message = WhatsAppUtils.generateCustomerJarSummary(
                                    customer.name,
                                    jarStats.thisMonthJarCount,
                                    jarStats.thisMonthJarAmount,
                                    jarStats.totalPaid,
                                    jarStats.pendingBalance,
                                    jarStats.pendingReturnJars
                                )
                                WhatsAppUtils.shareOnWhatsApp(
                                    context,
                                    customer.mobileNumber,
                                    message
                                )
                            },
                            modifier = buttonModifier,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "Share",
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                // ── Overall Stats ────────────────────────────────────
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Total Orders", orders.size.toString(), Icons.Default.Receipt, modifier = Modifier.weight(1f))
                        StatCard("Total Amount", activeOrders.sumOf { it.grandTotal }.toRupee(), Icons.Default.AccountBalanceWallet, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Paid", activeOrders.sumOf { it.grandTotal - it.balanceAmount }.toRupee(), Icons.Default.CheckCircle, iconTint = StatusCompleted, modifier = Modifier.weight(1f))
                        StatCard("Pending", activeOrders.sumOf { it.balanceAmount }.toRupee(), Icons.Default.Warning, iconTint = PaymentUnpaid, modifier = Modifier.weight(1f))
                    }
                }

                // ── Recent Jar Entries ───────────────────────────────
                if (state.recentJarEntries.isNotEmpty()) {
                    item { SectionHeader("Recent Jar Entries") }
                    items(state.recentJarEntries) { entry ->
                        JarEntryCard(entry = entry, onClick = { onNavigateToOrder(entry.orderId) })
                    }
                }

                // ── Order History ────────────────────────────────────
                item { SectionHeader("Order History") }
                if (orders.isEmpty()) {
                    item { EmptyState(Icons.Default.Receipt, "No orders", "Create the first order for this customer") }
                } else {
                    items(orders, key = { it.id }) { order ->
                        Card(onClick = { onNavigateToOrder(order.id) }, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                            Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("#${order.billNumber}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(DateUtils.formatDate(order.deliveryDate), style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(order.grandTotal.toRupee(), fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { StatusChip(order.orderStatus); StatusChip(order.paymentStatus) }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // ── Bottom Sheets ────────────────────────────────────────

    if (state.showQuickJar && customer != null) {
        QuickJarBottomSheet(
            customer = customer,
            jarStats = jarStats,
            jarRate = state.waterJarRate,
            availableStock = state.availableJarStock,
            onSave = { quantity, isCustomerOwned, paidAmount, deliveryDate ->
                viewModel.saveQuickJarEntry(quantity, isCustomerOwned, paidAmount, deliveryDate)
            },
            onDismiss = { viewModel.dismissQuickJar() }
        )
    }

    if (state.showReturnJar && customer != null) {
        ReturnJarBottomSheet(
            customer = customer,
            totalPendingJars = jarStats.pendingReturnJars,
            onSave = { returnedNow, damagedNow ->
                viewModel.saveJarReturn(returnedNow, damagedNow)
            },
            onDismiss = { viewModel.dismissReturnJar() }
        )
    }

    if (state.showRecordPayment && customer != null) {
        RecordPaymentBottomSheet(
            customer = customer,
            pendingAmount = jarStats.pendingBalance,
            onSave = { amount, method ->
                viewModel.saveLumpSumPayment(amount, method)
            },
            onDismiss = { viewModel.dismissRecordPayment() }
        )
    }
}

// ═══════════════════════════════════════════════════════════
// JAR SUMMARY ITEM
// ═══════════════════════════════════════════════════════════

@Composable
private fun JarSummaryItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════
// JAR ENTRY CARD
// ═══════════════════════════════════════════════════════════

@Composable
private fun JarEntryCard(
    entry: JarEntry,
    onClick: () -> Unit
) {
    val jarLabel = if (entry.isCustomerOwned) "customer jars" else "jars"
    val paymentColor = when (entry.paymentStatus) {
        PaymentStatusType.PAID -> StatusCompleted
        PaymentStatusType.PARTIALLY_PAID -> StatusPending
        else -> PaymentUnpaid
    }
    val paymentLabel = when (entry.paymentStatus) {
        PaymentStatusType.PAID -> "paid"
        PaymentStatusType.PARTIALLY_PAID -> "partial"
        else -> "unpaid"
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.WaterDrop, null, tint = Teal40, modifier = Modifier.size(20.dp))
                Column {
                    Text(
                        DateUtils.formatShortDate(entry.date),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${entry.quantity} $jarLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    entry.amount.toRupee(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    paymentLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = paymentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
