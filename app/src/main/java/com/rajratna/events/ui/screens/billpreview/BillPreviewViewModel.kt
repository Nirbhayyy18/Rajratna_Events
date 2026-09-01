package com.rajratna.events.ui.screens.billpreview

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.Order
import com.rajratna.events.data.entity.OrderItem
import com.rajratna.events.util.MarathiBillGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class BillPreviewState(
    val isLoading: Boolean = true,
    val order: Order? = null,
    val orderItems: List<OrderItem> = emptyList(),
    val totalPaid: Double = 0.0,
    val billBitmap: Bitmap? = null,
    val error: String? = null
)

class BillPreviewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RajratnaApp).repository

    private val _state = MutableStateFlow(BillPreviewState())
    val state: StateFlow<BillPreviewState> = _state.asStateFlow()

    private var currentOrderId: String = ""

    fun loadBillPreview(orderId: String) {
        if (currentOrderId == orderId) return
        currentOrderId = orderId

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val order = repository.getOrderById(orderId)
                val items = repository.getOrderItemsList(orderId)
                val totalPaid = repository.getTotalPaidForOrder(orderId)

                if (order == null) {
                    _state.update { it.copy(isLoading = false, error = "Order not found") }
                    return@launch
                }

                _state.update {
                    it.copy(
                        order = order,
                        orderItems = items,
                        totalPaid = totalPaid
                    )
                }

                // Generate bitmap on default dispatcher
                val context = getApplication<RajratnaApp>()
                val bitmap = withContext(Dispatchers.Default) {
                    MarathiBillGenerator.generateBitmap(context, order, items, totalPaid)
                }

                _state.update {
                    it.copy(isLoading = false, billBitmap = bitmap)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
            }
        }
    }

    fun generateAndSharePdf(onFile: (File) -> Unit) {
        val order = _state.value.order ?: return
        val items = _state.value.orderItems
        val totalPaid = _state.value.totalPaid

        viewModelScope.launch {
            val context = getApplication<RajratnaApp>()
            val file = withContext(Dispatchers.Default) {
                MarathiBillGenerator.generatePdf(context, order, items, totalPaid)
            }
            onFile(file)
        }
    }

    fun generateAndShareImage(onFile: (File) -> Unit) {
        val order = _state.value.order ?: return
        val items = _state.value.orderItems
        val totalPaid = _state.value.totalPaid

        viewModelScope.launch {
            val context = getApplication<RajratnaApp>()
            val file = withContext(Dispatchers.Default) {
                MarathiBillGenerator.generateImage(context, order, items, totalPaid)
            }
            onFile(file)
        }
    }

    override fun onCleared() {
        super.onCleared()
        _state.value.billBitmap?.recycle()
    }
}
