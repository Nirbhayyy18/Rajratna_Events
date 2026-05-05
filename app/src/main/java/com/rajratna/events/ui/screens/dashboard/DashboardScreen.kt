package com.rajratna.events.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajratna.events.ui.components.SectionHeader
import com.rajratna.events.ui.components.StatCard
import com.rajratna.events.ui.theme.*
import com.rajratna.events.util.toRupee

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToNewOrder: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToItems: () -> Unit,
    onNavigateToBackup: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Refresh dashboard when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Rajratna Events",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Business Dashboard",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToItems) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = "Items & Rates")
                    }
                    IconButton(onClick = onNavigateToBackup) {
                        Icon(Icons.Outlined.Backup, contentDescription = "Backup")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewOrder,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New Order", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Today's Summary ─────────────────────────
                item {
                    SectionHeader(title = "📊 Today's Summary")
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            StatCard(
                                title = "Order Amount",
                                value = state.todayOrderAmount.toRupee(),
                                icon = Icons.Default.Receipt,
                                iconTint = Orange40,
                                modifier = Modifier.width(160.dp)
                            )
                        }
                        item {
                            StatCard(
                                title = "Received",
                                value = state.todayPaymentReceived.toRupee(),
                                icon = Icons.Default.AccountBalanceWallet,
                                iconTint = StatusCompleted,
                                modifier = Modifier.width(160.dp)
                            )
                        }
                        item {
                            StatCard(
                                title = "Pending",
                                value = state.todayPendingBalance.toRupee(),
                                icon = Icons.Default.Warning,
                                iconTint = PaymentUnpaid,
                                modifier = Modifier.width(160.dp)
                            )
                        }
                        item {
                            StatCard(
                                title = "Transport",
                                value = state.todayTransportRent.toRupee(),
                                icon = Icons.Default.LocalShipping,
                                iconTint = Teal40,
                                modifier = Modifier.width(160.dp)
                            )
                        }
                    }
                }

                // ── Weekly & Monthly ────────────────────────
                item {
                    SectionHeader(title = "📈 Weekly & Monthly")
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "This Week",
                            orderAmount = state.weekOrderAmount,
                            received = state.weekPaymentReceived,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "This Month",
                            orderAmount = state.monthOrderAmount,
                            received = state.monthPaymentReceived,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Order Status (Today) ────────────────────
                item {
                    SectionHeader(title = "📋 Today's Orders (${state.todayOrderCount})")
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            StatusCountChip("Pending", state.pendingCount, StatusPending, StatusPendingBg)
                        }
                        item {
                            StatusCountChip("Confirmed", state.confirmedCount, StatusConfirmed, StatusConfirmedBg)
                        }
                        item {
                            StatusCountChip("Delivered", state.deliveredCount, StatusDelivered, StatusDeliveredBg)
                        }
                        item {
                            StatusCountChip("Completed", state.completedCount, StatusCompleted, StatusCompletedBg)
                        }
                        item {
                            StatusCountChip("Cancelled", state.cancelledCount, StatusCancelled, StatusCancelledBg)
                        }
                    }
                }

                // ── Quick Actions ───────────────────────────
                item {
                    SectionHeader(title = "⚡ Quick Actions")
                }

                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickNavButton(
                                text = "New Order",
                                icon = Icons.Default.AddCircle,
                                onClick = onNavigateToNewOrder,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            QuickNavButton(
                                text = "All Orders",
                                icon = Icons.Default.ListAlt,
                                onClick = onNavigateToOrders,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickNavButton(
                                text = "Customers",
                                icon = Icons.Default.People,
                                onClick = onNavigateToCustomers,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            QuickNavButton(
                                text = "Payments",
                                icon = Icons.Default.Payments,
                                onClick = onNavigateToPayments,
                                containerColor = PaymentPaidBg,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickNavButton(
                                text = "Reports",
                                icon = Icons.Default.BarChart,
                                onClick = onNavigateToReports,
                                containerColor = StatusConfirmedBg,
                                modifier = Modifier.weight(1f)
                            )
                            QuickNavButton(
                                text = "Items & Rates",
                                icon = Icons.Default.Inventory2,
                                onClick = onNavigateToItems,
                                containerColor = StatusDeliveredBg,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// LOCAL COMPOSABLES
// ═══════════════════════════════════════════════════════════

@Composable
private fun SummaryCard(
    title: String,
    orderAmount: Double,
    received: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = orderAmount.toRupee(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Received: ${received.toRupee()}",
                style = MaterialTheme.typography.bodySmall,
                color = StatusCompleted
            )
        }
    }
}

@Composable
private fun StatusCountChip(
    label: String,
    count: Int,
    textColor: Color,
    bgColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
        }
    }
}

@Composable
private fun QuickNavButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
