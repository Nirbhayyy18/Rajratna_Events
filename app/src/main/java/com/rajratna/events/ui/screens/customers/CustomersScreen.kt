package com.rajratna.events.ui.screens.customers

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
import com.rajratna.events.ui.components.EmptyState
import com.rajratna.events.util.WhatsAppUtils
import com.rajratna.events.util.toRupee

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCustomer: (Long) -> Unit,
    viewModel: CustomersViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customers", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearch(it) },
                placeholder = { Text("Search by name or mobile") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp)
            )
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (state.customers.isEmpty()) {
                EmptyState(Icons.Default.People, "No customers yet", "Customers are created automatically when you add orders")
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.customers, key = { it.customer.id }) { cs ->
                        Card(onClick = { onNavigateToCustomer(cs.customer.id) }, shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(cs.customer.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Text(cs.customer.mobileNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row {
                                        IconButton(onClick = { WhatsAppUtils.callCustomer(context, cs.customer.mobileNumber) }) {
                                            Icon(Icons.Default.Call, "Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { WhatsAppUtils.shareOnWhatsApp(context, cs.customer.mobileNumber, "Hello ${cs.customer.name}") }) {
                                            Icon(Icons.Default.Share, "WhatsApp", tint = com.rajratna.events.ui.theme.StatusCompleted, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    MiniStat("Orders", cs.totalOrders.toString())
                                    MiniStat("Total", cs.totalAmount.toRupee())
                                    MiniStat("Paid", cs.totalPaid.toRupee())
                                    MiniStat("Pending", cs.pendingBalance.toRupee(), isHighlight = cs.pendingBalance > 0)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, isHighlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (isHighlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
