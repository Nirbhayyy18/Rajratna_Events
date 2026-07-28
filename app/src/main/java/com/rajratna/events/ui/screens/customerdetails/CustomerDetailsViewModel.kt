package com.rajratna.events.ui.screens.customerdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.Customer
import com.rajratna.events.data.entity.Order
import com.rajratna.events.data.repository.CustomerJarStats
import com.rajratna.events.data.repository.JarEntry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerDetailsState(
    val customer: Customer? = null,
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    // Jar stats
    val jarStats: CustomerJarStats = CustomerJarStats(),
    val recentJarEntries: List<JarEntry> = emptyList(),
    // Bottom sheet state
    val showQuickJar: Boolean = false,
    val showReturnJar: Boolean = false,
    val showRecordPayment: Boolean = false,
    val waterJarRate: Double = 30.0,
    val availableJarStock: Int = 0,
    val actionMessage: String? = null
)

class CustomerDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RajratnaApp).repository
    private val _state = MutableStateFlow(CustomerDetailsState())
    val state: StateFlow<CustomerDetailsState> = _state.asStateFlow()

    fun loadCustomer(customerId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val customer = repository.getCustomerById(customerId)
            _state.value = _state.value.copy(customer = customer)

            // Load jar stats and recent entries
            if (customer != null) {
                val jarStats = repository.getCustomerJarStats(customerId)
                val recentEntries = repository.getRecentJarEntries(customerId)
                _state.value = _state.value.copy(jarStats = jarStats, recentJarEntries = recentEntries)
            }

            repository.getOrdersByCustomer(customerId).collect { orders ->
                _state.value = _state.value.copy(orders = orders, isLoading = false)
            }
        }
    }

    private fun refreshJarData() {
        val customerId = _state.value.customer?.id ?: return
        viewModelScope.launch {
            val jarStats = repository.getCustomerJarStats(customerId)
            val recentEntries = repository.getRecentJarEntries(customerId)
            _state.value = _state.value.copy(jarStats = jarStats, recentJarEntries = recentEntries)
        }
    }

    // ── Quick Jar Entry ──────────────────────────────────────

    fun openQuickJar() {
        viewModelScope.launch {
            val waterJar = repository.getWaterJarItem()
            val rate = waterJar?.ratePerDay ?: 30.0
            val stockMap = repository.getAvailableOnDate(com.rajratna.events.util.DateUtils.startOfToday())
            val availableStock = waterJar?.let { stockMap[it.id] } ?: 0
            _state.value = _state.value.copy(
                showQuickJar = true,
                waterJarRate = rate,
                availableJarStock = availableStock
            )
        }
    }

    fun dismissQuickJar() {
        _state.value = _state.value.copy(showQuickJar = false)
    }

    fun saveQuickJarEntry(quantity: Int, isCustomerOwned: Boolean, paidAmount: Double, deliveryDate: Long) {
        val customer = _state.value.customer ?: return
        viewModelScope.launch {
            val waterJar = repository.getWaterJarItem() ?: return@launch
            repository.saveQuickJarEntry(customer, quantity, isCustomerOwned, paidAmount, deliveryDate, waterJar)
            _state.value = _state.value.copy(showQuickJar = false, actionMessage = "Jar entry saved!")
            loadCustomer(customer.id)
        }
    }

    // ── Return Jar ───────────────────────────────────────────

    fun openReturnJar() {
        _state.value = _state.value.copy(showReturnJar = true)
    }

    fun dismissReturnJar() {
        _state.value = _state.value.copy(showReturnJar = false)
    }

    fun saveJarReturn(returnedNow: Int, damagedNow: Int) {
        val customer = _state.value.customer ?: return
        viewModelScope.launch {
            val pendingReturns = repository.getCustomerPendingJarReturns(customer.id)
            if (pendingReturns.isEmpty()) return@launch

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

            _state.value = _state.value.copy(showReturnJar = false, actionMessage = "Jar return recorded!")
            loadCustomer(customer.id)
        }
    }

    // ── Record Payment ───────────────────────────────────────

    fun openRecordPayment() {
        _state.value = _state.value.copy(showRecordPayment = true)
    }

    fun dismissRecordPayment() {
        _state.value = _state.value.copy(showRecordPayment = false)
    }

    fun saveLumpSumPayment(amount: Double, paymentMethod: String) {
        val customer = _state.value.customer ?: return
        viewModelScope.launch {
            repository.recordLumpSumPayment(customer, amount, paymentMethod)
            _state.value = _state.value.copy(showRecordPayment = false, actionMessage = "Payment of ₹${amount.toInt()} recorded!")
            loadCustomer(customer.id)
        }
    }

    fun clearActionMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }
}
