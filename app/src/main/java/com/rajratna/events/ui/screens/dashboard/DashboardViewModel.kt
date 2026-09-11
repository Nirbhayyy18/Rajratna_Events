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
    val itemId: String,
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
    val orderId: String,
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
    val orderId: String,
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
        observeDataChanges()
    }

    private fun observeDataChanges() {
        viewModelScope.launch {
            repository.getAllOrders().collect {
                refreshDashboardSilently()
            }
        }
        viewModelScope.launch {
            repository.getAllPaymentsFlow().collect {
                refreshDashboardSilently()
            }
        }
        viewModelScope.launch {
            repository.getAllCustomers().collect {
                refreshDashboardSilently()
            }
        }
    }

    private fun refreshDashboardSilently() {
        viewModelScope.launch {
            try {
                val updated = buildDashboardState(
                    selectedStockDate = _state.value.selectedStockDate,
                    selectedOverviewDate = _state.value.selectedOverviewDate
                )
                _state.value = updated.copy(isLoading = false)
            } catch (e: Exception) {
                // Keep existing state on error
            }
        }
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

        // Fetch datasets once
        val allOrders = repository.getAllOrdersList()
        val allOrderItems = repository.getAllOrderItemsList()
        val allItems = repository.getAllItemsList().filter { it.isActive }
        val allCustomers = repository.getAllCustomersList()
        val customerPendingJars = allCustomers.sumOf { it.pendingReturnJars }
        val waterJar = repository.getWaterJarItem()
        val payments = repository.getPaymentsInRangeList(selectedOverviewDate, selectedOverviewEnd)

        val itemsByOrder = allOrderItems.groupBy { it.orderId }

        // Stock calculation in-memory
        val isStockToday = selectedStockDate == todayStart
        val activeOrdersForStock = allOrders.filter {
            it.orderStatus == OrderStatus.CONFIRMED || it.orderStatus == OrderStatus.DELIVERED
        }

        val itemStocks = allItems.map { item ->
            val isWaterJar = (waterJar != null && item.id == waterJar.id) ||
                    item.name.equals("Water Jar", ignoreCase = true) ||
                    item.name.contains("jar", ignoreCase = true)
            val customerExtra = if (isWaterJar) customerPendingJars else 0

            var outQty = customerExtra
            var riskQty = 0
            if (isStockToday) {
                for (order in activeOrdersForStock) {
                    val orderDeliveryStart = DateUtils.startOfDay(order.deliveryDate)
                    if (orderDeliveryStart <= todayStart) {
                        val orderItems = itemsByOrder[order.id] ?: emptyList()
                        val match = orderItems.find { it.itemId == item.id && !it.isCustomerOwned }
                        if (match != null) {
                            val pending = match.quantity - match.returnedQuantity - match.damagedQuantity
                            if (pending > 0) outQty += pending
                        }
                    }
                }
            } else {
                for (order in activeOrdersForStock) {
                    val orderItems = itemsByOrder[order.id] ?: emptyList()
                    val match = orderItems.find { it.itemId == item.id && !it.isCustomerOwned }
                    if (match != null) {
                        val pending = match.quantity - match.returnedQuantity - match.damagedQuantity
                        if (pending > 0) {
                            val orderDeliveryStart = DateUtils.startOfDay(order.deliveryDate)
                            val orderReturnStart = DateUtils.startOfDay(order.returnDate)
                            if (orderDeliveryStart <= selectedStockDate && selectedStockDate <= orderReturnStart) {
                                outQty += pending
                            } else if (orderDeliveryStart < selectedStockDate && orderReturnStart < selectedStockDate) {
                                riskQty += pending
                            }
                        }
                    }
                }
            }

            val available = maxOf(0, item.totalStock - outQty)
            ItemStockInfo(
                itemId = item.id,
                name = item.name,
                totalStock = item.totalStock,
                availableStock = available,
                outStock = outQty,
                lowStockAlert = item.lowStockAlert,
                isLowStock = item.lowStockAlert > 0 && available <= item.lowStockAlert,
                riskStock = riskQty
            )
        }

        // Pending returns in-memory
        val pendingReturnOrders = allOrders.filter {
            it.orderStatus in listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED, OrderStatus.COMPLETED) &&
            (itemsByOrder[it.id] ?: emptyList()).any { oi ->
                !oi.isCustomerOwned && oi.quantity > (oi.returnedQuantity + oi.damagedQuantity)
            }
        }.sortedBy { it.returnDate }

        val customersWithPendingJars = allCustomers.count { it.pendingReturnJars > 0 }
        val overdueReturnCount = pendingReturnOrders.count { it.returnDate < todayStart } + customersWithPendingJars
        val customersWithPendingPayment = allCustomers.count { it.pendingAmount > 0.0 }
        val pendingPaymentsCount = allOrders.count { it.orderStatus != OrderStatus.CANCELLED && it.balanceAmount > 0.0 } + customersWithPendingPayment
        val lowStockCount = itemStocks.count { it.isLowStock }
        val tomorrowBookings = allOrders.filter {
            it.orderStatus != OrderStatus.CANCELLED &&
            it.deliveryDate >= tomorrowStart &&
            it.deliveryDate < tomorrowEnd
        }

        val tomorrowItemCount = tomorrowBookings.sumOf { order ->
            (itemsByOrder[order.id] ?: emptyList())
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

        val upcomingDeliveries = upcomingOrders.map { order ->
            val itemSummary = (itemsByOrder[order.id] ?: emptyList())
                .filter { !it.isCustomerOwned }
                .take(3)
                .joinToString(", ") { "${it.itemName} x${it.quantity}" }
                .ifBlank { "No items" }

            UpcomingDeliveryInfo(
                orderId = order.id,
                deliveryDate = order.deliveryDate,
                customerName = order.customerName,
                itemSummary = itemSummary
            )
        }

        val pendingReturnsFromOrders = pendingReturnOrders
            .filter { it.returnDate <= todayEnd }
            .take(3)
            .map { order ->
                val orderItems = itemsByOrder[order.id] ?: emptyList()
                val pendingItems = orderItems
                    .filter { !it.isCustomerOwned && it.quantity > (it.returnedQuantity + it.damagedQuantity) }
                    .map { PendingItemInfo(it.itemName, it.quantity - it.returnedQuantity - it.damagedQuantity) }

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

        val customerPendingReturnPreviews = if (pendingReturnsFromOrders.size < 3) {
            allCustomers
                .filter { it.pendingReturnJars > 0 }
                .take(3 - pendingReturnsFromOrders.size)
                .map { customer ->
                    PendingReturnPreview(
                        orderId = "",
                        billNumber = 0,
                        customerName = customer.name,
                        customerMobile = customer.mobileNumber,
                        returnDate = todayStart,
                        isOverdue = true,
                        isDueToday = true,
                        pendingItems = listOf(PendingItemInfo("Water Jar", customer.pendingReturnJars))
                    )
                }
        } else emptyList()

        val pendingReturns = pendingReturnsFromOrders + customerPendingReturnPreviews

        // Metrics computed in memory
        val todayIncome = payments.sumOf { it.amount }
        val dayOrders = allOrders.filter {
            it.orderStatus != OrderStatus.CANCELLED &&
            (it.deliveryDate in selectedOverviewDate until selectedOverviewEnd ||
             it.orderDate in selectedOverviewDate until selectedOverviewEnd)
        }

        val isOverviewToday = selectedOverviewDate == todayStart
        val todayPendingPayment = if (isOverviewToday) {
            val ordersPending = allOrders.filter { it.orderStatus != OrderStatus.CANCELLED }.sumOf { it.balanceAmount }
            val customersPendingNet = allCustomers.sumOf { maxOf(0.0, it.pendingAmount - it.advanceBalance) }
            ordersPending + customersPendingNet
        } else {
            dayOrders.sumOf { it.balanceAmount }
        }

        val todayOrderCount = dayOrders.size
        val activeOrderCount = allOrders.count { it.orderStatus in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.DELIVERED) }
        val returnedTodayCount = allOrders.count { it.orderStatus == OrderStatus.COMPLETED && it.updatedAt in todayStart until todayEnd }
        val pendingReturnCount = pendingReturnOrders.count { it.returnDate <= todayEnd } + customersWithPendingJars

        return DashboardState(
            isLoading = false,
            selectedOverviewDate = selectedOverviewDate,
            todayIncome = todayIncome,
            todayPendingPayment = todayPendingPayment,
            todayOrderCount = todayOrderCount,
            itemStocks = itemStocks,
            selectedStockDate = selectedStockDate,
            activeOrderCount = activeOrderCount,
            returnedTodayCount = returnedTodayCount,
            pendingReturnCount = pendingReturnCount,
            alerts = listOf(
                DashboardAlertInfo(
                    type = DashboardAlertType.OVERDUE_RETURNS,
                    count = overdueReturnCount,
                    description = if (overdueReturnCount == 0) "No overdue returns" else "$overdueReturnCount returns pending"
                ),
                DashboardAlertInfo(
                    type = DashboardAlertType.PENDING_PAYMENTS,
                    count = pendingPaymentsCount,
                    description = if (pendingPaymentsCount == 0) "No pending payments" else "$pendingPaymentsCount dues pending"
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
}
