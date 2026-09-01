package com.rajratna.events.ui.screens.customers

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
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
import com.rajratna.events.ui.theme.*
import com.rajratna.events.util.DateUtils
import com.rajratna.events.util.WhatsAppUtils
import com.rajratna.events.util.toRupee

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCustomer: (String) -> Unit,
    viewModel: CustomersViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                title = { Text("Customers", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearch(it) },
                placeholder = { Text("Search by name, mobile, or address") },
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
                        CustomerJarCard(
                            cs = cs,
                            onCardClick = { onNavigateToCustomer(cs.customer.id) },
                            onCall = { WhatsAppUtils.callCustomer(context, cs.customer.mobileNumber) },
                            onAddJar = { viewModel.openQuickJar(cs.customer) },
                            onReturnJar = { viewModel.openReturnJar(cs.customer) },
                            onRecordPayment = { viewModel.openRecordPayment(cs.customer) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
        }
    }

    // ── Bottom Sheets ────────────────────────────────────────

    if (state.showQuickJar && state.selectedCustomer != null && state.selectedCustomerJarStats != null) {
        QuickJarBottomSheet(
            customer = state.selectedCustomer!!,
            jarStats = state.selectedCustomerJarStats!!,
            jarRate = state.waterJarRate,
            availableStock = state.availableJarStock,
            onSave = { quantity, isCustomerOwned, paidAmount, deliveryDate ->
                viewModel.saveQuickJarEntry(quantity, isCustomerOwned, paidAmount, deliveryDate)
            },
            onDismiss = { viewModel.dismissQuickJar() }
        )
    }

    if (state.showReturnJar && state.selectedCustomer != null && state.selectedCustomerJarStats != null) {
        ReturnJarBottomSheet(
            customer = state.selectedCustomer!!,
            totalPendingJars = state.selectedCustomerJarStats!!.pendingReturnJars,
            onSave = { returnedNow, damagedNow ->
                viewModel.saveJarReturn(returnedNow, damagedNow)
            },
            onDismiss = { viewModel.dismissReturnJar() }
        )
    }

    if (state.showRecordPayment && state.selectedCustomer != null && state.selectedCustomerJarStats != null) {
        RecordPaymentBottomSheet(
            customer = state.selectedCustomer!!,
            pendingAmount = state.selectedCustomerJarStats!!.pendingBalance,
            onSave = { amount, method ->
                viewModel.saveLumpSumPayment(amount, method)
            },
            onDismiss = { viewModel.dismissRecordPayment() }
        )
    }
}

// ═══════════════════════════════════════════════════════════
// CUSTOMER JAR CARD
// ═══════════════════════════════════════════════════════════

@Composable
private fun CustomerJarCard(
    cs: CustomerWithStats,
    onCardClick: () -> Unit,
    onCall: () -> Unit,
    onAddJar: () -> Unit,
    onReturnJar: () -> Unit,
    onRecordPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val jarStats = cs.jarStats
    val hasPendingBalance = jarStats.pendingBalance > 0

    Card(
        onClick = onCardClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (hasPendingBalance)
                PaymentUnpaid.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header: Avatar + Name + Call button
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    // Circular initials avatar
                    InitialsAvatar(name = cs.customer.name, size = 46.dp)
                    Column {
                        Text(
                            cs.customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            cs.customer.mobileNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = onCall,
                    modifier = Modifier.size(38.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = StatusConfirmedBg)
                ) {
                    Icon(Icons.Default.Call, "Call", tint = StatusConfirmed, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            // Jar stats
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    JarStatRow(
                        icon = Icons.Default.WaterDrop,
                        label = "This Month",
                        value = "${jarStats.thisMonthJarCount} jars",
                        color = Teal40
                    )
                    Spacer(Modifier.height(6.dp))
                    JarStatRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "Pending",
                        value = jarStats.pendingBalance.toRupee(),
                        color = if (jarStats.pendingBalance > 0) PaymentUnpaid else StatusCompleted
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (jarStats.pendingReturnJars > 0) {
                        JarStatRow(
                            icon = Icons.Default.SwapHoriz,
                            label = "Return",
                            value = "${jarStats.pendingReturnJars} jars pending",
                            color = StatusPending
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (jarStats.lastJarQuantity > 0) {
                        JarStatRow(
                            icon = Icons.Default.History,
                            label = "Last",
                            value = "${jarStats.lastJarQuantity} jars on ${DateUtils.formatShortDate(jarStats.lastJarDate)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "No jar entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Quick action buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // + Jar button (always visible)
                Button(
                    onClick = onAddJar,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Teal40,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Jar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                // Return button (only if pending returns > 0)
                if (jarStats.pendingReturnJars > 0) {
                    OutlinedButton(
                        onClick = onReturnJar,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.AssignmentReturn, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Return", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Pay button (only if pending balance > 0)
                if (jarStats.pendingBalance > 0) {
                    OutlinedButton(
                        onClick = onRecordPayment,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Payment, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pay", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Paid clear indicator
                if (jarStats.pendingBalance <= 0 && cs.totalOrders > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PaymentPaidBg,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = PaymentPaid, modifier = Modifier.size(14.dp))
                            Text("Paid clear", style = MaterialTheme.typography.labelMedium, color = PaymentPaid)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JarStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = color)
        Text(
            "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}
