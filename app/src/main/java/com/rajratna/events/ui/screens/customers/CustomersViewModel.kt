package com.rajratna.events.ui.screens.customers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.Customer
import com.rajratna.events.data.entity.Order
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

    init { loadCustomers() }

    private fun loadCustomers() {
        viewModelScope.launch {
            repository.getAllCustomers().collect { customers ->
                val withStats = customers.map { customer ->
                    var totalOrders = 0; var totalAmount = 0.0; var pendingBalance = 0.0
                    repository.getOrdersByCustomer(customer.id).first().let { orders ->
                        totalOrders = orders.size
                        totalAmount = orders.filter { it.orderStatus != "Cancelled" }.sumOf { it.grandTotal }
                        pendingBalance = orders.filter { it.orderStatus != "Cancelled" }.sumOf { it.balanceAmount }
                    }
                    val jarStats = repository.getCustomerJarStats(customer.id)
                    CustomerWithStats(customer, totalOrders, totalAmount, totalAmount - pendingBalance, pendingBalance, jarStats)
                }
                _state.value = _state.value.copy(customers = withStats, isLoading = false)
            }
        }
    }

    fun updateSearch(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                loadCustomers()
            } else {
                repository.searchCustomers(query).collect { customers ->
                    val withStats = customers.map { c ->
                        val orders = repository.getOrdersByCustomer(c.id).first()
                        val active = orders.filter { it.orderStatus != "Cancelled" }
                        val jarStats = repository.getCustomerJarStats(c.id)
                        CustomerWithStats(c, orders.size, active.sumOf { it.grandTotal }, active.sumOf { it.grandTotal - it.balanceAmount }, active.sumOf { it.balanceAmount }, jarStats)
                    }
                    _state.value = _state.value.copy(customers = withStats, isLoading = false)
                }
            }
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

    fun saveQuickJarEntry(quantity: Int, isCustomerOwned: Boolean, paidAmount: Double, deliveryDate: Long) {
        val customer = _state.value.selectedCustomer ?: return
        viewModelScope.launch {
            val waterJar = repository.getWaterJarItem() ?: return@launch
            repository.saveQuickJarEntry(customer, quantity, isCustomerOwned, paidAmount, deliveryDate, waterJar)
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
            if (pendingReturns.isEmpty()) return@launch

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
            repository.recordLumpSumPayment(customer, amount, paymentMethod)
            _state.value = _state.value.copy(showRecordPayment = false, selectedCustomer = null, selectedCustomerJarStats = null, actionMessage = "Payment of ₹${amount.toInt()} recorded for ${customer.name}")
            loadCustomers()
        }
    }

    fun clearActionMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }
}
