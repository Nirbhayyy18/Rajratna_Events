package com.rajratna.events.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rajratna.events.data.entity.Customer
import com.rajratna.events.data.entity.PaymentMethod
import com.rajratna.events.data.repository.CustomerJarStats
import com.rajratna.events.ui.theme.*
import com.rajratna.events.util.DateUtils
import com.rajratna.events.util.toRupee

// ═══════════════════════════════════════════════════════════
// QUICK JAR ENTRY BOTTOM SHEET
// ═══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickJarBottomSheet(
    customer: Customer,
    jarStats: CustomerJarStats,
    jarRate: Double,
    availableStock: Int,
    onSave: (quantity: Int, isCustomerOwned: Boolean, paidAmount: Double, deliveryDate: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var quantity by remember { mutableIntStateOf(0) }
    var customQuantityText by remember { mutableStateOf("") }
    var isCustomQuantity by remember { mutableStateOf(false) }
    var isCustomerOwned by remember { mutableStateOf(false) }
    var paidOption by remember { mutableStateOf("zero") } // "zero", "full", "custom"
    var customPaidText by remember { mutableStateOf("") }
    var deliveryDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showConfirmation by remember { mutableStateOf(false) }

    val effectiveQuantity = if (isCustomQuantity) (customQuantityText.toIntOrNull() ?: 0) else quantity
    val totalAmount = effectiveQuantity * jarRate
    val paidAmount = when (paidOption) {
        "full" -> totalAmount
        "custom" -> customPaidText.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                "Add Jar Entry",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Customer info
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Customer: ${customer.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Mobile: ${customer.mobileNumber}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (jarStats.pendingBalance > 0) {
                        Text("Balance: ${jarStats.pendingBalance.toRupee()}", style = MaterialTheme.typography.bodyMedium, color = PaymentUnpaid, fontWeight = FontWeight.SemiBold)
                    }
                    if (jarStats.pendingReturnJars > 0) {
                        Text("Pending Return: ${jarStats.pendingReturnJars} jars", style = MaterialTheme.typography.bodyMedium, color = StatusPending)
                    }
                    if (jarStats.lastJarQuantity > 0) {
                        Text("Last: ${jarStats.lastJarQuantity} jars on ${DateUtils.formatShortDate(jarStats.lastJarDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Quantity
            Text("Quantity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3).forEach { q ->
                    FilterChip(
                        selected = !isCustomQuantity && quantity == q,
                        onClick = { quantity = q; isCustomQuantity = false },
                        label = { Text("$q") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                FilterChip(
                    selected = isCustomQuantity,
                    onClick = { isCustomQuantity = true; quantity = 0 },
                    label = { Text("Custom") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            if (isCustomQuantity) {
                OutlinedTextField(
                    value = customQuantityText,
                    onValueChange = { customQuantityText = it.filter { c -> c.isDigit() } },
                    label = { Text("Enter quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Jar Source
            Text("Jar Source", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isCustomerOwned,
                    onClick = { isCustomerOwned = false },
                    label = { Text("Our Jar") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = isCustomerOwned,
                    onClick = { isCustomerOwned = true },
                    label = { Text("Customer Jar") },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Rate & total
            if (effectiveQuantity > 0) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TealContainer)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("₹${jarRate.toInt()} × $effectiveQuantity", style = MaterialTheme.typography.bodyLarge)
                        Text("= ${totalAmount.toRupee()}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Teal40)
                    }
                }
            }

            // Stock warning (only for Our Jar)
            if (!isCustomerOwned && effectiveQuantity > availableStock && effectiveQuantity > 0) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusCancelledBg),
                    border = BorderStroke(1.dp, StatusCancelled)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, null, tint = StatusCancelled, modifier = Modifier.size(20.dp))
                        Text("Only $availableStock water jars available today.", style = MaterialTheme.typography.bodyMedium, color = StatusCancelled)
                    }
                }
            }

            // Paid Now
            Text("Paid Now", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = paidOption == "zero",
                    onClick = { paidOption = "zero" },
                    label = { Text("₹0") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = paidOption == "full",
                    onClick = { paidOption = "full" },
                    label = { Text("Full") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = paidOption == "custom",
                    onClick = { paidOption = "custom" },
                    label = { Text("Custom") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            if (paidOption == "custom") {
                OutlinedTextField(
                    value = customPaidText,
                    onValueChange = { customPaidText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Enter amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    prefix = { Text("₹") }
                )
            }

            // Save button
            Button(
                onClick = { showConfirmation = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = effectiveQuantity > 0 && (isCustomerOwned || effectiveQuantity <= availableStock)
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save Jar Entry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Confirmation dialog
    if (showConfirmation) {
        SaveConfirmationDialog(
            customerName = customer.name,
            quantity = effectiveQuantity,
            isCustomerOwned = isCustomerOwned,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            onConfirm = {
                showConfirmation = false
                onSave(effectiveQuantity, isCustomerOwned, paidAmount, deliveryDate)
            },
            onDismiss = { showConfirmation = false }
        )
    }
}

// ═══════════════════════════════════════════════════════════
// RETURN JAR BOTTOM SHEET
// ═══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnJarBottomSheet(
    customer: Customer,
    totalPendingJars: Int,
    onSave: (returnedNow: Int, damagedNow: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var returnedNow by remember { mutableIntStateOf(0) }
    var customReturnText by remember { mutableStateOf("") }
    var isCustomReturn by remember { mutableStateOf(false) }
    var damagedNow by remember { mutableIntStateOf(0) }
    var customDamagedText by remember { mutableStateOf("") }
    var isCustomDamaged by remember { mutableStateOf(false) }

    val effectiveReturned = if (isCustomReturn) (customReturnText.toIntOrNull() ?: 0) else returnedNow
    val effectiveDamaged = if (isCustomDamaged) (customDamagedText.toIntOrNull() ?: 0) else damagedNow
    val totalProcessing = effectiveReturned + effectiveDamaged
    val isValid = totalProcessing > 0 && totalProcessing <= totalPendingJars

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Return Jar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Customer: ${customer.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Pending Return: $totalPendingJars jars", style = MaterialTheme.typography.bodyMedium, color = StatusPending, fontWeight = FontWeight.SemiBold)
                }
            }

            // Returned Now
            Text("Returned Now", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..minOf(3, totalPendingJars)).forEach { q ->
                    FilterChip(
                        selected = !isCustomReturn && returnedNow == q,
                        onClick = { returnedNow = q; isCustomReturn = false },
                        label = { Text("$q") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                FilterChip(
                    selected = isCustomReturn,
                    onClick = { isCustomReturn = true; returnedNow = 0 },
                    label = { Text("Custom") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            if (isCustomReturn) {
                OutlinedTextField(
                    value = customReturnText,
                    onValueChange = { customReturnText = it.filter { c -> c.isDigit() } },
                    label = { Text("Enter returned quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Damaged / Missing
            Text("Damaged / Missing", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isCustomDamaged && damagedNow == 0,
                    onClick = { damagedNow = 0; isCustomDamaged = false },
                    label = { Text("0") },
                    shape = RoundedCornerShape(12.dp)
                )
                if (totalPendingJars >= 1) {
                    FilterChip(
                        selected = !isCustomDamaged && damagedNow == 1,
                        onClick = { damagedNow = 1; isCustomDamaged = false },
                        label = { Text("1") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                FilterChip(
                    selected = isCustomDamaged,
                    onClick = { isCustomDamaged = true; damagedNow = 0 },
                    label = { Text("Custom") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            if (isCustomDamaged) {
                OutlinedTextField(
                    value = customDamagedText,
                    onValueChange = { customDamagedText = it.filter { c -> c.isDigit() } },
                    label = { Text("Enter damaged/missing quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Validation warning
            if (totalProcessing > totalPendingJars) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusCancelledBg),
                    border = BorderStroke(1.dp, StatusCancelled)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, null, tint = StatusCancelled, modifier = Modifier.size(20.dp))
                        Text("Total (returned + damaged) cannot exceed $totalPendingJars pending jars.", style = MaterialTheme.typography.bodyMedium, color = StatusCancelled)
                    }
                }
            }

            Button(
                onClick = { onSave(effectiveReturned, effectiveDamaged) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = isValid
            ) {
                Icon(Icons.Default.AssignmentReturn, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save Return", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// RECORD PAYMENT BOTTOM SHEET
// ═══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentBottomSheet(
    customer: Customer,
    pendingAmount: Double,
    onSave: (amount: Double, paymentMethod: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountOption by remember { mutableStateOf("full") }
    var customAmountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var showExcessWarning by remember { mutableStateOf(false) }

    val effectiveAmount = when (amountOption) {
        "full" -> pendingAmount
        else -> customAmountText.toDoubleOrNull() ?: 0.0
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Record Payment",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Customer: ${customer.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Pending: ${pendingAmount.toRupee()}", style = MaterialTheme.typography.bodyMedium, color = PaymentUnpaid, fontWeight = FontWeight.SemiBold)
                }
            }

            // Amount
            Text("Amount", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = amountOption == "full",
                    onClick = { amountOption = "full" },
                    label = { Text("Full ${pendingAmount.toRupee()}") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = amountOption == "custom",
                    onClick = { amountOption = "custom" },
                    label = { Text("Custom") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            if (amountOption == "custom") {
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = { customAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Enter amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    prefix = { Text("₹") }
                )
            }

            // Payment Mode
            Text("Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(PaymentMethod.CASH, PaymentMethod.UPI, PaymentMethod.OTHER).forEach { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method },
                        label = { Text(method) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (effectiveAmount > pendingAmount) {
                        showExcessWarning = true
                    } else {
                        onSave(effectiveAmount, paymentMethod)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = effectiveAmount > 0
            ) {
                Icon(Icons.Default.Payment, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Excess payment confirmation
    if (showExcessWarning) {
        AlertDialog(
            onDismissRequest = { showExcessWarning = false },
            title = { Text("Excess Payment") },
            text = { Text("Payment amount (${effectiveAmount.toRupee()}) exceeds pending balance (${pendingAmount.toRupee()}). Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    showExcessWarning = false
                    onSave(effectiveAmount, paymentMethod)
                }) { Text("Save Anyway") }
            },
            dismissButton = {
                TextButton(onClick = { showExcessWarning = false }) { Text("Cancel") }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════
// SAVE CONFIRMATION DIALOG
// ═══════════════════════════════════════════════════════════

@Composable
fun SaveConfirmationDialog(
    customerName: String,
    quantity: Int,
    isCustomerOwned: Boolean,
    totalAmount: Double,
    paidAmount: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val jarType = if (isCustomerOwned) "customer jars" else "water jars"
    val balance = totalAmount - paidAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.WaterDrop, null, tint = Teal40) },
        title = { Text("Confirm Jar Entry", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Add $quantity $jarType for $customerName today?")
                Spacer(Modifier.height(8.dp))
                Text("Amount: ${totalAmount.toRupee()}", fontWeight = FontWeight.SemiBold)
                Text("Paid Now: ${paidAmount.toRupee()}")
                if (balance > 0) {
                    Text("Balance: ${balance.toRupee()}", color = PaymentUnpaid, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
