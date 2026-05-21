package com.rajratna.events.ui.screens.dashboard

import android.app.DatePickerDialog
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
import java.util.*

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
                // C. ITEM-WISE STOCK OVERVIEW (Date-Aware)
                // ═══════════════════════════════════════════
                item {
                    val context = LocalContext.current
                    val isToday = state.selectedStockDate == DateUtils.startOfToday()
                    val dateLabel = if (isToday) "Today" else DateUtils.formatShortDate(state.selectedStockDate)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Stock Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = {
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = state.selectedStockDate
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val selected = Calendar.getInstance()
                                            selected.set(year, month, day, 0, 0, 0)
                                            selected.set(Calendar.MILLISECOND, 0)
                                            viewModel.selectStockDate(selected.timeInMillis)
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                label = {
                                    Text(dateLabel, style = MaterialTheme.typography.labelMedium)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = "Select Date",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            if (!isToday) {
                                FilledTonalIconButton(
                                    onClick = { viewModel.selectStockDate(DateUtils.startOfToday()) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Today,
                                        contentDescription = "Back to Today",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(onClick = onNavigateToItems) {
                                Icon(
                                    imageVector = Icons.Outlined.Inventory2,
                                    contentDescription = "Manage Items",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(state.itemStocks) { itemStock ->
                    ItemStockCard(
                        item = itemStock,
                        selectedDate = state.selectedStockDate
                    )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Today's Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryMetric(
                    label = "Income",
                    value = todayIncome.toRupee(),
                    valueColor = StatusCompleted,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(40.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SummaryMetric(
                    label = "Pending",
                    value = pendingPayment.toRupee(),
                    valueColor = if (pendingPayment > 0) PaymentUnpaid else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(40.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusChipCompact(
            icon = Icons.Default.LocalShipping,
            count = activeCount,
            label = "Active",
            chipColor = MaterialTheme.colorScheme.surface,
            textColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatusChipCompact(
            icon = Icons.Default.CheckCircle,
            count = returnedToday,
            label = "Returned",
            chipColor = MaterialTheme.colorScheme.surface,
            textColor = StatusCompleted,
            modifier = Modifier.weight(1f)
        )
        StatusChipCompact(
            icon = Icons.Default.AccessTime,
            count = pendingReturn,
            label = "Pending",
            chipColor = MaterialTheme.colorScheme.surface,
            textColor = if (pendingReturn > 0) PaymentUnpaid else MaterialTheme.colorScheme.onSurfaceVariant,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = chipColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(28.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
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
private fun ItemStockCard(item: ItemStockInfo, selectedDate: Long) {
    val isToday = selectedDate == DateUtils.startOfToday()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                // Stock numbers: Total → Out/Booked → Available/Expected Available
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StockNumber(label = "Total", value = item.totalStock, color = MaterialTheme.colorScheme.onSurface)
                    if (isToday) {
                        StockNumber(
                            label = "Out Today",
                            value = item.outStock,
                            color = if (item.outStock > 0) PaymentPartial else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StockNumber(
                            label = "Available",
                            value = item.availableStock,
                            color = StatusCompleted
                        )
                    } else {
                        StockNumber(
                            label = "Booked",
                            value = item.outStock,
                            color = if (item.outStock > 0) PaymentPartial else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StockNumber(
                            label = "Expected Avail",
                            value = item.availableStock,
                            color = StatusCompleted
                        )
                    }
                }
            }

            // Risk line (only if selected date is future and risk exists)
            if (!isToday && item.riskStock > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Risk warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Risk: ${item.riskStock} pending returns",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = PaymentUnpaidBg
                    ) {
                        Text(
                            text = "RISK",
                            style = MaterialTheme.typography.labelSmall,
                            color = PaymentUnpaid,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
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
