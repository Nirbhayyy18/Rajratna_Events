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
    val outStock: Int,
    val lowStockAlert: Int,
    val isLowStock: Boolean,
    val riskStock: Int = 0
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
    // Item-wise stock (date-aware)
    val itemStocks: List<ItemStockInfo> = emptyList(),
    val selectedStockDate: Long = DateUtils.startOfToday(),
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
            val selectedDate = _state.value.selectedStockDate

            // Items and date-wise stock
            val itemStocks = loadStockForDate(selectedDate)

            // Pending return orders (due today or overdue)
            val pendingReturnOrders = repository.getPendingReturnOrders(todayEnd, limit = 3)

            val pendingReturns = pendingReturnOrders.map { order ->
                val orderItems = repository.getOrderItemsList(order.id)
                val pendingItems = orderItems
                    .filter { !it.isCustomerOwned && it.quantity > it.returnedQuantity }
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
                // Item-wise stock (date-aware)
                itemStocks = itemStocks,
                selectedStockDate = selectedDate,
                // Counts
                activeOrderCount = repository.getActiveOrderCount(),
                returnedTodayCount = repository.getReturnedTodayCount(todayStart, todayEnd),
                pendingReturnCount = repository.getPendingReturnCount(todayEnd),
                // Previews
                pendingReturns = pendingReturns
            )
        }
    }

    /**
     * Change the selected stock date and reload stock data only.
     */
    fun selectStockDate(date: Long) {
        viewModelScope.launch {
            val itemStocks = loadStockForDate(date)
            _state.value = _state.value.copy(
                selectedStockDate = date,
                itemStocks = itemStocks
            )
        }
    }

    private suspend fun loadStockForDate(date: Long): List<ItemStockInfo> {
        val stockDetailsList = repository.getStockDetailsForDate(date)
        val items = repository.getAllItemsList().filter { it.isActive }
        val detailsMap = stockDetailsList.associateBy { it.itemId }

        return items.map { item ->
            val details = detailsMap[item.id]
            val outStock = details?.outQty ?: 0
            val available = details?.availableQty ?: item.totalStock
            val risk = details?.riskQty ?: 0

            ItemStockInfo(
                itemId = item.id,
                name = item.name,
                totalStock = item.totalStock,
                availableStock = available,
                outStock = outStock,
                lowStockAlert = item.lowStockAlert,
                isLowStock = item.lowStockAlert > 0 && available <= item.lowStockAlert,
                riskStock = risk
            )
        }
    }
}
