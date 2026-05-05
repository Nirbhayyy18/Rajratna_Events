package com.rajratna.events.ui.screens.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.Order
import com.rajratna.events.data.entity.OrderStatus
import com.rajratna.events.data.entity.PaymentStatusType
import com.rajratna.events.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OrdersListState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val activeFilter: String = "All"
)

class OrdersListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as RajratnaApp).repository

    private val _state = MutableStateFlow(OrdersListState())
    val state: StateFlow<OrdersListState> = _state.asStateFlow()

    val filters = listOf(
        "All", "Today", "Tomorrow", "This Week", "This Month",
        OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.DELIVERED,
        OrderStatus.COMPLETED, OrderStatus.CANCELLED,
        PaymentStatusType.UNPAID, PaymentStatusType.PARTIALLY_PAID, PaymentStatusType.PAID
    )

    init {
        applyFilter("All")
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        if (query.isNotBlank()) {
            viewModelScope.launch {
                repository.searchOrders(query).collect { orders ->
                    _state.value = _state.value.copy(orders = orders, isLoading = false)
                }
            }
        } else {
            applyFilter(_state.value.activeFilter)
        }
    }

    fun applyFilter(filter: String) {
        _state.value = _state.value.copy(activeFilter = filter, isLoading = true, searchQuery = "")
        viewModelScope.launch {
            val flow: Flow<List<Order>> = when (filter) {
                "All" -> repository.getAllOrders()
                "Today" -> repository.getOrdersByDate(DateUtils.startOfToday(), DateUtils.endOfToday())
                "Tomorrow" -> repository.getOrdersByDeliveryDate(DateUtils.startOfTomorrow(), DateUtils.endOfTomorrow())
                "This Week" -> repository.getOrdersInRange(DateUtils.startOfThisWeek(), DateUtils.endOfThisWeek())
                "This Month" -> repository.getOrdersInRange(DateUtils.startOfThisMonth(), DateUtils.endOfThisMonth())
                OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.DELIVERED,
                OrderStatus.COMPLETED, OrderStatus.CANCELLED -> repository.getOrdersByStatus(filter)
                PaymentStatusType.UNPAID, PaymentStatusType.PARTIALLY_PAID,
                PaymentStatusType.PAID -> repository.getOrdersByPaymentStatus(filter)
                else -> repository.getAllOrders()
            }
            flow.collect { orders ->
                _state.value = _state.value.copy(orders = orders, isLoading = false)
            }
        }
    }

    fun updateOrderStatus(orderId: Long, status: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status)
        }
    }
}
