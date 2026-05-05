package com.rajratna.events.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajratna.events.ui.theme.*
import com.rajratna.events.util.DateUtils
import com.rajratna.events.util.WhatsAppUtils
import com.rajratna.events.util.toRupee

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToNewOrder: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToItems: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToReturns: () -> Unit,
    onNavigateToOrderDetails: (Long) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rajratna Events",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToItems) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = "Items & Rates")
                    }
                    IconButton(onClick = onNavigateToBackup) {
                        Icon(Icons.Outlined.Backup, contentDescription = "Backup")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ═══════════════════════════════════════════
                // A. TODAY SUMMARY — Compact 3-column card
                // ═══════════════════════════════════════════
                item {
                    TodaySummaryCard(
                        todayIncome = state.todayIncome,
                        pendingPayment = state.todayPendingPayment,
                        orderCount = state.todayOrderCount
                    )
                }

                // ═══════════════════════════════════════════
                // B. ORDER STATUS — Compact 3-chip row
                // ═══════════════════════════════════════════
                item {
                    OrderStatusRow(
                        activeCount = state.activeOrderCount,
                        returnedToday = state.returnedTodayCount,
                        pendingReturn = state.pendingReturnCount
                    )
                }

                // ═══════════════════════════════════════════
                // C. ITEM-WISE STOCK OVERVIEW
                // ═══════════════════════════════════════════
                item {
                    DashboardSectionTitle(
                        title = "Stock Overview",
                        actionIcon = Icons.Outlined.Inventory2,
                        actionDescription = "Manage Items",
                        onActionClick = onNavigateToItems
                    )
                }

                items(state.itemStocks) { itemStock ->
                    ItemStockCard(itemStock)
                }

                // ═══════════════════════════════════════════
                // D. PENDING RETURNS PREVIEW
                // ═══════════════════════════════════════════
                item {
                    PendingReturnsSection(
                        items = state.pendingReturns,
                        onViewAll = onNavigateToReturns,
                        onNavigateToOrder = onNavigateToOrderDetails
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TODAY SUMMARY — Compact card with 3 metrics
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TodaySummaryCard(
    todayIncome: Double,
    pendingPayment: Double,
    orderCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Today's Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetric(
                    label = "Income",
                    value = todayIncome.toRupee(),
                    valueColor = StatusCompleted,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = "Pending",
                    value = pendingPayment.toRupee(),
                    valueColor = if (pendingPayment > 0) PaymentUnpaid else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = "Orders",
                    value = orderCount.toString(),
                    valueColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// ORDER STATUS ROW — 3 compact chips
// ═══════════════════════════════════════════════════════════════

@Composable
private fun OrderStatusRow(
    activeCount: Int,
    returnedToday: Int,
    pendingReturn: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusChipCompact(
            icon = Icons.Default.LocalShipping,
            count = activeCount,
            label = "Active",
            chipColor = TealContainer,
            textColor = Teal40,
            modifier = Modifier.weight(1f)
        )
        StatusChipCompact(
            icon = Icons.Default.CheckCircle,
            count = returnedToday,
            label = "Returned",
            chipColor = StatusCompletedBg,
            textColor = StatusCompleted,
            modifier = Modifier.weight(1f)
        )
        StatusChipCompact(
            icon = Icons.Default.AccessTime,
            count = pendingReturn,
            label = "Pending",
            chipColor = PaymentUnpaidBg,
            textColor = PaymentUnpaid,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusChipCompact(
    icon: ImageVector,
    count: Int,
    label: String,
    chipColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = chipColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SECTION TITLE
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DashboardSectionTitle(
    title: String,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (actionIcon != null && onActionClick != null) {
            IconButton(onClick = onActionClick) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ITEM-WISE STOCK CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ItemStockCard(item: ItemStockInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item name + low stock warning
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.isLowStock) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PaymentUnpaidBg
                        ) {
                            Text(
                                text = "LOW STOCK",
                                style = MaterialTheme.typography.labelSmall,
                                color = PaymentUnpaid,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Stock numbers
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StockNumber(label = "Total", value = item.totalStock, color = MaterialTheme.colorScheme.onSurface)
                StockNumber(label = "Avail", value = item.availableStock, color = StatusCompleted)
                StockNumber(label = "Out", value = item.rentedStock, color = if (item.rentedStock > 0) PaymentPartial else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StockNumber(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PENDING RETURNS SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PendingReturnsSection(
    items: List<PendingReturnPreview>,
    onViewAll: () -> Unit,
    onNavigateToOrder: (Long) -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pending Returns",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onViewAll) {
                    Text("View All", fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (items.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = StatusCompleted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "No pending returns",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items.forEachIndexed { index, item ->
                    PendingReturnRow(
                        item = item,
                        onCall = { WhatsAppUtils.callCustomer(context, item.customerMobile) },
                        onWhatsApp = {
                            // Generate a basic return reminder
                            val message = buildString {
                                appendLine("Hello ${item.customerName},")
                                appendLine()
                                appendLine("Your rented items for Bill No. ${item.billNumber} are pending return.")
                                appendLine()
                                appendLine("Pending items:")
                                item.pendingItems.forEach { pi ->
                                    appendLine("${pi.itemName}: ${pi.pendingQuantity}")
                                }
                                appendLine()
                                appendLine("Return Date: ${DateUtils.formatDate(item.returnDate)}")
                                appendLine()
                                appendLine("Please return the items as soon as possible.")
                            }
                            WhatsAppUtils.shareOnWhatsApp(context, item.customerMobile, message)
                        },
                        onOpenOrder = { onNavigateToOrder(item.orderId) }
                    )
                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingReturnRow(
    item: PendingReturnPreview,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onOpenOrder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Name + Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.customerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Bill #${item.billNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PendingReturnBadge(isOverdue = item.isOverdue, isDueToday = item.isDueToday)
        }

        // Row 2: Pending items
        Text(
            text = item.pendingItems.joinToString(", ") { "${it.itemName}: ${it.pendingQuantity}" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Row 3: Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(
                onClick = onCall,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = TealContainer
                )
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(16.dp), tint = Teal40)
            }
            FilledTonalIconButton(
                onClick = onWhatsApp,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = StatusCompletedBg
                )
            ) {
                Icon(Icons.Default.Chat, contentDescription = "WhatsApp", modifier = Modifier.size(16.dp), tint = StatusCompleted)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpenOrder, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text("Open Order", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PendingReturnBadge(isOverdue: Boolean, isDueToday: Boolean) {
    val (label, textColor, backgroundColor) = when {
        isOverdue -> Triple("OVERDUE", PaymentUnpaid, PaymentUnpaidBg)
        isDueToday -> Triple("DUE TODAY", StatusConfirmed, StatusConfirmedBg)
        else -> Triple("UPCOMING", StatusPending, StatusPendingBg)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = backgroundColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}
