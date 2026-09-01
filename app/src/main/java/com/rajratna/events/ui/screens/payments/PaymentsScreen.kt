package com.rajratna.events.ui.screens.payments

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajratna.events.ui.components.*
import com.rajratna.events.ui.theme.*
import com.rajratna.events.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOrder: (String) -> Unit,
    viewModel: PaymentsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadPayments() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Payments", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Summary cards
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { StatCard("Today Received", state.todayReceived.toRupee(), Icons.Default.AccountBalanceWallet, StatusCompleted, Modifier.width(150.dp)) }
                    item { StatCard("Today Pending", state.todayPending.toRupee(), Icons.Default.Warning, PaymentUnpaid, Modifier.width(150.dp)) }
                    item { StatCard("Week Received", state.weekReceived.toRupee(), Icons.Default.DateRange, Teal40, Modifier.width(150.dp)) }
                    item { StatCard("Month Received", state.monthReceived.toRupee(), Icons.Default.CalendarMonth, Orange40, Modifier.width(150.dp)) }
                    item { StatCard("Overall Pending", state.overallPending.toRupee(), Icons.Default.PriorityHigh, PaymentUnpaid, Modifier.width(150.dp)) }
                }
            }
            item { SectionHeader("Payment History") }
            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (state.payments.isEmpty()) {
                item { EmptyState(Icons.Default.Payment, "No payments yet", "Payments will appear here when recorded") }
            } else {
                items(state.payments, key = { it.id }) { payment ->
                    Card(
                        onClick = { onNavigateToOrder(payment.orderId) },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(payment.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Bill #${payment.orderId} • ${payment.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(DateUtils.formatDate(payment.paymentDate), style = MaterialTheme.typography.labelSmall)
                                if (payment.notes.isNotBlank()) Text(payment.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Text(payment.amount.toRupee(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusCompleted)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
