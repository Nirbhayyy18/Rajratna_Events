package com.rajratna.events.ui.screens.neworder

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajratna.events.data.entity.OrderStatus
import com.rajratna.events.ui.components.AmountRow
import com.rajratna.events.ui.theme.*
import com.rajratna.events.util.DateUtils
import com.rajratna.events.util.toRupee
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOrderScreen(
    onNavigateBack: () -> Unit,
    onOrderSaved: (Long) -> Unit,
    editOrderId: Long? = null,
    viewModel: NewOrderViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var lastActionStatus by remember { mutableStateOf<String?>(null) }

    // Load order for editing
    LaunchedEffect(editOrderId) {
        if (editOrderId != null && editOrderId > 0) {
            viewModel.loadOrder(editOrderId)
        }
    }

    // Navigate on successful save
    LaunchedEffect(state.savedOrderId) {
        state.savedOrderId?.let { orderId ->
            val message = if (state.isEditMode) {
                "Order updated successfully"
            } else if (lastActionStatus == OrderStatus.PENDING) {
                "Order Saved as Pending"
            } else {
                "Order Confirmed successfully"
            }
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            onOrderSaved(orderId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) "Edit Order" else "New Order",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Error Message ───────────────────────────
                if (state.errorMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = state.errorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // ── Customer Details ────────────────────────
                item {
                    SectionCard(title = "👤 Customer Details") {
                        OutlinedTextField(
                            value = state.customerName,
                            onValueChange = { viewModel.updateCustomerName(it) },
                            label = { Text("Customer Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.mobileNumber,
                            onValueChange = { viewModel.updateMobileNumber(it) },
                            label = { Text("Mobile Number *") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.address,
                            onValueChange = { viewModel.updateAddress(it) },
                            label = { Text("Delivery Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // ── Dates ───────────────────────────────────
                item {
                    SectionCard(title = "📅 Order Dates") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DateField(
                                label = "Delivery Date",
                                date = state.deliveryDate,
                                onDateSelected = { viewModel.updateDeliveryDate(it) },
                                modifier = Modifier.weight(1f)
                            )
                            DateField(
                                label = "Return Date",
                                date = state.returnDate,
                                onDateSelected = { viewModel.updateReturnDate(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Rental Days: ${state.rentalDays}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = { viewModel.updateNotes(it) },
                            label = { Text("Notes (optional)") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // ── Items Selection ─────────────────────────
                item {
                    SectionCard(title = "📦 Items") {
                        if (state.isCheckingStock) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Checking stock...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        state.itemEntries.forEach { entry ->
                            val stockDetails = state.rangeStockDetails[entry.item.id]
                            val available = stockDetails?.availableQty ?: entry.item.totalStock
                            val risk = stockDetails?.riskQty ?: 0

                            ItemQuantityRow(
                                name = entry.item.name,
                                rate = entry.item.ratePerDay,
                                quantity = entry.quantity,
                                rentalDays = state.rentalDays,
                                deliveryDate = state.deliveryDate,
                                isCustomerOwned = entry.isCustomerOwned,
                                availableForDates = available,
                                riskStock = risk,
                                stockError = state.stockErrors[entry.item.id],
                                onQuantityChange = { viewModel.updateItemQuantity(entry.item.id, it) },
                                onCustomerOwnedChange = { viewModel.updateItemCustomerOwned(entry.item.id, it) }
                            )
                            if (entry != state.itemEntries.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // ── Bill Summary ────────────────────────────
                item {
                    SectionCard(title = "💰 Bill Summary") {
                        AmountRow(label = "Items Total", amount = state.itemsTotal)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.transportRent,
                            onValueChange = { viewModel.updateTransportRent(it) },
                            label = { Text("Transport Rent (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        AmountRow(
                            label = "Grand Total",
                            amount = state.grandTotal,
                            isBold = true,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.advancePaid,
                            onValueChange = { viewModel.updateAdvancePaid(it) },
                            label = { Text("Advance Paid (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        AmountRow(
                            label = "Balance",
                            amount = state.balanceAmount,
                            isBold = true,
                            color = if (state.balanceAmount > 0) PaymentUnpaid else StatusCompleted
                        )
                    }
                }

                // ── Action Buttons ──────────────────────────
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                lastActionStatus = OrderStatus.CONFIRMED
                                viewModel.saveOrder(OrderStatus.CONFIRMED)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !state.isSaving && state.isStockValid
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (state.isEditMode) "Update Order" else "Confirm Order",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (!state.isEditMode) {
                            OutlinedButton(
                                onClick = {
                                    lastActionStatus = OrderStatus.PENDING
                                    viewModel.saveOrder(OrderStatus.PENDING)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !state.isSaving && state.isStockValid
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Save as Pending", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (!state.isStockValid) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Stock not available for selected dates. Reduce quantity or change dates.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// LOCAL COMPOSABLES
// ═══════════════════════════════════════════════════════════

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemQuantityRow(
    name: String,
    rate: Double,
    quantity: Int,
    rentalDays: Int,
    deliveryDate: Long,
    isCustomerOwned: Boolean = false,
    availableForDates: Int? = null,
    riskStock: Int = 0,
    stockError: String? = null,
    onQuantityChange: (Int) -> Unit,
    onCustomerOwnedChange: (Boolean) -> Unit
) {
    var quantityText by remember { mutableStateOf(quantity.toString()) }

    LaunchedEffect(quantity) {
        val displayedQuantity = quantityText.toIntOrNull()
        when {
            quantityText.isEmpty() && quantity != 0 -> quantityText = quantity.toString()
            quantityText.isNotEmpty() && displayedQuantity != quantity -> quantityText = quantity.toString()
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹${rate.toInt()} / day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isCustomerOwned && availableForDates != null) {
                    Text(
                        text = "Available for selected dates: $availableForDates",
                        style = MaterialTheme.typography.bodySmall,
                        color = Teal40,
                        fontWeight = FontWeight.Medium
                    )
                    if (riskStock > 0) {
                        Text(
                            text = "Risk: $riskStock pending return before this date",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (quantity > 0) {
                    Text(
                        text = "$quantity × ${rate.toInt()} × $rentalDays = ${(quantity * rate * rentalDays).toRupee()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledIconButton(
                    onClick = { onQuantityChange(quantity - 1) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            val normalizedInput = input.trimStart('0').ifEmpty {
                                if (input.isEmpty()) "" else "0"
                            }
                            val parsedQuantity = normalizedInput
                                .toLongOrNull()
                                ?.coerceAtMost(Int.MAX_VALUE.toLong())
                                ?.toInt()
                                ?: 0

                            quantityText = normalizedInput
                            onQuantityChange(parsedQuantity)
                        }
                    },
                    modifier = Modifier.width(72.dp),
                    singleLine = true,
                    isError = stockError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    placeholder = {
                        Text(
                            text = "0",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    },
                    shape = RoundedCornerShape(10.dp)
                )

                FilledIconButton(
                    onClick = { onQuantityChange(quantity + 1) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                }
            }
        }

        if (name.contains("Water Jar", ignoreCase = true) && quantity > 0) {
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Source:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                FilterChip(
                    selected = !isCustomerOwned,
                    onClick = { onCustomerOwnedChange(false) },
                    label = { Text("Our Jar") }
                )
                FilterChip(
                    selected = isCustomerOwned,
                    onClick = { onCustomerOwnedChange(true) },
                    label = { Text("Customer Jar") }
                )
            }
        }

        // Stock error message
        if (stockError != null) {
            Text(
                text = stockError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun DateField(
    label: String,
    date: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = date }

    OutlinedCard(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val cal = Calendar.getInstance()
                    cal.set(year, month, day, 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    onDateSelected(cal.timeInMillis)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = DateUtils.formatDate(date),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
