package com.rajratna.events.ui.screens.reports

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajratna.events.ui.components.AmountRow
import com.rajratna.events.ui.theme.*
import com.rajratna.events.util.DateUtils
import com.rajratna.events.util.toRupee
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadReports() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Income Reports", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading && state.report.totalOrders == 0) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ═══════════════════════════════════════════
                // 1. Period Filter Tabs
                // ═══════════════════════════════════════════
                item {
                    PeriodFilterTabs(
                        selectedPeriod = state.selectedPeriod,
                        onSelectPeriod = { viewModel.selectPeriod(it) }
                    )
                }

                // ═══════════════════════════════════════════
                // 2. Period-Specific Controls
                // ═══════════════════════════════════════════
                item {
                    when (state.selectedPeriod) {
                        "Today" -> {
                            Text(
                                text = state.dateRangeLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        "Week" -> {
                            Text(
                                text = state.dateRangeLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        "Month" -> {
                            MonthSelector(
                                monthLabel = viewModel.getMonthLabel(),
                                onPrevious = { viewModel.navigateMonth(-1) },
                                onNext = { viewModel.navigateMonth(1) },
                                onCurrentMonth = { viewModel.selectCurrentMonth() },
                                onPreviousMonth = { viewModel.selectPreviousMonth() }
                            )
                        }
                        "Range" -> {
                            RangeSelector(
                                customStart = state.customStart,
                                customEnd = state.customEnd?.let { it - 1 }, // minus the +1 offset for display
                                rangeError = state.rangeError,
                                onSelectStart = { viewModel.selectRangeStart(it) },
                                onSelectEnd = { viewModel.selectRangeEnd(it) },
                                onQuickRange = { viewModel.applyQuickRange(it) }
                            )
                        }
                    }
                }

                // ═══════════════════════════════════════════
                // 3. Report Summary Card
                // ═══════════════════════════════════════════
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Report Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "${state.report.totalOrders} orders",
                                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            AmountRow("Total Income", state.report.totalIncome, isBold = true)
                            AmountRow("Received Amount", state.report.receivedAmount, color = StatusCompleted)
                            AmountRow(
                                "Pending Amount",
                                state.report.pendingAmount,
                                isBold = true,
                                color = if (state.report.pendingAmount > 0) PaymentUnpaid else StatusCompleted
                            )
                        }
                    }
                }

                // ═══════════════════════════════════════════
                // 4. Item Breakdown Card
                // ═══════════════════════════════════════════
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Item Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            if (state.report.itemWiseIncome.isEmpty() && state.report.transportRent == 0.0) {
                                Text(
                                    "No item income in this period",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                state.report.itemWiseIncome.forEach { item ->
                                    AmountRow("${item.itemName} Income", item.totalIncome)
                                }
                                if (state.report.transportRent > 0) {
                                    AmountRow("Transport Rent", state.report.transportRent)
                                }
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════
                // 5. Share / Export Buttons
                // ═══════════════════════════════════════════
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val message = viewModel.generateWhatsAppReport()
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, message)
                                    setPackage("com.whatsapp")
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallback = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, message)
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(android.content.Intent.createChooser(fallback, "Share Report"))
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("WhatsApp", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { viewModel.generateAndSharePdf(context) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export PDF", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PERIOD FILTER TABS — Today | Week | Month | Range
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PeriodFilterTabs(
    selectedPeriod: String,
    onSelectPeriod: (String) -> Unit
) {
    val periods = listOf("Today", "Week", "Month", "Range")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onSelectPeriod(period) },
                label = {
                    Text(
                        period,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MONTH SELECTOR — < May 2026 > with quick buttons
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MonthSelector(
    monthLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrentMonth: () -> Unit,
    onPreviousMonth: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month navigation row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quick buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onCurrentMonth,
                    label = { Text("Current Month", style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = onPreviousMonth,
                    label = { Text("Previous Month", style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// RANGE SELECTOR — From / To dates with quick range chips
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RangeSelector(
    customStart: Long?,
    customEnd: Long?,
    rangeError: String?,
    onSelectStart: (Long) -> Unit,
    onSelectEnd: (Long) -> Unit,
    onQuickRange: (String) -> Unit
) {
    val context = LocalContext.current
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date fields row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // From Date
                OutlinedCard(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "From Date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (customStart != null) DateUtils.formatShortDate(customStart)
                            else "Select",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // To Date
                OutlinedCard(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "To Date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (customEnd != null) DateUtils.formatShortDate(customEnd)
                            else "Select",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Range Error
            if (rangeError != null) {
                Text(
                    text = rangeError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            // Quick range chips
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Last 7 Days", "Last 30 Days", "This Month").forEach { label ->
                    AssistChip(
                        onClick = { onQuickRange(label) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Date Pickers
    if (showStartPicker) {
        val cal = Calendar.getInstance()
        if (customStart != null) cal.timeInMillis = customStart
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val c = Calendar.getInstance()
                c.set(year, month, day, 0, 0, 0)
                c.set(Calendar.MILLISECOND, 0)
                onSelectStart(c.timeInMillis)
                showStartPicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Select From Date")
            setOnCancelListener { showStartPicker = false }
        }.show()
    }

    if (showEndPicker) {
        val cal = Calendar.getInstance()
        if (customEnd != null) cal.timeInMillis = customEnd
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val c = Calendar.getInstance()
                c.set(year, month, day, 23, 59, 59)
                c.set(Calendar.MILLISECOND, 999)
                onSelectEnd(c.timeInMillis)
                showEndPicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Select To Date")
            setOnCancelListener { showEndPicker = false }
        }.show()
    }
}
