package com.rajratna.events.ui.screens.returns

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.Order
import com.rajratna.events.data.entity.OrderItem
import com.rajratna.events.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Pending return display model combining order and its unreturned items.
 */
data class PendingReturnOrder(
    val order: Order,
    val items: List<OrderItem>,
    val isOverdue: Boolean,
    val isDueToday: Boolean,
    val isUpcoming: Boolean
) {
    val pendingItems: List<OrderItem>
        get() = items.filter { it.quantity > it.returnedQuantity }
}

/**
 * Returned order display model.
 */
data class ReturnedOrder(
    val order: Order,
    val items: List<OrderItem>,
    val isFullyReturned: Boolean
)

/**
 * Return entry for the record return dialog.
 */
data class ReturnEntry(
    val orderItemId: Long,
    val itemName: String,
    val givenQuantity: Int,
    val alreadyReturned: Int,
    val pendingQuantity: Int,
    val returnedNow: Int = 0
)

enum class PendingFilter { ALL, DUE_TODAY, OVERDUE, UPCOMING }
enum class ReturnedFilter { TODAY, THIS_WEEK, THIS_MONTH, ALL }

data class ReturnsState(
    val isLoading: Boolean = true,
    val selectedTab: Int = 0, // 0 = Pending, 1 = Returned
    val pendingFilter: PendingFilter = PendingFilter.ALL,
    val selectedDate: Long? = null,
    val returnedFilter: ReturnedFilter = ReturnedFilter.TODAY,
    val pendingOrders: List<PendingReturnOrder> = emptyList(),
    val filteredPendingOrders: List<PendingReturnOrder> = emptyList(),
    val returnedOrders: List<ReturnedOrder> = emptyList(),
    val filteredReturnedOrders: List<ReturnedOrder> = emptyList(),
    // Record Return dialog state
    val showRecordReturn: Boolean = false,
    val recordReturnOrderId: Long = 0,
    val returnEntries: List<ReturnEntry> = emptyList(),
    val isSaving: Boolean = false
)

class ReturnsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as RajratnaApp).repository

    private val _state = MutableStateFlow(ReturnsState())
    val state: StateFlow<ReturnsState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val todayStart = DateUtils.startOfToday()
            val todayEnd = DateUtils.endOfToday()

            // Load pending returns
            val pendingOrders = repository.getOrdersWithPendingReturns()
            val pendingReturnOrders = pendingOrders.map { order ->
                val items = repository.getOrderItemsList(order.id).filter { !it.isCustomerOwned }
                PendingReturnOrder(
                    order = order,
                    items = items,
                    isOverdue = order.returnDate < todayStart,
                    isDueToday = order.returnDate in todayStart until todayEnd,
                    isUpcoming = order.returnDate >= todayEnd
                )
            }

            // Load returned orders
            val returnedOrders = repository.getReturnedOrders()
            val returnedOrderModels = returnedOrders.map { order ->
                val items = repository.getOrderItemsList(order.id).filter { !it.isCustomerOwned }
                val fullyReturned = items.all { it.quantity <= it.returnedQuantity }
                ReturnedOrder(
                    order = order,
                    items = items,
                    isFullyReturned = fullyReturned
                )
            }

            _state.value = _state.value.copy(
                isLoading = false,
                pendingOrders = pendingReturnOrders,
                returnedOrders = returnedOrderModels
            )

            applyPendingFilter(_state.value.pendingFilter)
            applyReturnedFilter(_state.value.returnedFilter)
        }
    }

    fun selectTab(tab: Int) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    fun setPendingFilter(filter: PendingFilter) {
        _state.value = _state.value.copy(pendingFilter = filter, selectedDate = null)
        applyPendingFilter(filter)
    }

    fun setSelectedDate(date: Long?) {
        _state.value = _state.value.copy(selectedDate = date)
        applyPendingFilter(_state.value.pendingFilter)
    }

    fun setReturnedFilter(filter: ReturnedFilter) {
        _state.value = _state.value.copy(returnedFilter = filter)
        applyReturnedFilter(filter)
    }

    private fun applyPendingFilter(filter: PendingFilter) {
        val selectedDate = _state.value.selectedDate
        val filtered = if (selectedDate != null) {
            val dateEnd = selectedDate + 86400000L // 24 hours in ms
            _state.value.pendingOrders.filter { 
                it.order.returnDate in selectedDate until dateEnd
            }
        } else {
            when (filter) {
                PendingFilter.ALL -> _state.value.pendingOrders
                PendingFilter.DUE_TODAY -> _state.value.pendingOrders.filter { it.isDueToday }
                PendingFilter.OVERDUE -> _state.value.pendingOrders.filter { it.isOverdue }
                PendingFilter.UPCOMING -> _state.value.pendingOrders.filter { it.isUpcoming }
            }
        }
        _state.value = _state.value.copy(filteredPendingOrders = filtered)
    }

    private fun applyReturnedFilter(filter: ReturnedFilter) {
        val todayStart = DateUtils.startOfToday()
        val todayEnd = DateUtils.endOfToday()
        val weekStart = DateUtils.startOfThisWeek()
        val monthStart = DateUtils.startOfThisMonth()

        val filtered = when (filter) {
            ReturnedFilter.TODAY -> _state.value.returnedOrders.filter {
                it.order.updatedAt in todayStart until todayEnd
            }
            ReturnedFilter.THIS_WEEK -> _state.value.returnedOrders.filter {
                it.order.updatedAt >= weekStart
            }
            ReturnedFilter.THIS_MONTH -> _state.value.returnedOrders.filter {
                it.order.updatedAt >= monthStart
            }
            ReturnedFilter.ALL -> _state.value.returnedOrders
        }
        _state.value = _state.value.copy(filteredReturnedOrders = filtered)
    }

    // ── Record Return ───────────────────────────────────────

    fun openRecordReturn(orderId: Long) {
        viewModelScope.launch {
            val items = repository.getOrderItemsList(orderId)
            val entries = items
                .filter { !it.isCustomerOwned && it.quantity > it.returnedQuantity }
                .map { item ->
                    ReturnEntry(
                        orderItemId = item.id,
                        itemName = item.itemName,
                        givenQuantity = item.quantity,
                        alreadyReturned = item.returnedQuantity,
                        pendingQuantity = item.quantity - item.returnedQuantity,
                        returnedNow = 0
                    )
                }
            _state.value = _state.value.copy(
                showRecordReturn = true,
                recordReturnOrderId = orderId,
                returnEntries = entries
            )
        }
    }

    fun closeRecordReturn() {
        _state.value = _state.value.copy(
            showRecordReturn = false,
            recordReturnOrderId = 0,
            returnEntries = emptyList()
        )
    }

    fun updateReturnedNow(orderItemId: Long, value: Int) {
        val entries = _state.value.returnEntries.map { entry ->
            if (entry.orderItemId == orderItemId) {
                entry.copy(returnedNow = value.coerceIn(0, entry.pendingQuantity))
            } else entry
        }
        _state.value = _state.value.copy(returnEntries = entries)
    }

    fun markAllReturned() {
        val entries = _state.value.returnEntries.map { entry ->
            entry.copy(returnedNow = entry.pendingQuantity)
        }
        _state.value = _state.value.copy(returnEntries = entries)
    }

    fun saveReturn(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)

            val returnMap = _state.value.returnEntries
                .filter { it.returnedNow > 0 }
                .associate { it.orderItemId to it.returnedNow }

            if (returnMap.isNotEmpty()) {
                repository.recordReturn(_state.value.recordReturnOrderId, returnMap)
            }

            _state.value = _state.value.copy(isSaving = false)
            closeRecordReturn()
            loadData()
            onSuccess()
        }
    }
}
