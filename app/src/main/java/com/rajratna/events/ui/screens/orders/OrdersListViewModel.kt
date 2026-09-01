package com.rajratna.events.ui.screens.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.Order
import com.rajratna.events.data.entity.OrderStatus
import com.rajratna.events.data.entity.PaymentStatusType
import com.rajratna.events.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OrdersListState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    // Date filter
    val dateFilter: String = "All",
    val customDateStart: Long? = null,
    val customDateEnd: Long? = null,
    // Advanced filters
    val statusFilter: String? = null,
    val paymentFilter: String? = null,
    val customerFilter: String = "",
    val showAdvancedFilters: Boolean = false
)

class OrdersListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as RajratnaApp).repository

    private val _state = MutableStateFlow(OrdersListState())
    val state: StateFlow<OrdersListState> = _state.asStateFlow()

    val dateFilters = listOf("All", "Today", "Tomorrow", "Week")

    private var filterJob: Job? = null

    init {
        applyFilters()
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilters()
    }

    fun selectDateFilter(filter: String) {
        _state.value = _state.value.copy(
            dateFilter = filter,
            customDateStart = null,
            customDateEnd = null
        )
        applyFilters()
    }

    fun selectCustomDate(start: Long, end: Long) {
        _state.value = _state.value.copy(
            dateFilter = "Custom",
            customDateStart = start,
            customDateEnd = end
        )
        applyFilters()
    }

    fun setStatusFilter(status: String?) {
        _state.value = _state.value.copy(statusFilter = status)
    }

    fun setPaymentFilter(payment: String?) {
        _state.value = _state.value.copy(paymentFilter = payment)
    }

    fun setCustomerFilter(customer: String) {
        _state.value = _state.value.copy(customerFilter = customer)
    }

    fun toggleAdvancedFilters() {
        _state.value = _state.value.copy(showAdvancedFilters = !_state.value.showAdvancedFilters)
    }

    fun applyAdvancedFilters() {
        _state.value = _state.value.copy(showAdvancedFilters = false)
        applyFilters()
    }

    fun clearAdvancedFilters() {
        _state.value = _state.value.copy(
            statusFilter = null,
            paymentFilter = null,
            customerFilter = "",
            showAdvancedFilters = false
        )
        applyFilters()
    }

    fun clearAllFilters() {
        _state.value = OrdersListState(dateFilter = "All")
        applyFilters()
    }

    fun applyFilters() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val s = _state.value

            // Step 1: Get base orders by date filter
            val dateRange = getDateRange(s.dateFilter, s.customDateStart, s.customDateEnd)
            val sourceFlow: Flow<List<Order>> = if (dateRange != null) {
                repository.getOrdersByDeliveryDate(dateRange.first, dateRange.second)
            } else {
                repository.getAllOrders()
            }

            sourceFlow.collect { allOrders ->
                var filtered = allOrders

                // Step 2: Apply search
                if (s.searchQuery.isNotBlank()) {
                    val q = s.searchQuery.lowercase()
                    filtered = filtered.filter {
                        it.customerName.lowercase().contains(q) ||
                        it.customerMobile.contains(q) ||
                        it.billNumber.toString().contains(q)
                    }
                }

                // Step 3: Apply status filter
                if (s.statusFilter != null) {
                    filtered = filtered.filter { it.orderStatus == s.statusFilter }
                }

                // Step 4: Apply payment filter
                if (s.paymentFilter != null) {
                    filtered = filtered.filter { it.paymentStatus == s.paymentFilter }
                }

                // Step 5: Apply customer filter
                if (s.customerFilter.isNotBlank()) {
                    val cf = s.customerFilter.lowercase()
                    filtered = filtered.filter {
                        it.customerName.lowercase().contains(cf)
                    }
                }

                _state.value = _state.value.copy(orders = filtered, isLoading = false)
            }
        }
    }

    private fun getDateRange(filter: String, customStart: Long?, customEnd: Long?): Pair<Long, Long>? {
        return when (filter) {
            "Today" -> DateUtils.startOfToday() to DateUtils.endOfToday()
            "Tomorrow" -> DateUtils.startOfTomorrow() to DateUtils.endOfTomorrow()
            "Week" -> DateUtils.startOfThisWeek() to DateUtils.endOfThisWeek()
            "Custom" -> if (customStart != null && customEnd != null) customStart to customEnd else null
            else -> null // "All"
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status)
        }
    }

    val hasActiveAdvancedFilters: Boolean
        get() {
            val s = _state.value
            return s.statusFilter != null || s.paymentFilter != null || s.customerFilter.isNotBlank()
        }
}
