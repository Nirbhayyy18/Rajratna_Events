package com.rajratna.events.ui.screens.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReportPeriod(
    val label: String,
    val orderAmount: Double = 0.0,
    val itemsAmount: Double = 0.0,
    val transportRent: Double = 0.0,
    val paymentReceived: Double = 0.0,
    val pendingBalance: Double = 0.0,
    val orderCount: Int = 0
)

data class ReportsState(
    val isLoading: Boolean = true,
    val daily: ReportPeriod = ReportPeriod("Today"),
    val weekly: ReportPeriod = ReportPeriod("This Week"),
    val monthly: ReportPeriod = ReportPeriod("This Month")
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RajratnaApp).repository
    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    init { loadReports() }

    fun loadReports() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            _state.value = ReportsState(
                isLoading = false,
                daily = loadPeriod("Today", DateUtils.startOfToday(), DateUtils.endOfToday()),
                weekly = loadPeriod("This Week", DateUtils.startOfThisWeek(), DateUtils.endOfThisWeek()),
                monthly = loadPeriod("This Month", DateUtils.startOfThisMonth(), DateUtils.endOfThisMonth())
            )
        }
    }

    private suspend fun loadPeriod(label: String, start: Long, end: Long): ReportPeriod {
        return ReportPeriod(
            label = label,
            orderAmount = repository.getTotalOrderAmount(start, end),
            itemsAmount = repository.getTotalItemsAmount(start, end),
            transportRent = repository.getTotalTransportRent(start, end),
            paymentReceived = repository.getTotalPaymentReceived(start, end),
            pendingBalance = repository.getTotalPendingBalance(start, end),
            orderCount = repository.getOrderCount(start, end)
        )
    }
}
