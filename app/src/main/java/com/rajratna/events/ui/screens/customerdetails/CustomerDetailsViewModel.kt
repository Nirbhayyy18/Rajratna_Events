package com.rajratna.events.ui.screens.customerdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.Customer
import com.rajratna.events.data.entity.Order
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerDetailsState(
    val customer: Customer? = null,
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true
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
            repository.getOrdersByCustomer(customerId).collect { orders ->
                _state.value = _state.value.copy(orders = orders, isLoading = false)
            }
        }
    }
}
