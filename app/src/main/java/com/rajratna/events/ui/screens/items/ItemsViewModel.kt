package com.rajratna.events.ui.screens.items

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.Item
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ItemsState(
    val items: List<Item> = emptyList(),
    val rentedMap: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = true
)

class ItemsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RajratnaApp).repository
    private val _state = MutableStateFlow(ItemsState())
    val state: StateFlow<ItemsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllItems().collect { items ->
                // Refresh rented quantities every time items update
                val rented = try {
                    repository.getRentedQuantities()
                        .associate { it.itemId to it.totalRented }
                } catch (_: Exception) {
                    emptyMap()
                }
                _state.value = ItemsState(items, rented, false)
            }
        }
    }

    fun refreshRented() {
        viewModelScope.launch {
            val rented = try {
                repository.getRentedQuantities()
                    .associate { it.itemId to it.totalRented }
            } catch (_: Exception) {
                emptyMap()
            }
            _state.value = _state.value.copy(rentedMap = rented)
        }
    }

    fun addItem(name: String, rate: Double, totalStock: Int, lowStockAlert: Int) {
        viewModelScope.launch {
            repository.insertItem(
                Item(
                    name = name,
                    ratePerDay = rate,
                    totalStock = maxOf(0, totalStock),
                    lowStockAlert = maxOf(0, lowStockAlert)
                )
            )
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch {
            repository.updateItem(
                item.copy(
                    totalStock = maxOf(0, item.totalStock),
                    lowStockAlert = maxOf(0, item.lowStockAlert)
                )
            )
        }
    }

    fun toggleActive(id: Long, isActive: Boolean) {
        viewModelScope.launch { repository.setItemActive(id, isActive) }
    }

    /** Compute available stock for a given item */
    fun getAvailable(item: Item, rentedMap: Map<Long, Int>): Int {
        val rented = rentedMap[item.id] ?: 0
        return maxOf(0, item.totalStock - rented)
    }

    /** Get rented count for a given item */
    fun getRented(item: Item, rentedMap: Map<Long, Int>): Int {
        return rentedMap[item.id] ?: 0
    }
}
