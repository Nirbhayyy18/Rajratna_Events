package com.rajratna.events.ui.screens.customers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.Customer
import com.rajratna.events.data.entity.Order
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerWithStats(
    val customer: Customer,
    val totalOrders: Int = 0,
    val totalAmount: Double = 0.0,
    val totalPaid: Double = 0.0,
    val pendingBalance: Double = 0.0
)

data class CustomersState(
    val customers: List<CustomerWithStats> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = ""
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
                    CustomerWithStats(customer, totalOrders, totalAmount, totalAmount - pendingBalance, pendingBalance)
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
                        CustomerWithStats(c, orders.size, active.sumOf { it.grandTotal }, active.sumOf { it.grandTotal - it.balanceAmount }, active.sumOf { it.balanceAmount })
                    }
                    _state.value = _state.value.copy(customers = withStats, isLoading = false)
                }
            }
        }
    }
}
