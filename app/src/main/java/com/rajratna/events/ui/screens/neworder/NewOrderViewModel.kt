package com.rajratna.events.ui.screens.neworder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.entity.*
import com.rajratna.events.data.repository.StockDetails
import com.rajratna.events.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ItemEntry(
    val item: Item,
    val quantity: Int = 0,
    val isCustomerOwned: Boolean = false
)

data class NewOrderState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val editOrderId: String? = null,
    // Customer
    val customerName: String = "",
    val mobileNumber: String = "",
    val address: String = "",
    // Dates
    val orderDate: Long = System.currentTimeMillis(),
    val deliveryDate: Long = System.currentTimeMillis(),
    val returnDate: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
    val rentalDays: Int = 1,
    val notes: String = "",
    // Items
    val itemEntries: List<ItemEntry> = emptyList(),
    // Amounts
    val itemsTotal: Double = 0.0,
    val transportRent: String = "",
    val grandTotal: Double = 0.0,
    val advancePaid: String = "",
    val balanceAmount: Double = 0.0,
    // Stock validation
    val availableStock: Map<String, Int> = emptyMap(),
    val rangeStockDetails: Map<String, StockDetails> = emptyMap(),
    val stockErrors: Map<String, String> = emptyMap(),
    val isStockValid: Boolean = true,
    val isCheckingStock: Boolean = false,
    // Result
    val savedOrderId: String? = null,
    val errorMessage: String? = null
)

class NewOrderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as RajratnaApp).repository

    private val _state = MutableStateFlow(NewOrderState())
    val state: StateFlow<NewOrderState> = _state.asStateFlow()

    private var stockCheckJob: Job? = null

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            repository.getActiveItems().collect { items ->
                _state.value = _state.value.copy(
                    itemEntries = items.map { ItemEntry(it) }
                )
                checkStockAvailability()
            }
        }
    }

    /**
     * Load existing order for editing.
     */
    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val order = repository.getOrderById(orderId) ?: return@launch
            val orderItems = repository.getOrderItemsList(orderId)

            // Load all active items and set quantities from saved order
            repository.getActiveItems().first().let { activeItems ->
                val entries = activeItems.map { item ->
                    val savedItem = orderItems.find { it.itemId == item.id }
                    ItemEntry(
                        item = item,
                        quantity = savedItem?.quantity ?: 0,
                        isCustomerOwned = savedItem?.isCustomerOwned ?: false
                    )
                }
                // Also include items that were in the order but may now be inactive
                val missingItems = orderItems.filter { oi -> entries.none { it.item.id == oi.itemId } }
                val extraEntries = missingItems.map { oi ->
                    ItemEntry(
                        item = Item(id = oi.itemId, name = oi.itemName, ratePerDay = oi.ratePerDay),
                        quantity = oi.quantity,
                        isCustomerOwned = oi.isCustomerOwned
                    )
                }

                _state.value = _state.value.copy(
                    isLoading = false,
                    isEditMode = true,
                    editOrderId = orderId,
                    customerName = order.customerName,
                    mobileNumber = order.customerMobile,
                    address = order.customerAddress,
                    orderDate = order.orderDate,
                    deliveryDate = order.deliveryDate,
                    returnDate = order.returnDate,
                    rentalDays = order.rentalDays,
                    notes = order.notes,
                    itemEntries = entries + extraEntries,
                    transportRent = if (order.transportRent > 0) order.transportRent.toInt().toString() else "",
                    advancePaid = if (order.advancePaid > 0) order.advancePaid.toInt().toString() else ""
                )
                recalculate()
                checkStockAvailability()
            }
        }
    }

    // ── Update Functions ────────────────────────────────────

    fun updateCustomerName(name: String) {
        _state.value = _state.value.copy(customerName = name, errorMessage = null)
    }

    fun updateMobileNumber(mobile: String) {
        _state.value = _state.value.copy(mobileNumber = mobile, errorMessage = null)
    }

    fun updateAddress(address: String) {
        _state.value = _state.value.copy(address = address)
    }

    fun updateDeliveryDate(date: Long) {
        _state.value = _state.value.copy(deliveryDate = date)
        recalculateDays()
        checkStockAvailability()
    }

    fun updateReturnDate(date: Long) {
        _state.value = _state.value.copy(returnDate = date)
        recalculateDays()
        checkStockAvailability()
    }

    fun updateNotes(notes: String) {
        _state.value = _state.value.copy(notes = notes)
    }

    fun updateItemQuantity(itemId: String, quantity: Int) {
        val entries = _state.value.itemEntries.map {
            if (it.item.id == itemId) it.copy(quantity = maxOf(0, quantity)) else it
        }
        _state.value = _state.value.copy(itemEntries = entries)
        recalculate()
        validateStockLocally()
    }

    fun updateItemCustomerOwned(itemId: String, isCustomerOwned: Boolean) {
        val entries = _state.value.itemEntries.map {
            if (it.item.id == itemId) it.copy(isCustomerOwned = isCustomerOwned) else it
        }
        _state.value = _state.value.copy(itemEntries = entries)
        recalculate()
        validateStockLocally()
    }

    fun updateTransportRent(rent: String) {
        _state.value = _state.value.copy(transportRent = rent)
        recalculate()
    }

    fun updateAdvancePaid(advance: String) {
        _state.value = _state.value.copy(advancePaid = advance)
        recalculate()
    }

    private fun recalculateDays() {
        val days = DateUtils.calculateRentalDays(
            _state.value.deliveryDate,
            _state.value.returnDate
        )
        _state.value = _state.value.copy(rentalDays = days)
        recalculate()
    }

    private fun recalculate() {
        val s = _state.value
        val itemsTotal = s.itemEntries.sumOf {
            it.quantity * it.item.ratePerDay * s.rentalDays
        }
        val transport = s.transportRent.toDoubleOrNull() ?: 0.0
        val grandTotal = itemsTotal + transport
        val advance = s.advancePaid.toDoubleOrNull() ?: 0.0
        val balance = grandTotal - advance

        _state.value = s.copy(
            itemsTotal = itemsTotal,
            grandTotal = grandTotal,
            balanceAmount = balance
        )
    }

    // ── Stock Validation ────────────────────────────────────

    /**
     * Check stock availability for the selected date range.
     * Queries the database for min available across all days in range.
     */
    private fun checkStockAvailability() {
        stockCheckJob?.cancel()
        stockCheckJob = viewModelScope.launch {
            _state.value = _state.value.copy(isCheckingStock = true)
            val s = _state.value
            val details = repository.getStockDetailsForRange(
                deliveryDate = s.deliveryDate,
                returnDate = s.returnDate,
                excludeOrderId = s.editOrderId
            )
            _state.value = _state.value.copy(
                rangeStockDetails = details,
                isCheckingStock = false
            )
            validateStockLocally()
        }
    }

    /**
     * Validate entered quantities against available stock (no DB call).
     */
    private fun validateStockLocally() {
        val s = _state.value
        val errors = mutableMapOf<String, String>()

        for (entry in s.itemEntries) {
            if (entry.quantity <= 0) continue
            if (entry.isCustomerOwned) continue

            val details = s.rangeStockDetails[entry.item.id]
            if (details != null && entry.quantity > details.availableQty) {
                errors[entry.item.id] = "Only ${details.availableQty} available for selected dates"
            }
        }

        _state.value = s.copy(
            stockErrors = errors,
            isStockValid = errors.isEmpty()
        )
    }

    // ── Save Order ──────────────────────────────────────────

    fun saveOrder(status: String = OrderStatus.PENDING) {
        val s = _state.value

        // Validation
        if (s.customerName.isBlank()) {
            _state.value = s.copy(errorMessage = "Customer name is required")
            return
        }
        if (s.mobileNumber.isBlank() || s.mobileNumber.length < 10) {
            _state.value = s.copy(errorMessage = "Valid mobile number is required")
            return
        }
        if (s.itemEntries.none { it.quantity > 0 }) {
            _state.value = s.copy(errorMessage = "At least one item must be selected")
            return
        }
        if (!s.isStockValid) {
            _state.value = s.copy(errorMessage = "Fix stock errors before saving")
            return
        }

        viewModelScope.launch {
            _state.value = s.copy(isSaving = true, errorMessage = null)

            val transport = s.transportRent.toDoubleOrNull() ?: 0.0
            val advance = s.advancePaid.toDoubleOrNull() ?: 0.0
            val paid = advance
            val balance = s.grandTotal - paid
            val paymentStatus = when {
                paid >= s.grandTotal -> PaymentStatusType.PAID
                paid > 0 -> PaymentStatusType.PARTIALLY_PAID
                else -> PaymentStatusType.UNPAID
            }

            // Find or create customer
            var customer = repository.getCustomerByMobile(s.mobileNumber)
            if (customer == null) {
                val customerId = repository.insertCustomer(
                    Customer(
                        name = s.customerName,
                        mobileNumber = s.mobileNumber,
                        address = s.address
                    )
                )
                customer = Customer(id = customerId, name = s.customerName, mobileNumber = s.mobileNumber, address = s.address)
            }

            val order = Order(
                id = if (s.isEditMode) s.editOrderId!! else "",
                customerId = customer.id,
                customerName = s.customerName,
                customerMobile = s.mobileNumber,
                customerAddress = s.address,
                orderDate = s.orderDate,
                deliveryDate = s.deliveryDate,
                returnDate = s.returnDate,
                rentalDays = s.rentalDays,
                notes = s.notes,
                itemsTotal = s.itemsTotal,
                transportRent = transport,
                grandTotal = s.grandTotal,
                advancePaid = advance,
                balanceAmount = balance,
                orderStatus = status,
                paymentStatus = paymentStatus
            )

            val orderItems = s.itemEntries
                .filter { it.quantity > 0 }
                .map { entry ->
                    OrderItem(
                        orderId = "", // Will be set by repository
                        itemId = entry.item.id,
                        itemName = entry.item.name,
                        quantity = entry.quantity,
                        ratePerDay = entry.item.ratePerDay,
                        rentalDays = s.rentalDays,
                        totalAmount = entry.quantity * entry.item.ratePerDay * s.rentalDays,
                        isCustomerOwned = entry.isCustomerOwned
                    )
                }

            val orderId = if (s.isEditMode) {
                repository.updateOrder(order, orderItems)
                order.id
            } else {
                repository.createOrder(order, orderItems)
            }

            // If advance was paid, record as initial payment
            if (advance > 0 && !s.isEditMode) {
                repository.recordPayment(
                    Payment(
                        orderId = orderId,
                        customerName = s.customerName,
                        customerMobile = s.mobileNumber,
                        amount = advance,
                        paymentDate = System.currentTimeMillis(),
                        paymentMethod = PaymentMethod.CASH,
                        notes = "Advance payment"
                    )
                )
            }

            _state.value = _state.value.copy(
                isSaving = false,
                savedOrderId = orderId
            )
        }
    }
}
