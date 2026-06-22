package com.rajratna.events.ui.screens.dashboard

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.rajratna.events.ui.theme.PaymentPartial
import com.rajratna.events.ui.theme.PaymentUnpaid
import com.rajratna.events.ui.theme.StatusCompleted
import com.rajratna.events.util.DateUtils
import com.rajratna.events.util.toRupee
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToNewOrder: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToItems: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToReturns: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToOrderDetails: (Long) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    Scaffold(
        containerColor = Color(0xFFF6F7FB),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rajratna Events",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
//                    IconButton(onClick = onNavigateToItems) {
//                        Icon(Icons.Outlined.Inventory2, contentDescription = "Inventory")
//                    }
                    IconButton(onClick = onNavigateToBackup) {
                        Icon(Icons.Outlined.Backup, contentDescription = "Backup")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF6F7FB))
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewOrder,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OverviewCard(
                        date = state.selectedOverviewDate,
                        incomeToday = state.todayIncome,
                        pendingAmount = state.todayPendingPayment,
                        deliveries = state.activeOrderCount,
                        returns = state.returnedTodayCount,
                        onPickDate = {
                            val cal = Calendar.getInstance().apply { timeInMillis = state.selectedOverviewDate }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selected = Calendar.getInstance().apply {
                                        set(year, month, day, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    viewModel.selectOverviewDate(selected.timeInMillis)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )
                }

//                item {
//                    SectionTitle("Quick Actions")
//                }
//
//                item {
//                    QuickActionsRow(
//                        onNavigateToNewOrder = onNavigateToNewOrder,
//                        onNavigateToReturns = onNavigateToReturns,
//                        onNavigateToPayments = onNavigateToPayments,
//                        onNavigateToReports = onNavigateToReports
//                    )
//                }

                item {
                    SectionTitle("Important Alerts")
                }

                item {
                    AlertsCard(
                        alerts = state.alerts,
                        onClick = { alertType ->
                            when (alertType) {
                                DashboardAlertType.OVERDUE_RETURNS -> onNavigateToReturns()
                                DashboardAlertType.PENDING_PAYMENTS -> onNavigateToOrders()
                                DashboardAlertType.LOW_STOCK -> onNavigateToItems()
                                DashboardAlertType.TOMORROW_BOOKINGS -> onNavigateToOrders()
                            }
                        }
                    )
                }

                item {
                    StockOverviewHeader(
                        date = state.selectedStockDate,
                        onPickDate = {
                            val cal = Calendar.getInstance().apply { timeInMillis = state.selectedStockDate }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selected = Calendar.getInstance().apply {
                                        set(year, month, day, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    viewModel.selectStockDate(selected.timeInMillis)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        onOpenItems = onNavigateToItems
                    )
                }

                item {
                    StockOverviewTable(state.itemStocks)
                }

                item {
                    UpcomingDeliveriesSection(
                        deliveries = state.upcomingDeliveries,
                        onViewAll = onNavigateToOrders,
                        onOpenOrder = onNavigateToOrderDetails
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    date: Long,
    incomeToday: Double,
    pendingAmount: Double,
    deliveries: Int,
    returns: Int,
    onPickDate: () -> Unit
) {
    val dateLabel = if (date == DateUtils.startOfToday()) "Today" else DateUtils.formatShortDate(date)
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Today's Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                AssistChip(
                    onClick = onPickDate,
                    label = { Text(dateLabel) },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Income Today", incomeToday.toRupee(), StatusCompleted, Icons.Default.Paid, Modifier.weight(1f))
                MetricTile("Pending Amount", pendingAmount.toRupee(), PaymentUnpaid, Icons.Default.Payment, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Deliveries", deliveries.toString(), MaterialTheme.colorScheme.primary, Icons.Default.LocalShipping, Modifier.weight(1f))
                MetricTile("Returns", returns.toString(), Color(0xFF2E9A4B), Icons.Default.Replay, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, valueColor: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFFF9FAFC),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = valueColor.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

//@Composable
//private fun QuickActionsRow(
//    onNavigateToNewOrder: () -> Unit,
//    onNavigateToReturns: () -> Unit,
//    onNavigateToPayments: () -> Unit,
//    onNavigateToReports: () -> Unit
//) {
//    val actions = listOf(
//        QuickActionUi("New Order", "Create order", Icons.Default.Add, Color(0xFFEFF4FF), Color(0xFF1858D2), onNavigateToNewOrder),
//        QuickActionUi("Record Return", "Add return", Icons.Default.Replay, Color(0xFFEFFAF3), Color(0xFF2F9B50), onNavigateToReturns),
//        QuickActionUi("Add Payment", "Record payment", Icons.Default.Payment, Color(0xFFFFF6EA), Color(0xFFDB7A00), onNavigateToPayments),
//        QuickActionUi("Reports", "View reports", Icons.Default.Assessment, Color(0xFFF7F1FF), Color(0xFF7A3CC8), onNavigateToReports)
//    )
//
//    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
//        actions.forEach { action ->
//            Card(
//                modifier = Modifier
//                    .weight(1f)
//                    .clickable(onClick = action.onClick),
//                shape = RoundedCornerShape(14.dp),
//                colors = CardDefaults.cardColors(containerColor = action.bgColor),
//                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 12.dp, horizontal = 10.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.spacedBy(6.dp)
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(30.dp)
//                            .background(action.iconTint.copy(alpha = 0.16f), CircleShape),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Icon(action.icon, contentDescription = null, tint = action.iconTint, modifier = Modifier.size(18.dp))
//                    }
//                    Text(action.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = action.iconTint, maxLines = 1)
//                    Text(action.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
//                }
//            }
//        }
//    }
//}

@Composable
private fun AlertsCard(alerts: List<DashboardAlertInfo>, onClick: (DashboardAlertType) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column {
            alerts.forEachIndexed { index, alert ->
                AlertRow(alert = alert, onClick = { onClick(alert.type) })
                if (index != alerts.lastIndex) HorizontalDivider(color = Color(0xFFE8EAF1))
            }
        }
    }
}

@Composable
private fun AlertRow(alert: DashboardAlertInfo, onClick: () -> Unit) {
    val iconData = when (alert.type) {
        DashboardAlertType.OVERDUE_RETURNS -> Triple(Icons.Default.WarningAmber, Color(0xFFFFEFEF), Color(0xFFD63939))
        DashboardAlertType.PENDING_PAYMENTS -> Triple(Icons.Default.Payment, Color(0xFFFFF6EA), Color(0xFFDB7A00))
        DashboardAlertType.LOW_STOCK -> Triple(Icons.Default.Inventory2, Color(0xFFEFF4FF), Color(0xFF1E5CC8))
        DashboardAlertType.TOMORROW_BOOKINGS -> Triple(Icons.Default.Schedule, Color(0xFFEFFAF3), Color(0xFF2F9B50))
    }
    val title = when (alert.type) {
        DashboardAlertType.OVERDUE_RETURNS -> "Overdue Returns"
        DashboardAlertType.PENDING_PAYMENTS -> "Pending Payments"
        DashboardAlertType.LOW_STOCK -> "Low Stock Alert"
        DashboardAlertType.TOMORROW_BOOKINGS -> "Tomorrow's Bookings"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(iconData.second, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconData.first, contentDescription = null, tint = iconData.third, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(alert.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(alert.count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = iconData.third)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun StockOverviewHeader(date: Long, onPickDate: () -> Unit, onOpenItems: () -> Unit) {
    val label = if (date == DateUtils.startOfToday()) "Today" else DateUtils.formatShortDate(date)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Stock Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = onPickDate,
                label = { Text(label) },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            IconButton(onClick = onOpenItems) {
                Icon(Icons.Default.Inventory2, contentDescription = "Inventory")
            }
        }
    }
}

@Composable
private fun StockOverviewTable(items: List<ItemStockInfo>) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text("Item", modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Total", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Booked", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelMedium, color = PaymentPartial)
                Text("Available", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.labelMedium, color = StatusCompleted)
            }
            HorizontalDivider(color = Color(0xFFE8EAF1))
            items.forEachIndexed { index, item ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.totalStock.toString(), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium)
                    Text(item.outStock.toString(), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium, color = if (item.outStock > 0) PaymentPartial else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.availableStock.toString(), modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.bodyMedium, color = if (item.isLowStock) Color(0xFFD63939) else StatusCompleted, fontWeight = FontWeight.SemiBold)
                }
                if (index != items.lastIndex) HorizontalDivider(color = Color(0xFFF1F2F7))
            }
        }
    }
}

@Composable
private fun UpcomingDeliveriesSection(
    deliveries: List<UpcomingDeliveryInfo>,
    onViewAll: () -> Unit,
    onOpenOrder: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Upcoming Deliveries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = "View All",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onViewAll)
            )
        }

        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            if (deliveries.isEmpty()) {
                Text(
                    text = "No upcoming deliveries",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column {
                    deliveries.forEachIndexed { index, delivery ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenOrder(delivery.orderId) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(DateUtils.formatDate(delivery.deliveryDate), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(delivery.customerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(delivery.itemSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Surface(color = Color(0xFFEFF4FF), shape = RoundedCornerShape(50)) {
                                Text("Delivery", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (index != deliveries.lastIndex) HorizontalDivider(color = Color(0xFFE8EAF1))
                    }
                }
            }
        }
    }
}

private data class QuickActionUi(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val bgColor: Color,
    val iconTint: Color,
    val onClick: () -> Unit
)
