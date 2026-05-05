package com.rajratna.events.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.OrderItem
import com.rajratna.events.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Per-item stock info for the dashboard.
 */
data class ItemStockInfo(
    val itemId: Long,
    val name: String,
    val totalStock: Int,
    val availableStock: Int,
    val rentedStock: Int,
    val lowStockAlert: Int,
    val isLowStock: Boolean
)

/**
 * Pending return preview for dashboard cards.
 */
data class PendingReturnPreview(
    val orderId: Long,
    val billNumber: Int,
    val customerName: String,
    val customerMobile: String,
    val returnDate: Long,
    val isOverdue: Boolean,
    val isDueToday: Boolean,
    val pendingItems: List<PendingItemInfo>
)

data class PendingItemInfo(
    val itemName: String,
    val pendingQuantity: Int
)

data class DashboardState(
    val isLoading: Boolean = true,
    // Today Summary
    val todayIncome: Double = 0.0,
    val todayPendingPayment: Double = 0.0,
    val todayOrderCount: Int = 0,
    // Item-wise stock
    val itemStocks: List<ItemStockInfo> = emptyList(),
    // Order status counts
    val activeOrderCount: Int = 0,
    val returnedTodayCount: Int = 0,
    val pendingReturnCount: Int = 0,
    // Pending returns preview
    val pendingReturns: List<PendingReturnPreview> = emptyList()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as RajratnaApp).repository

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val todayStart = DateUtils.startOfToday()
            val todayEnd = DateUtils.endOfToday()

            // Items and stock
            val items = repository.getAllItemsList().filter { it.isActive }
            val rentedByItem = repository.getRentedQuantities()
                .associate { it.itemId to it.totalRented }

            val itemStocks = items.map { item ->
                val rented = rentedByItem[item.id] ?: 0
                val available = maxOf(0, item.totalStock - rented)
                ItemStockInfo(
                    itemId = item.id,
                    name = item.name,
                    totalStock = item.totalStock,
                    availableStock = available,
                    rentedStock = rented,
                    lowStockAlert = item.lowStockAlert,
                    isLowStock = item.lowStockAlert > 0 && available <= item.lowStockAlert
                )
            }

            // Pending return orders (due today or overdue)
            val pendingReturnOrders = repository.getPendingReturnOrders(todayEnd, limit = 3)

            val pendingReturns = pendingReturnOrders.map { order ->
                val orderItems = repository.getOrderItemsList(order.id)
                val pendingItems = orderItems
                    .filter { it.quantity > it.returnedQuantity }
                    .map { PendingItemInfo(it.itemName, it.quantity - it.returnedQuantity) }

                PendingReturnPreview(
                    orderId = order.id,
                    billNumber = order.billNumber,
                    customerName = order.customerName,
                    customerMobile = order.customerMobile,
                    returnDate = order.returnDate,
                    isOverdue = order.returnDate < todayStart,
                    isDueToday = order.returnDate in todayStart until todayEnd,
                    pendingItems = pendingItems
                )
            }

            _state.value = DashboardState(
                isLoading = false,
                // Today summary
                todayIncome = repository.getTotalPaymentReceived(todayStart, todayEnd),
                todayPendingPayment = repository.getTotalPendingBalance(todayStart, todayEnd),
                todayOrderCount = repository.getOrderCount(todayStart, todayEnd),
                // Item-wise stock
                itemStocks = itemStocks,
                // Counts
                activeOrderCount = repository.getActiveOrderCount(),
                returnedTodayCount = repository.getReturnedTodayCount(todayStart, todayEnd),
                pendingReturnCount = repository.getPendingReturnCount(todayEnd),
                // Previews
                pendingReturns = pendingReturns
            )
        }
    }
}
