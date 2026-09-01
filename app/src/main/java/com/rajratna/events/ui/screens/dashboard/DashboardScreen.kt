package com.rajratna.events.ui.screens.dashboard

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajratna.events.ui.components.IconPod
import com.rajratna.events.ui.components.ThemeToggleButton
import com.rajratna.events.ui.theme.*
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
    onNavigateToOrderDetails: (String) -> Unit,
    currentTheme: ThemeMode = ThemeMode.SYSTEM,
    onCycleTheme: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Entry animation trigger
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
        visible = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Rajratna Events",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = DateUtils.formatShortDate(System.currentTimeMillis()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    ThemeToggleButton(
                        currentMode  = currentTheme,
                        onCycleTheme = onCycleTheme
                    )
                    IconButton(onClick = onNavigateToBackup) {
                        Icon(Icons.Outlined.Backup, contentDescription = "Backup")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 8 }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Hero Financial Card ───────────────
                    item {
                        HeroFinancialCard(
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

                    // ── Quick Actions Row ─────────────────
                    item {
                        QuickActionsRow(
                            onNavigateToNewOrder  = onNavigateToNewOrder,
                            onNavigateToReturns   = onNavigateToReturns,
                            onNavigateToPayments  = onNavigateToPayments,
                            onNavigateToReports   = onNavigateToReports
                        )
                    }

                    // ── Alerts section ───────────────────
                    item { SectionTitle("Important Alerts") }

                    item {
                        AlertsCard(
                            alerts = state.alerts,
                            onClick = { alertType ->
                                when (alertType) {
                                    DashboardAlertType.OVERDUE_RETURNS   -> onNavigateToReturns()
                                    DashboardAlertType.PENDING_PAYMENTS  -> onNavigateToOrders()
                                    DashboardAlertType.LOW_STOCK         -> onNavigateToItems()
                                    DashboardAlertType.TOMORROW_BOOKINGS -> onNavigateToOrders()
                                }
                            }
                        )
                    }

                    // ── Stock Overview ───────────────────
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

                    item { StockOverviewTable(state.itemStocks) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// HERO FINANCIAL CARD — Gradient + metric tiles
// ═══════════════════════════════════════════════════════════

@Composable
private fun HeroFinancialCard(
    date: Long,
    incomeToday: Double,
    pendingAmount: Double,
    deliveries: Int,
    returns: Int,
    onPickDate: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.3f

    val gradientColors = if (isDark) {
        listOf(EmeraldContainerDark, Color(0xFF003830))
    } else {
        listOf(Emerald40, Emerald20)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(gradientColors))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Today's Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.clickable(onClick = onPickDate)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Text(
                            DateUtils.formatShortDate(date),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Hero metric — Income
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = incomeToday.toRupee(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Income Today",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            // Secondary metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroMetricChip(
                    label = "Pending",
                    value = pendingAmount.toRupee(),
                    chipColor = StatusPendingBg,
                    textColor = StatusPending,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricChip(
                    label = "Active Orders",
                    value = "$deliveries",
                    chipColor = StatusConfirmedBg,
                    textColor = StatusConfirmed,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricChip(
                    label = "Returns",
                    value = "$returns",
                    chipColor = StatusCompletedBg,
                    textColor = StatusCompleted,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroMetricChip(
    label: String,
    value: String,
    chipColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = chipColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.8f), maxLines = 1)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// QUICK ACTIONS ROW — Premium icon pods
// ═══════════════════════════════════════════════════════════

private data class QuickActionUi(
    val title: String,
    val icon: ImageVector,
    val bgColor: Color,
    val iconColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionsRow(
    onNavigateToNewOrder: () -> Unit,
    onNavigateToReturns: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val actions = listOf(
        QuickActionUi("New Order",  Icons.Default.Add,        StatusConfirmedBg, StatusConfirmed,  onNavigateToNewOrder),
        QuickActionUi("Returns",    Icons.Default.Replay,     StatusCompletedBg, StatusCompleted,  onNavigateToReturns),
        QuickActionUi("Payment",    Icons.Default.Payment,    StatusPendingBg,   StatusPending,    onNavigateToPayments),
        QuickActionUi("Reports",    Icons.Default.Assessment, Color(0xFFF3F0FF), Color(0xFF5B21B6), onNavigateToReports)
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.forEach { action ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = action.onClick),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = action.bgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconPod(
                        icon = action.icon,
                        containerColor = action.iconColor.copy(alpha = 0.15f),
                        iconColor = action.iconColor,
                        size = 38.dp,
                        iconSize = 18.dp,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text(
                        action.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = action.iconColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// SECTION TITLE
// ═══════════════════════════════════════════════════════════

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

// ═══════════════════════════════════════════════════════════
// ALERTS CARD
// ═══════════════════════════════════════════════════════════

@Composable
private fun AlertsCard(alerts: List<DashboardAlertInfo>, onClick: (DashboardAlertType) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            alerts.forEachIndexed { index, alert ->
                AlertRow(alert = alert, onClick = { onClick(alert.type) })
                if (index != alerts.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun AlertRow(alert: DashboardAlertInfo, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background.red < 0.3f
    val iconData = when (alert.type) {
        DashboardAlertType.OVERDUE_RETURNS   -> Triple(
            Icons.Default.WarningAmber,
            if (isDark) AlertOverdueIconBgDark  else AlertOverdueIconBg,
            if (isDark) StatusCancelledDark      else AlertOverdueIconColor
        )
        DashboardAlertType.PENDING_PAYMENTS  -> Triple(
            Icons.Default.Payment,
            if (isDark) AlertPaymentIconBgDark  else AlertPaymentIconBg,
            if (isDark) StatusPendingDark        else AlertPaymentIconColor
        )
        DashboardAlertType.LOW_STOCK         -> Triple(
            Icons.Default.Inventory2,
            if (isDark) AlertStockIconBgDark    else AlertStockIconBg,
            if (isDark) StatusConfirmedDark      else AlertStockIconColor
        )
        DashboardAlertType.TOMORROW_BOOKINGS -> Triple(
            Icons.Default.Schedule,
            if (isDark) AlertBookingIconBgDark  else AlertBookingIconBg,
            if (isDark) StatusCompletedDark      else AlertBookingIconColor
        )
    }
    val title = when (alert.type) {
        DashboardAlertType.OVERDUE_RETURNS   -> "Overdue Returns"
        DashboardAlertType.PENDING_PAYMENTS  -> "Pending Payments"
        DashboardAlertType.LOW_STOCK         -> "Low Stock Alert"
        DashboardAlertType.TOMORROW_BOOKINGS -> "Tomorrow's Bookings"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconPod(
            icon = iconData.first,
            containerColor = iconData.second,
            iconColor = iconData.third,
            size = 40.dp,
            iconSize = 20.dp,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                alert.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            alert.count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = iconData.third
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
    }
}

// ═══════════════════════════════════════════════════════════
// STOCK OVERVIEW
// ═══════════════════════════════════════════════════════════

@Composable
private fun StockOverviewHeader(date: Long, onPickDate: () -> Unit, onOpenItems: () -> Unit) {
    val label = if (date == DateUtils.startOfToday()) "Today" else DateUtils.formatShortDate(date)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Stock Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text("Item",      modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Total",     modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Out",       modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = StatusPending)
                Text("Available", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = StatusCompleted)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            items.forEachIndexed { index, item ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.totalStock.toString(), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        item.outStock.toString(),
                        modifier = Modifier.weight(0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (item.outStock > 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (item.outStock > 0) StatusPending else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        item.availableStock.toString(),
                        modifier = Modifier.weight(0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isLowStock) StatusCancelled else StatusCompleted
                    )
                }
                if (index != items.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}
