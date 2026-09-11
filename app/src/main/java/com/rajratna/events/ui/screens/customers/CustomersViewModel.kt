package com.rajratna.events.ui.screens.customers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.Customer
import com.rajratna.events.data.entity.Order
import com.rajratna.events.data.entity.OrderStatus
import com.rajratna.events.data.repository.CustomerJarStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerWithStats(
    val customer: Customer,
    val totalOrders: Int = 0,
    val totalAmount: Double = 0.0,
    val totalPaid: Double = 0.0,
    val pendingBalance: Double = 0.0,
    val jarStats: CustomerJarStats = CustomerJarStats()
)

data class CustomersState(
    val customers: List<CustomerWithStats> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    // Quick jar entry state
    val selectedCustomer: Customer? = null,
    val selectedCustomerJarStats: CustomerJarStats? = null,
    val showQuickJar: Boolean = false,
    val showReturnJar: Boolean = false,
    val showRecordPayment: Boolean = false,
    val waterJarRate: Double = 30.0,
    val availableJarStock: Int = 0,
    val actionMessage: String? = null
)

class CustomersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RajratnaApp).repository
    private val _state = MutableStateFlow(CustomersState())
    val state: StateFlow<CustomersState> = _state.asStateFlow()

    private var allLoadedCustomers: List<CustomerWithStats> = emptyList()

    init { loadCustomers() }

    private fun loadCustomers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repository.getAllCustomers().collect { customers ->
                val allOrders = repository.getAllOrdersList()
                val allJarItems = repository.getAllOrderItemsList().filter {
                    it.itemName.equals("Water Jar", ignoreCase = true)
                }

                val jarItemsByOrder = allJarItems.groupBy { it.orderId }
                val ordersByCustomer = allOrders.groupBy { it.customerId }
                val monthStart = com.rajratna.events.util.DateUtils.startOfThisMonth()
                val monthEnd = com.rajratna.events.util.DateUtils.endOfThisMonth()

                val withStats = customers.map { customer ->
                    val custOrders = ordersByCustomer[customer.id] ?: emptyList()
                    val activeOrders = custOrders.filter { it.orderStatus != OrderStatus.CANCELLED }
                    val totalOrders = custOrders.size
                    val totalAmount = activeOrders.sumOf { it.grandTotal }
                    val grossPending = customer.pendingAmount + activeOrders.sumOf { it.balanceAmount }
                    val pendingBalance = maxOf(0.0, grossPending - customer.advanceBalance)
                    val totalPaid = totalAmount - pendingBalance

                    val monthOrders = activeOrders.filter { it.deliveryDate in monthStart until monthEnd }
                    var thisMonthJarCount = 0
                    var thisMonthJarAmount = 0.0
                    for (order in monthOrders) {
                        val items = jarItemsByOrder[order.id] ?: emptyList()
                        for (item in items) {
                            thisMonthJarCount += item.quantity
                            thisMonthJarAmount += item.totalAmount
                        }
                    }

                    var pendingReturnJars = customer.pendingReturnJars
                    for (order in activeOrders) {
                        if (order.orderStatus in listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED)) {
                            val items = jarItemsByOrder[order.id] ?: emptyList()
                            for (item in items) {
                                if (!item.isCustomerOwned) {
                                    val pending = item.quantity - item.returnedQuantity - item.damagedQuantity
                                    if (pending > 0) pendingReturnJars += pending
                                }
                            }
                        }
                    }

                    var lastJarQuantity = 0
                    var lastJarDate = 0L
                    var lastJarIsCustomerOwned = false
                    val sortedOrders = activeOrders.sortedByDescending { it.deliveryDate }
                    for (order in sortedOrders) {
                        val items = jarItemsByOrder[order.id] ?: emptyList()
                        if (items.isNotEmpty()) {
                            val first = items.first()
                            lastJarQuantity = first.quantity
                            lastJarDate = order.deliveryDate
                            lastJarIsCustomerOwned = first.isCustomerOwned
                            break
                        }
                    }

                    val jarStats = CustomerJarStats(
                        thisMonthJarCount = thisMonthJarCount,
                        thisMonthJarAmount = thisMonthJarAmount,
                        thisMonthPaid = 0.0,
                        totalPaid = totalPaid,
                        pendingBalance = pendingBalance,
                        pendingReturnJars = pendingReturnJars,
                        lastJarQuantity = lastJarQuantity,
                        lastJarDate = lastJarDate,
                        lastJarIsCustomerOwned = lastJarIsCustomerOwned,
                        advanceBalance = customer.advanceBalance
                    )

                    CustomerWithStats(customer, totalOrders, totalAmount, totalPaid, pendingBalance, jarStats)
                }

                allLoadedCustomers = withStats
                val currentQuery = _state.value.searchQuery
                val displayed = if (currentQuery.isBlank()) {
                    withStats
                } else {
                    filterCustomers(withStats, currentQuery)
                }
                _state.value = _state.value.copy(customers = displayed, isLoading = false)
            }
        }
    }

    fun updateSearch(query: String) {
        _state.value = _state.value.copy(
            searchQuery = query,
            customers = if (query.isBlank()) allLoadedCustomers else filterCustomers(allLoadedCustomers, query)
        )
    }

    private fun filterCustomers(list: List<CustomerWithStats>, query: String): List<CustomerWithStats> {
        val q = query.lowercase().trim()
        return list.filter {
            it.customer.name.lowercase().contains(q) ||
            it.customer.mobileNumber.contains(q) ||
            it.customer.address.lowercase().contains(q)
        }
    }

    // ── Quick Jar Entry ──────────────────────────────────────

    fun openQuickJar(customer: Customer) {
        viewModelScope.launch {
            val jarStats = repository.getCustomerJarStats(customer.id)
            val waterJar = repository.getWaterJarItem()
            val rate = waterJar?.ratePerDay ?: 30.0
            val stockMap = repository.getAvailableOnDate(com.rajratna.events.util.DateUtils.startOfToday())
            val availableStock = waterJar?.let { stockMap[it.id] } ?: 0
            _state.value = _state.value.copy(
                selectedCustomer = customer,
                selectedCustomerJarStats = jarStats,
                showQuickJar = true,
                waterJarRate = rate,
                availableJarStock = availableStock
            )
        }
    }

    fun dismissQuickJar() {
        _state.value = _state.value.copy(showQuickJar = false, selectedCustomer = null, selectedCustomerJarStats = null)
    }

    fun saveQuickJarEntry(quantity: Int, isCustomerOwned: Boolean, paidAmount: Double, deliveryDate: Long, customRate: Double? = null) {
        val customer = _state.value.selectedCustomer ?: return
        viewModelScope.launch {
            val waterJar = repository.getWaterJarItem() ?: return@launch
            repository.saveQuickJarEntry(customer, quantity, isCustomerOwned, paidAmount, deliveryDate, waterJar, customRate)
            _state.value = _state.value.copy(showQuickJar = false, selectedCustomer = null, selectedCustomerJarStats = null, actionMessage = "Jar entry saved for ${customer.name}")
            loadCustomers()
        }
    }

    // ── Return Jar ───────────────────────────────────────────

    fun openReturnJar(customer: Customer) {
        viewModelScope.launch {
            val jarStats = repository.getCustomerJarStats(customer.id)
            _state.value = _state.value.copy(
                selectedCustomer = customer,
                selectedCustomerJarStats = jarStats,
                showReturnJar = true
            )
        }
    }

    fun dismissReturnJar() {
        _state.value = _state.value.copy(showReturnJar = false, selectedCustomer = null, selectedCustomerJarStats = null)
    }

    fun saveJarReturn(returnedNow: Int, damagedNow: Int) {
        val customer = _state.value.selectedCustomer ?: return
        viewModelScope.launch {
            val pendingReturns = repository.getCustomerPendingJarReturns(customer.id)

            // Distribute returns across pending order items (oldest first)
            var remainingReturn = returnedNow
            var remainingDamaged = damagedNow
            for (pending in pendingReturns) {
                if (remainingReturn <= 0 && remainingDamaged <= 0) break
                val canProcess = pending.pendingQuantity
                val returnForThis = minOf(remainingReturn, canProcess)
                val leftAfterReturn = canProcess - returnForThis
                val damagedForThis = minOf(remainingDamaged, leftAfterReturn)

                if (returnForThis > 0 || damagedForThis > 0) {
                    repository.recordReturnWithDamaged(
                        pending.orderId,
                        mapOf(pending.orderItemId to returnForThis),
                        mapOf(pending.orderItemId to damagedForThis)
                    )
                }
                remainingReturn -= returnForThis
                remainingDamaged -= damagedForThis
            }

            // Deduct remaining from baseline pendingReturnJars on Customer entity
            val totalProcessedFromBaseline = remainingReturn + remainingDamaged
            if (totalProcessedFromBaseline > 0 && customer.pendingReturnJars > 0) {
                val newBaseline = maxOf(0, customer.pendingReturnJars - totalProcessedFromBaseline)
                repository.updateCustomer(customer.copy(pendingReturnJars = newBaseline))
            }

            _state.value = _state.value.copy(showReturnJar = false, selectedCustomer = null, selectedCustomerJarStats = null, actionMessage = "Jar return recorded for ${customer.name}")
            loadCustomers()
        }
    }

    // ── Record Payment ───────────────────────────────────────

    fun openRecordPayment(customer: Customer) {
        viewModelScope.launch {
            val jarStats = repository.getCustomerJarStats(customer.id)
            _state.value = _state.value.copy(
                selectedCustomer = customer,
                selectedCustomerJarStats = jarStats,
                showRecordPayment = true
            )
        }
    }

    fun dismissRecordPayment() {
        _state.value = _state.value.copy(showRecordPayment = false, selectedCustomer = null, selectedCustomerJarStats = null)
    }

    fun saveLumpSumPayment(amount: Double, paymentMethod: String) {
        val customer = _state.value.selectedCustomer ?: return
        viewModelScope.launch {
            var remaining = amount

            // First, pay off order-based balances
            remaining = repository.recordLumpSumPaymentAndGetRemaining(customer, remaining, paymentMethod)

            var currentCustomer = repository.getCustomerById(customer.id) ?: customer

            // Then deduct remaining from baseline pendingAmount on Customer entity
            if (remaining > 0 && currentCustomer.pendingAmount > 0) {
                val deductFromBaseline = minOf(remaining, currentCustomer.pendingAmount)
                val newBaseline = maxOf(0.0, currentCustomer.pendingAmount - deductFromBaseline)
                currentCustomer = currentCustomer.copy(pendingAmount = newBaseline)
                repository.updateCustomer(currentCustomer)
                remaining -= deductFromBaseline
            }

            // Any remaining excess payment is added to customer's advanceBalance
            if (remaining > 0) {
                val newAdvance = currentCustomer.advanceBalance + remaining
                repository.updateCustomer(currentCustomer.copy(advanceBalance = newAdvance))
            }

            val advanceMsg = if (remaining > 0) " (₹${remaining.toInt()} added to Advance Credit)" else ""
            _state.value = _state.value.copy(
                showRecordPayment = false,
                selectedCustomer = null,
                selectedCustomerJarStats = null,
                actionMessage = "Payment of ₹${amount.toInt()} recorded for ${customer.name}$advanceMsg"
            )
            loadCustomers()
        }
    }

    fun addCustomer(
        name: String,
        mobileNumber: String,
        address: String,
        totalJars: Int = 0,
        pendingReturnJars: Int = 0,
        pendingAmount: Double = 0.0,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedName = name.trim()
        val trimmedMobile = mobileNumber.trim()
        val trimmedAddress = address.trim()

        if (trimmedName.isBlank()) {
            onError("Customer name is required")
            return
        }
        if (trimmedMobile.isNotBlank() && trimmedMobile.length < 10) {
            onError("Please enter a valid 10-digit mobile number or leave blank")
            return
        }

        viewModelScope.launch {
            try {
                if (trimmedMobile.isNotBlank()) {
                    val existing = repository.getCustomerByMobile(trimmedMobile)
                    if (existing != null) {
                        onError("Customer with mobile $trimmedMobile already exists: ${existing.name}")
                        return@launch
                    }
                }
                repository.insertCustomer(
                    Customer(
                        name = trimmedName,
                        mobileNumber = trimmedMobile,
                        address = trimmedAddress,
                        totalJars = totalJars,
                        pendingReturnJars = pendingReturnJars,
                        pendingAmount = pendingAmount
                    )
                )
                _state.value = _state.value.copy(actionMessage = "Customer $trimmedName added successfully")
                loadCustomers()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to add customer")
            }
        }
    }

    fun updateCustomer(
        customer: Customer,
        name: String,
        mobileNumber: String,
        address: String,
        totalJars: Int,
        pendingReturnJars: Int,
        pendingAmount: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedName = name.trim()
        val trimmedMobile = mobileNumber.trim()
        val trimmedAddress = address.trim()

        if (trimmedName.isBlank()) {
            onError("Customer name is required")
            return
        }
        if (trimmedMobile.isNotBlank() && trimmedMobile.length < 10) {
            onError("Please enter a valid 10-digit mobile number or leave blank")
            return
        }

        viewModelScope.launch {
            try {
                val updated = customer.copy(
                    name = trimmedName,
                    mobileNumber = trimmedMobile,
                    address = trimmedAddress,
                    totalJars = totalJars,
                    pendingReturnJars = pendingReturnJars,
                    pendingAmount = pendingAmount
                )
                repository.updateCustomer(updated)
                _state.value = _state.value.copy(actionMessage = "Customer $trimmedName updated successfully")
                loadCustomers()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update customer")
            }
        }
    }

    fun deleteCustomer(
        customer: Customer,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(customer.id)
                _state.value = _state.value.copy(actionMessage = "Customer ${customer.name} deleted")
                loadCustomers()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to delete customer")
            }
        }
    }

    fun clearActionMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }
}
