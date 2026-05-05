package com.rajratna.events.ui.screens.payments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.Payment
import com.rajratna.events.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PaymentsState(
    val payments: List<Payment> = emptyList(),
    val isLoading: Boolean = true,
    val todayReceived: Double = 0.0,
    val todayPending: Double = 0.0,
    val weekReceived: Double = 0.0,
    val monthReceived: Double = 0.0,
    val overallPending: Double = 0.0
)

class PaymentsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RajratnaApp).repository
    private val _state = MutableStateFlow(PaymentsState())
    val state: StateFlow<PaymentsState> = _state.asStateFlow()

    init { loadPayments() }

    fun loadPayments() {
        viewModelScope.launch {
            val ts = DateUtils.startOfToday(); val te = DateUtils.endOfToday()
            val ws = DateUtils.startOfThisWeek(); val we = DateUtils.endOfThisWeek()
            val ms = DateUtils.startOfThisMonth(); val me = DateUtils.endOfThisMonth()
            val todayRec = repository.getTotalPaymentReceived(ts, te)
            val todayPend = repository.getTotalPendingBalance(ts, te)
            val weekRec = repository.getTotalPaymentReceived(ws, we)
            val monthRec = repository.getTotalPaymentReceived(ms, me)
            val overallPend = repository.getOverallPendingBalance()
            _state.value = _state.value.copy(todayReceived = todayRec, todayPending = todayPend, weekReceived = weekRec, monthReceived = monthRec, overallPending = overallPend)
            repository.getAllPayments().collect { payments ->
                _state.value = _state.value.copy(payments = payments, isLoading = false)
            }
        }
    }
}
