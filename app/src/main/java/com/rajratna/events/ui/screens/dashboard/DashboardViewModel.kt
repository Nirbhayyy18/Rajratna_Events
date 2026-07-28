package com.rajratna.events.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.OrderStatus
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

data class DashboardAlertInfo(
    val type: DashboardAlertType,
    val count: Int,
    val description: String
)

enum class DashboardAlertType {
    OVERDUE_RETURNS,
    PENDING_PAYMENTS,
    LOW_STOCK,
    TOMORROW_BOOKINGS
}

data class UpcomingDeliveryInfo(
    val orderId: Long,
    val deliveryDate: Long,
    val customerName: String,
    val itemSummary: String
)

data class DashboardState(
    val isLoading: Boolean = true,
    val selectedOverviewDate: Long = DateUtils.startOfToday(),
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
    // Alerts + deliveries
    val alerts: List<DashboardAlertInfo> = emptyList(),
    val upcomingDeliveries: List<UpcomingDeliveryInfo> = emptyList(),
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
            _state.value = buildDashboardState(
                selectedStockDate = _state.value.selectedStockDate,
                selectedOverviewDate = _state.value.selectedOverviewDate
            )
        }
    }

    fun selectOverviewDate(date: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            _state.value = buildDashboardState(
                selectedStockDate = _state.value.selectedStockDate,
                selectedOverviewDate = DateUtils.startOfDay(date)
            )
        }
    }

    /**
     * Change the selected stock date and reload stock data only.
     */
    fun selectStockDate(date: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            _state.value = buildDashboardState(
                selectedStockDate = DateUtils.startOfDay(date),
                selectedOverviewDate = _state.value.selectedOverviewDate
            )
        }
    }

    private suspend fun buildDashboardState(selectedStockDate: Long, selectedOverviewDate: Long): DashboardState {
        val todayStart = DateUtils.startOfToday()
        val todayEnd = DateUtils.endOfToday()
        val selectedOverviewEnd = selectedOverviewDate + 24 * 60 * 60 * 1000L
        val tomorrowStart = DateUtils.startOfTomorrow()
        val tomorrowEnd = DateUtils.endOfTomorrow()

        val itemStocks = loadStockForDate(selectedStockDate)
        val allOrders = repository.getAllOrdersList()
        val pendingReturnOrders = repository.getOrdersWithPendingReturns()

        val overdueReturnCount = pendingReturnOrders.count { it.returnDate < todayStart }
        val pendingPaymentsCount = allOrders.count { it.orderStatus != OrderStatus.CANCELLED && it.balanceAmount > 0.0 }
        val lowStockCount = itemStocks.count { it.isLowStock }
        val tomorrowBookings = allOrders.filter {
            it.orderStatus != OrderStatus.CANCELLED &&
                it.deliveryDate >= tomorrowStart &&
                it.deliveryDate < tomorrowEnd
        }

        var tomorrowItemCount = 0
        for (order in tomorrowBookings) {
            tomorrowItemCount += repository.getOrderItemsList(order.id)
                .filter { !it.isCustomerOwned }
                .sumOf { it.quantity }
        }

        val upcomingOrders = allOrders
            .filter {
                it.orderStatus != OrderStatus.CANCELLED &&
                    it.orderStatus != OrderStatus.COMPLETED &&
                    it.deliveryDate >= todayStart
            }
            .sortedBy { it.deliveryDate }
            .take(2)
        val upcomingDeliveries = mutableListOf<UpcomingDeliveryInfo>()
        for (order in upcomingOrders) {
            val itemSummary = repository.getOrderItemsList(order.id)
                .filter { !it.isCustomerOwned }
                .take(3)
                .joinToString(", ") { "${it.itemName} x${it.quantity}" }
                .ifBlank { "No items" }

            upcomingDeliveries.add(UpcomingDeliveryInfo(
                orderId = order.id,
                deliveryDate = order.deliveryDate,
                customerName = order.customerName,
                itemSummary = itemSummary
            ))
        }

        val pendingReturnOrdersFromRepo = repository.getPendingReturnOrders(todayEnd, limit = 3)
        val pendingReturns = mutableListOf<PendingReturnPreview>()
        for (order in pendingReturnOrdersFromRepo) {
            val orderItems = repository.getOrderItemsList(order.id)
            val pendingItems = orderItems
                .filter { !it.isCustomerOwned && it.quantity > (it.returnedQuantity + it.damagedQuantity) }
                .map { PendingItemInfo(it.itemName, it.quantity - it.returnedQuantity - it.damagedQuantity) }

            pendingReturns.add(PendingReturnPreview(
                orderId = order.id,
                billNumber = order.billNumber,
                customerName = order.customerName,
                customerMobile = order.customerMobile,
                returnDate = order.returnDate,
                isOverdue = order.returnDate < todayStart,
                isDueToday = order.returnDate in todayStart until todayEnd,
                pendingItems = pendingItems
            ))
        }

        return DashboardState(
            isLoading = false,
            selectedOverviewDate = selectedOverviewDate,
            todayIncome = repository.getTotalPaymentReceived(selectedOverviewDate, selectedOverviewEnd),
            todayPendingPayment = repository.getTotalPendingBalance(selectedOverviewDate, selectedOverviewEnd),
            todayOrderCount = repository.getOrderCount(selectedOverviewDate, selectedOverviewEnd),
            itemStocks = itemStocks,
            selectedStockDate = selectedStockDate,
            activeOrderCount = repository.getActiveOrderCount(),
            returnedTodayCount = repository.getReturnedTodayCount(todayStart, todayEnd),
            pendingReturnCount = repository.getPendingReturnCount(todayEnd),
            alerts = listOf(
                DashboardAlertInfo(
                    type = DashboardAlertType.OVERDUE_RETURNS,
                    count = overdueReturnCount,
                    description = if (overdueReturnCount == 0) "No overdue returns" else "$overdueReturnCount orders overdue"
                ),
                DashboardAlertInfo(
                    type = DashboardAlertType.PENDING_PAYMENTS,
                    count = pendingPaymentsCount,
                    description = if (pendingPaymentsCount == 0) "No pending payments" else "$pendingPaymentsCount orders pending"
                ),
                DashboardAlertInfo(
                    type = DashboardAlertType.LOW_STOCK,
                    count = lowStockCount,
                    description = if (lowStockCount == 0) "Stock levels are healthy" else "$lowStockCount items low in stock"
                ),
                DashboardAlertInfo(
                    type = DashboardAlertType.TOMORROW_BOOKINGS,
                    count = tomorrowBookings.size,
                    description = if (tomorrowBookings.isEmpty()) "No bookings for tomorrow" else "${tomorrowBookings.size} deliveries - $tomorrowItemCount items"
                )
            ),
            upcomingDeliveries = upcomingDeliveries,
            pendingReturns = pendingReturns
        )
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
