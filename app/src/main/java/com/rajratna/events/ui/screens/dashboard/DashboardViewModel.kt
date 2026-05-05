package com.rajratna.events.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.OrderStatus
import com.rajratna.events.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardState(
    val isLoading: Boolean = true,
    // Today
    val todayOrderAmount: Double = 0.0,
    val todayPaymentReceived: Double = 0.0,
    val todayPendingBalance: Double = 0.0,
    val todayTransportRent: Double = 0.0,
    val todayOrderCount: Int = 0,
    // This Week
    val weekOrderAmount: Double = 0.0,
    val weekPaymentReceived: Double = 0.0,
    // This Month
    val monthOrderAmount: Double = 0.0,
    val monthPaymentReceived: Double = 0.0,
    // Status Counts (today)
    val pendingCount: Int = 0,
    val confirmedCount: Int = 0,
    val deliveredCount: Int = 0,
    val completedCount: Int = 0,
    val cancelledCount: Int = 0
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
            val weekStart = DateUtils.startOfThisWeek()
            val weekEnd = DateUtils.endOfThisWeek()
            val monthStart = DateUtils.startOfThisMonth()
            val monthEnd = DateUtils.endOfThisMonth()

            _state.value = DashboardState(
                isLoading = false,
                // Today
                todayOrderAmount = repository.getTotalOrderAmount(todayStart, todayEnd),
                todayPaymentReceived = repository.getTotalPaymentReceived(todayStart, todayEnd),
                todayPendingBalance = repository.getTotalPendingBalance(todayStart, todayEnd),
                todayTransportRent = repository.getTotalTransportRent(todayStart, todayEnd),
                todayOrderCount = repository.getOrderCount(todayStart, todayEnd),
                // This Week
                weekOrderAmount = repository.getTotalOrderAmount(weekStart, weekEnd),
                weekPaymentReceived = repository.getTotalPaymentReceived(weekStart, weekEnd),
                // This Month
                monthOrderAmount = repository.getTotalOrderAmount(monthStart, monthEnd),
                monthPaymentReceived = repository.getTotalPaymentReceived(monthStart, monthEnd),
                // Status Counts
                pendingCount = repository.getOrderCountByStatus(OrderStatus.PENDING, todayStart, todayEnd),
                confirmedCount = repository.getOrderCountByStatus(OrderStatus.CONFIRMED, todayStart, todayEnd),
                deliveredCount = repository.getOrderCountByStatus(OrderStatus.DELIVERED, todayStart, todayEnd),
                completedCount = repository.getOrderCountByStatus(OrderStatus.COMPLETED, todayStart, todayEnd),
                cancelledCount = repository.getOrderCountByStatus(OrderStatus.CANCELLED, todayStart, todayEnd)
            )
        }
    }
}
