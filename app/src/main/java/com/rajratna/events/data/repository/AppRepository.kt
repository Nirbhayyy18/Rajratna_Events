package com.rajratna.events.data.repository

import com.rajratna.events.data.dao.CustomerDao
import com.rajratna.events.data.dao.ItemDao
import com.rajratna.events.data.dao.OrderDao
import com.rajratna.events.data.dao.PaymentDao
import com.rajratna.events.data.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * Single repository that wraps all DAOs.
 * Provides a clean API for ViewModels.
 */
class AppRepository(
    private val itemDao: ItemDao,
    private val customerDao: CustomerDao,
    private val orderDao: OrderDao,
    private val paymentDao: PaymentDao
) {

    // ══════════════════════════════════════════════════════════
    // ITEMS
    // ══════════════════════════════════════════════════════════

    fun getAllItems(): Flow<List<Item>> = itemDao.getAllItems()
    fun getActiveItems(): Flow<List<Item>> = itemDao.getActiveItems()
    suspend fun getItemById(id: Long): Item? = itemDao.getItemById(id)
    suspend fun insertItem(item: Item): Long = itemDao.insertItem(item)
    suspend fun updateItem(item: Item) = itemDao.updateItem(item)
    suspend fun setItemActive(id: Long, isActive: Boolean) = itemDao.setItemActive(id, isActive)
    suspend fun getRentedQuantities() = orderDao.getRentedQuantities()
    suspend fun getAllItemsList() = itemDao.getAllItemsList()

    // ══════════════════════════════════════════════════════════
    // CUSTOMERS
    // ══════════════════════════════════════════════════════════

    fun getAllCustomers(): Flow<List<Customer>> = customerDao.getAllCustomers()
    suspend fun getCustomerById(id: Long): Customer? = customerDao.getCustomerById(id)
    fun searchCustomers(query: String): Flow<List<Customer>> = customerDao.searchCustomers(query)
    suspend fun getCustomerByMobile(mobile: String): Customer? = customerDao.getCustomerByMobile(mobile)
    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)

    // ══════════════════════════════════════════════════════════
    // ORDERS
    // ══════════════════════════════════════════════════════════

    fun getAllOrders(): Flow<List<Order>> = orderDao.getAllOrders()
    suspend fun getOrderById(id: Long): Order? = orderDao.getOrderById(id)
    fun getOrderByIdFlow(id: Long): Flow<Order?> = orderDao.getOrderByIdFlow(id)
    fun getOrdersByDate(startOfDay: Long, endOfDay: Long): Flow<List<Order>> = orderDao.getOrdersByDate(startOfDay, endOfDay)
    fun getOrdersByDeliveryDate(startOfDay: Long, endOfDay: Long): Flow<List<Order>> = orderDao.getOrdersByDeliveryDate(startOfDay, endOfDay)
    fun getOrdersInRange(start: Long, end: Long): Flow<List<Order>> = orderDao.getOrdersInRange(start, end)
    fun getOrdersByStatus(status: String): Flow<List<Order>> = orderDao.getOrdersByStatus(status)
    fun getOrdersByPaymentStatus(status: String): Flow<List<Order>> = orderDao.getOrdersByPaymentStatus(status)
    fun getOrdersByCustomer(customerId: Long): Flow<List<Order>> = orderDao.getOrdersByCustomer(customerId)
    fun searchOrders(query: String): Flow<List<Order>> = orderDao.searchOrders(query)

    suspend fun createOrder(order: Order, items: List<OrderItem>): Long {
        val billNumber = orderDao.getMaxBillNumber() + 1
        val orderId = orderDao.insertOrder(order.copy(billNumber = billNumber))
        val orderItems = items.map { it.copy(orderId = orderId) }
        orderDao.insertOrderItems(orderItems)
        return orderId
    }

    suspend fun updateOrder(order: Order, items: List<OrderItem>) {
        orderDao.updateOrder(order)
        orderDao.deleteOrderItems(order.id)
        orderDao.insertOrderItems(items.map { it.copy(orderId = order.id) })
    }

    suspend fun updateOrderStatus(orderId: Long, status: String) =
        orderDao.updateOrderStatus(orderId, status)

    // ── Dashboard Aggregations ──────────────────────────────

    suspend fun getOrderCount(start: Long, end: Long): Int = orderDao.getOrderCount(start, end)
    suspend fun getOrderCountByStatus(status: String, start: Long, end: Long): Int =
        orderDao.getOrderCountByStatus(status, start, end)
    suspend fun getTotalOrderAmount(start: Long, end: Long): Double = orderDao.getTotalOrderAmount(start, end)
    suspend fun getTotalItemsAmount(start: Long, end: Long): Double = orderDao.getTotalItemsAmount(start, end)
    suspend fun getTotalTransportRent(start: Long, end: Long): Double = orderDao.getTotalTransportRent(start, end)
    suspend fun getTotalPendingBalance(start: Long, end: Long): Double = orderDao.getTotalPendingBalance(start, end)
    suspend fun getOverallPendingBalance(): Double = orderDao.getOverallPendingBalance()
    suspend fun getActiveOrderCount(): Int = orderDao.getActiveOrderCount()

    // ── Order Items ─────────────────────────────────────────

    fun getOrderItems(orderId: Long): Flow<List<OrderItem>> = orderDao.getOrderItems(orderId)
    suspend fun getOrderItemsList(orderId: Long): List<OrderItem> = orderDao.getOrderItemsList(orderId)

    // ── Dashboard Helpers ───────────────────────────────────

    suspend fun getPendingReturnOrders(endOfToday: Long, limit: Int = 3) =
        orderDao.getPendingReturnOrders(endOfToday, limit)
    suspend fun getPendingReturnCount(endOfToday: Long) =
        orderDao.getPendingReturnCount(endOfToday)
    suspend fun getReturnedTodayCount(start: Long, end: Long) =
        orderDao.getReturnedTodayCount(start, end)
    suspend fun getTodayActiveCount(start: Long, end: Long) =
        orderDao.getTodayActiveCount(start, end)
    suspend fun getTodayReturnedCount(start: Long, end: Long) =
        orderDao.getTodayReturnedCount(start, end)
    suspend fun getFirstOrderItem(orderId: Long) =
        orderDao.getFirstOrderItem(orderId)

    // ══════════════════════════════════════════════════════════
    // RETURNS
    // ══════════════════════════════════════════════════════════

    /**
     * Get all orders with pending return items.
     */
    suspend fun getOrdersWithPendingReturns(): List<Order> =
        orderDao.getOrdersWithPendingReturns()

    /**
     * Get completed/returned orders.
     */
    suspend fun getReturnedOrders(): List<Order> =
        orderDao.getReturnedOrders()

    /**
     * Get returned orders within a date range.
     */
    suspend fun getReturnedOrdersInRange(start: Long, end: Long): List<Order> =
        orderDao.getReturnedOrdersInRange(start, end)

    /**
     * Record return of items for an order.
     * Updates each order item's returnedQuantity.
     * If all items are fully returned, marks order as Completed.
     *
     * @param returnEntries map of orderItemId to quantity being returned now
     */
    suspend fun recordReturn(orderId: Long, returnEntries: Map<Long, Int>) {
        // Update each order item's returned quantity
        for ((orderItemId, returnedNow) in returnEntries) {
            if (returnedNow <= 0) continue
            val items = orderDao.getOrderItemsList(orderId)
            val item = items.find { it.id == orderItemId } ?: continue
            val newReturned = (item.returnedQuantity + returnedNow).coerceAtMost(item.quantity)
            orderDao.updateReturnedQuantity(orderItemId, newReturned)
        }

        // Check if all items are fully returned
        if (orderDao.areAllItemsReturned(orderId)) {
            orderDao.updateOrderStatus(orderId, OrderStatus.COMPLETED)
        }
    }

    // ══════════════════════════════════════════════════════════
    // PAYMENTS
    // ══════════════════════════════════════════════════════════

    fun getAllPayments(): Flow<List<Payment>> = paymentDao.getAllPayments()
    fun getPaymentsByOrder(orderId: Long): Flow<List<Payment>> = paymentDao.getPaymentsByOrder(orderId)
    fun getPaymentsInRange(start: Long, end: Long): Flow<List<Payment>> = paymentDao.getPaymentsInRange(start, end)
    suspend fun getTotalPaidForOrder(orderId: Long): Double = paymentDao.getTotalPaidForOrder(orderId)
    suspend fun getTotalPaymentReceived(start: Long, end: Long): Double = paymentDao.getTotalPaymentReceived(start, end)
    suspend fun getOverallTotalReceived(): Double = paymentDao.getOverallTotalReceived()

    suspend fun recordPayment(payment: Payment) {
        paymentDao.insertPayment(payment)
        // Recalculate and update order payment status
        val totalPaid = paymentDao.getTotalPaidForOrder(payment.orderId)
        orderDao.updatePaymentInfo(payment.orderId, totalPaid)
    }

    // ══════════════════════════════════════════════════════════
    // BACKUP / RESTORE
    // ══════════════════════════════════════════════════════════

    suspend fun getAllDataForBackup(): BackupData {
        return BackupData(
            items = itemDao.getAllItemsList(),
            customers = customerDao.getAllCustomersList(),
            orders = orderDao.getAllOrdersList(),
            orderItems = orderDao.getAllOrderItemsList(),
            payments = paymentDao.getAllPaymentsList()
        )
    }

    suspend fun restoreFromBackup(data: BackupData) {
        // Insert all data - using REPLACE strategy
        data.items.forEach { itemDao.insertItem(it) }
        data.customers.forEach { customerDao.insertCustomer(it) }
        data.orders.forEach { orderDao.insertOrder(it) }
        orderDao.insertOrderItems(data.orderItems)
        data.payments.forEach { paymentDao.insertPayment(it) }
    }
}

/**
 * Container for all app data, used in backup/restore.
 */
data class BackupData(
    val items: List<Item> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val orders: List<Order> = emptyList(),
    val orderItems: List<OrderItem> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val backupTimestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0"
)
