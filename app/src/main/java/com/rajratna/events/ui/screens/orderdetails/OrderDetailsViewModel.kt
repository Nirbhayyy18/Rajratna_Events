package com.rajratna.events.ui.screens.orderdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.Order
import com.rajratna.events.data.entity.OrderItem
import com.rajratna.events.data.entity.Payment
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OrderDetailsState(
    val order: Order? = null,
    val orderItems: List<OrderItem> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val isLoading: Boolean = true
)

class OrderDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RajratnaApp).repository

    private val _state = MutableStateFlow(OrderDetailsState())
    val state: StateFlow<OrderDetailsState> = _state.asStateFlow()

    private var currentOrderId: Long = -1

    fun loadOrder(orderId: Long) {
        if (currentOrderId == orderId) return
        currentOrderId = orderId
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Combine flows for order, its items, and its payments
            combine(
                repository.getOrderByIdFlow(orderId),
                repository.getOrderItems(orderId),
                repository.getPaymentsByOrder(orderId)
            ) { order, items, payments ->
                OrderDetailsState(
                    order = order,
                    orderItems = items,
                    payments = payments,
                    isLoading = false
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun updateStatus(status: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.updateOrderStatus(currentOrderId, status)
            onSuccess()
        }
    }

    fun recordPayment(amount: Double, method: String, notes: String, onSuccess: () -> Unit) {
        val order = _state.value.order ?: return
        viewModelScope.launch {
            val payment = Payment(
                orderId = order.id,
                customerName = order.customerName,
                customerMobile = order.customerMobile,
                amount = amount,
                paymentDate = System.currentTimeMillis(),
                paymentMethod = method,
                notes = notes
            )
            repository.recordPayment(payment)
            onSuccess()
        }
    }

    fun recordReturn(returnEntries: Map<Long, Int>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.recordReturn(currentOrderId, returnEntries)
            onSuccess()
        }
    }
}
