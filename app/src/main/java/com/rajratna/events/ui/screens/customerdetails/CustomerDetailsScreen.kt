package com.rajratna.events.ui.screens.customerdetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajratna.events.ui.components.*
import com.rajratna.events.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    customerId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToOrder: (Long) -> Unit,
    onNavigateToNewOrder: () -> Unit,
    viewModel: CustomerDetailsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(customerId) { viewModel.loadCustomer(customerId) }
    val customer = state.customer
    val orders = state.orders
    val activeOrders = orders.filter { it.orderStatus != "Cancelled" }

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
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(customer.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(customer.mobileNumber, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (customer.address.isNotBlank()) Text(customer.address, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = { WhatsAppUtils.callCustomer(context, customer.mobileNumber) }) { Icon(Icons.Default.Call, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Call") }
                                FilledTonalButton(onClick = { WhatsAppUtils.shareOnWhatsApp(context, customer.mobileNumber, "Hello ${customer.name}") }) { Icon(Icons.Default.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("WhatsApp") }
                            }
                        }
                    }
                }
                // Stats
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Total Orders", orders.size.toString(), Icons.Default.Receipt, modifier = Modifier.weight(1f))
                        StatCard("Total Amount", activeOrders.sumOf { it.grandTotal }.toRupee(), Icons.Default.AccountBalanceWallet, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Paid", activeOrders.sumOf { it.grandTotal - it.balanceAmount }.toRupee(), Icons.Default.CheckCircle, iconTint = com.rajratna.events.ui.theme.StatusCompleted, modifier = Modifier.weight(1f))
                        StatCard("Pending", activeOrders.sumOf { it.balanceAmount }.toRupee(), Icons.Default.Warning, iconTint = com.rajratna.events.ui.theme.PaymentUnpaid, modifier = Modifier.weight(1f))
                    }
                }
                // Order history
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
}
