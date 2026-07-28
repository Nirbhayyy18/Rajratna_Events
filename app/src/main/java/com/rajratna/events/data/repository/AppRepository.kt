package com.rajratna.events.data.repository

import com.rajratna.events.data.dao.CustomerDao
import com.rajratna.events.data.dao.ItemDao
import com.rajratna.events.data.dao.OrderDao
import com.rajratna.events.data.dao.PaymentDao
import com.rajratna.events.data.entity.*
import com.rajratna.events.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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
    suspend fun getItemUsageCount(itemId: Long): Int = itemDao.getItemUsageCount(itemId)
    suspend fun deleteItem(item: Item) = itemDao.deleteItem(item)

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

    // ── Date-Wise Stock ─────────────────────────────────────

    suspend fun getAllOrdersList(): List<Order> = orderDao.getAllOrdersList()
    suspend fun getAllOrderItemsList(): List<OrderItem> = orderDao.getAllOrderItemsList()

    suspend fun getStockDetailsForDate(
        selectedDate: Long,
        excludeOrderId: Long? = null
    ): List<StockDetails> {
        val items = itemDao.getAllItemsList().filter { it.isActive }
        val activeOrders = orderDao.getAllOrdersList().filter { it.orderStatus == OrderStatus.CONFIRMED || it.orderStatus == OrderStatus.DELIVERED }
        val allOrderItems = orderDao.getAllOrderItemsList().filter { !it.isCustomerOwned }
        val itemsByOrder = allOrderItems.groupBy { it.orderId }

        val today = com.rajratna.events.util.DateUtils.startOfToday()
        val isToday = selectedDate == today

        return items.map { item ->
            if (isToday) {
                var physicalOut = 0
                activeOrders.forEach { order ->
                    val orderDeliveryStart = com.rajratna.events.util.DateUtils.startOfDay(order.deliveryDate)
                    if (order.id != excludeOrderId && orderDeliveryStart <= today) {
                        val orderItems = itemsByOrder[order.id] ?: emptyList()
                        val match = orderItems.find { it.itemId == item.id }
                        if (match != null) {
                            val pending = match.quantity - match.returnedQuantity - match.damagedQuantity
                            if (pending > 0) {
                                physicalOut += pending
                            }
                        }
                    }
                }
                val available = maxOf(0, item.totalStock - physicalOut)
                StockDetails(
                    itemId = item.id,
                    totalStock = item.totalStock,
                    outQty = physicalOut,
                    availableQty = available,
                    riskQty = 0
                )
            } else {
                var scheduledOut = 0
                var risk = 0
                activeOrders.forEach { order ->
                    if (order.id != excludeOrderId) {
                        val orderItems = itemsByOrder[order.id] ?: emptyList()
                        val match = orderItems.find { it.itemId == item.id }
                        if (match != null) {
                            val pending = match.quantity - match.returnedQuantity - match.damagedQuantity
                            if (pending > 0) {
                                val orderDeliveryStart = com.rajratna.events.util.DateUtils.startOfDay(order.deliveryDate)
                                val orderReturnStart = com.rajratna.events.util.DateUtils.startOfDay(order.returnDate)
                                if (orderDeliveryStart <= selectedDate && selectedDate <= orderReturnStart) {
                                    scheduledOut += pending
                                } else if (orderDeliveryStart < selectedDate && orderReturnStart < selectedDate) {
                                    risk += pending
                                }
                            }
                        }
                    }
                }
                val expectedAvailable = maxOf(0, item.totalStock - scheduledOut)
                StockDetails(
                    itemId = item.id,
                    totalStock = item.totalStock,
                    outQty = scheduledOut,
                    availableQty = expectedAvailable,
                    riskQty = risk
                )
            }
        }
    }

    suspend fun getStockDetailsForRange(
        deliveryDate: Long,
        returnDate: Long,
        excludeOrderId: Long? = null
    ): Map<Long, StockDetails> {
        val days = getDaysInRange(deliveryDate, returnDate)
        if (days.isEmpty()) return emptyMap()

        val items = itemDao.getAllItemsList().filter { it.isActive }
        val activeOrders = orderDao.getAllOrdersList().filter { it.orderStatus == OrderStatus.CONFIRMED || it.orderStatus == OrderStatus.DELIVERED }
        val allOrderItems = orderDao.getAllOrderItemsList().filter { !it.isCustomerOwned }
        val itemsByOrder = allOrderItems.groupBy { it.orderId }

        val today = com.rajratna.events.util.DateUtils.startOfToday()

        return items.associate { item ->
            var minAvailable = Int.MAX_VALUE
            var riskOnDelivery = 0
            var outOnDelivery = 0

            days.forEachIndexed { index, day ->
                val isToday = day == today
                var outQty = 0
                var riskQty = 0

                activeOrders.forEach { order ->
                    if (order.id != excludeOrderId) {
                        val orderItems = itemsByOrder[order.id] ?: emptyList()
                        val match = orderItems.find { it.itemId == item.id }
                        if (match != null) {
                            val pending = match.quantity - match.returnedQuantity - match.damagedQuantity
                            if (pending > 0) {
                                val orderDeliveryStart = com.rajratna.events.util.DateUtils.startOfDay(order.deliveryDate)
                                val orderReturnStart = com.rajratna.events.util.DateUtils.startOfDay(order.returnDate)
                                if (isToday) {
                                    if (orderDeliveryStart <= today) {
                                        outQty += pending
                                    }
                                } else {
                                    if (orderDeliveryStart <= day && day <= orderReturnStart) {
                                        outQty += pending
                                    } else if (orderDeliveryStart < day && orderReturnStart < day) {
                                        riskQty += pending
                                    }
                                }
                            }
                        }
                    }
                }

                val available = maxOf(0, item.totalStock - outQty)
                if (available < minAvailable) {
                    minAvailable = available
                }
                if (index == 0) {
                    riskOnDelivery = riskQty
                    outOnDelivery = outQty
                }
            }

            item.id to StockDetails(
                itemId = item.id,
                totalStock = item.totalStock,
                outQty = outOnDelivery,
                availableQty = if (minAvailable == Int.MAX_VALUE) item.totalStock else minAvailable,
                riskQty = riskOnDelivery
            )
        }
    }

    private fun getDaysInRange(startDate: Long, endDate: Long): List<Long> {
        val days = mutableListOf<Long>()
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = startDate
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        val endCal = java.util.Calendar.getInstance()
        endCal.timeInMillis = endDate
        endCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        endCal.set(java.util.Calendar.MINUTE, 0)
        endCal.set(java.util.Calendar.SECOND, 0)
        endCal.set(java.util.Calendar.MILLISECOND, 0)

        val start = cal.timeInMillis
        val end = endCal.timeInMillis
        if (start > end) {
            return listOf(start)
        }

        while (cal.timeInMillis <= endCal.timeInMillis) {
            days.add(cal.timeInMillis)
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return days
    }

    suspend fun getAvailableOnDate(
        selectedDate: Long,
        excludeOrderId: Long? = null
    ): Map<Long, Int> {
        return getStockDetailsForDate(selectedDate, excludeOrderId).associate { it.itemId to it.availableQty }
    }

    suspend fun getDateWiseRentedQuantities(selectedDate: Long) =
        orderDao.getDateWiseRentedQuantities(selectedDate)

    // ── Reports ─────────────────────────────────────────────

    suspend fun getItemWiseIncome(start: Long, end: Long) =
        orderDao.getItemWiseIncome(start, end)
    suspend fun getTotalIncomeByDelivery(start: Long, end: Long) =
        orderDao.getTotalIncomeByDelivery(start, end)
    suspend fun getOrderCountByDelivery(start: Long, end: Long) =
        orderDao.getOrderCountByDelivery(start, end)
    suspend fun getPendingBalanceByDelivery(start: Long, end: Long) =
        orderDao.getPendingBalanceByDelivery(start, end)

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

    /**
     * Record return with damaged/missing jar tracking.
     * Damaged jars reduce pending returns but do NOT return to available stock.
     * Instead, the physical totalStock of the item is decreased.
     */
    suspend fun recordReturnWithDamaged(
        orderId: Long,
        returnEntries: Map<Long, Int>,
        damagedEntries: Map<Long, Int>
    ) {
        val items = orderDao.getOrderItemsList(orderId)

        for (item in items) {
            val returnedNow = returnEntries[item.id] ?: 0
            val damagedNow = damagedEntries[item.id] ?: 0
            if (returnedNow <= 0 && damagedNow <= 0) continue

            val maxPending = item.quantity - item.returnedQuantity - item.damagedQuantity
            val totalProcessed = (returnedNow + damagedNow).coerceAtMost(maxPending)
            val actualReturned = returnedNow.coerceAtMost(totalProcessed)
            val actualDamaged = (totalProcessed - actualReturned).coerceAtMost(damagedNow)

            val newReturned = item.returnedQuantity + actualReturned
            val newDamaged = item.damagedQuantity + actualDamaged
            orderDao.updateReturnedAndDamagedQuantity(item.id, newReturned, newDamaged)

            // Reduce physical stock for damaged jars
            if (actualDamaged > 0 && !item.isCustomerOwned) {
                val dbItem = itemDao.getItemById(item.itemId)
                if (dbItem != null) {
                    val newStock = maxOf(0, dbItem.totalStock - actualDamaged)
                    itemDao.updateItem(dbItem.copy(totalStock = newStock))
                }
            }
        }

        // Check if all items are fully returned/accounted for
        if (orderDao.areAllItemsReturned(orderId)) {
            orderDao.updateOrderStatus(orderId, OrderStatus.COMPLETED)
        }
    }

    // ══════════════════════════════════════════════════════════
    // QUICK JAR ENTRY
    // ══════════════════════════════════════════════════════════

    /**
     * Find the "Water Jar" item from the database.
     */
    suspend fun getWaterJarItem(): Item? {
        return itemDao.getAllItemsList().find { it.name.equals("Water Jar", ignoreCase = true) }
    }

    /**
     * Create a quick jar entry as a normal Delivered order.
     * Returns the created order ID.
     */
    suspend fun saveQuickJarEntry(
        customer: Customer,
        quantity: Int,
        isCustomerOwned: Boolean,
        paidAmount: Double,
        deliveryDate: Long,
        waterJarItem: Item
    ): Long {
        val totalAmount = quantity * waterJarItem.ratePerDay
        val paymentStatus = when {
            paidAmount >= totalAmount -> PaymentStatusType.PAID
            paidAmount > 0 -> PaymentStatusType.PARTIALLY_PAID
            else -> PaymentStatusType.UNPAID
        }

        val order = Order(
            customerId = customer.id,
            customerName = customer.name,
            customerMobile = customer.mobileNumber,
            customerAddress = customer.address,
            orderDate = deliveryDate,
            deliveryDate = deliveryDate,
            returnDate = deliveryDate + 24 * 60 * 60 * 1000L,
            rentalDays = 1,
            notes = if (isCustomerOwned) "Quick Jar Entry (Customer Jar)" else "Quick Jar Entry",
            itemsTotal = totalAmount,
            transportRent = 0.0,
            grandTotal = totalAmount,
            advancePaid = paidAmount,
            balanceAmount = totalAmount - paidAmount,
            orderStatus = OrderStatus.DELIVERED,
            paymentStatus = paymentStatus
        )

        val orderItem = OrderItem(
            orderId = 0,
            itemId = waterJarItem.id,
            itemName = waterJarItem.name,
            quantity = quantity,
            ratePerDay = waterJarItem.ratePerDay,
            rentalDays = 1,
            totalAmount = totalAmount,
            isCustomerOwned = isCustomerOwned
        )

        val orderId = createOrder(order, listOf(orderItem))

        // Record payment if paid
        if (paidAmount > 0) {
            recordPayment(
                Payment(
                    orderId = orderId,
                    customerName = customer.name,
                    customerMobile = customer.mobileNumber,
                    amount = paidAmount,
                    paymentDate = System.currentTimeMillis(),
                    paymentMethod = PaymentMethod.CASH,
                    notes = "Quick Jar Entry payment"
                )
            )
        }

        return orderId
    }

    /**
     * Record a lump sum payment for a customer, allocating to oldest unpaid orders first.
     */
    suspend fun recordLumpSumPayment(
        customer: Customer,
        amount: Double,
        paymentMethod: String
    ) {
        // Get all unpaid/partially-paid orders for this customer, oldest first
        val customerOrders = orderDao.getAllOrdersList()
            .filter { it.customerId == customer.id && it.orderStatus != OrderStatus.CANCELLED }
            .filter { it.balanceAmount > 0 }
            .sortedBy { it.deliveryDate }

        var remaining = amount

        for (order in customerOrders) {
            if (remaining <= 0) break

            val payForThis = minOf(remaining, order.balanceAmount)
            recordPayment(
                Payment(
                    orderId = order.id,
                    customerName = customer.name,
                    customerMobile = customer.mobileNumber,
                    amount = payForThis,
                    paymentDate = System.currentTimeMillis(),
                    paymentMethod = paymentMethod,
                    notes = "Lump sum payment"
                )
            )
            remaining -= payForThis
        }
    }

    /**
     * Get customer jar summary stats for current month.
     */
    suspend fun getCustomerJarStats(customerId: Long): CustomerJarStats {
        val monthStart = DateUtils.startOfThisMonth()
        val monthEnd = DateUtils.endOfThisMonth()

        val allOrders = orderDao.getAllOrdersList()
            .filter { it.customerId == customerId && it.orderStatus != OrderStatus.CANCELLED }

        val monthOrders = allOrders.filter { it.deliveryDate in monthStart until monthEnd }

        val allOrderItems = orderDao.getAllOrderItemsList()
        val orderItemsByOrder = allOrderItems.groupBy { it.orderId }

        // This month jar count (Water Jar only)
        var thisMonthJarCount = 0
        var thisMonthJarAmount = 0.0
        for (order in monthOrders) {
            val items = orderItemsByOrder[order.id] ?: emptyList()
            for (item in items) {
                if (item.itemName.equals("Water Jar", ignoreCase = true)) {
                    thisMonthJarCount += item.quantity
                    thisMonthJarAmount += item.totalAmount
                }
            }
        }

        // Total paid for this customer
        val totalOrderAmount = allOrders.sumOf { it.grandTotal }
        val pendingBalance = allOrders.sumOf { it.balanceAmount }
        val paidAmount = totalOrderAmount - pendingBalance

        // Pending return jars (Our Jar only, across all active orders)
        var pendingReturnJars = 0
        for (order in allOrders) {
            if (order.orderStatus in listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED)) {
                val items = orderItemsByOrder[order.id] ?: emptyList()
                for (item in items) {
                    if (item.itemName.equals("Water Jar", ignoreCase = true) && !item.isCustomerOwned) {
                        val pending = item.quantity - item.returnedQuantity - item.damagedQuantity
                        if (pending > 0) pendingReturnJars += pending
                    }
                }
            }
        }

        // Last jar entry
        var lastJarQuantity = 0
        var lastJarDate = 0L
        var lastJarIsCustomerOwned = false
        val sortedOrders = allOrders.sortedByDescending { it.deliveryDate }
        for (order in sortedOrders) {
            val items = orderItemsByOrder[order.id] ?: emptyList()
            val jarItem = items.find { it.itemName.equals("Water Jar", ignoreCase = true) }
            if (jarItem != null) {
                lastJarQuantity = jarItem.quantity
                lastJarDate = order.deliveryDate
                lastJarIsCustomerOwned = jarItem.isCustomerOwned
                break
            }
        }

        // This month paid
        val thisMonthPaid = monthOrders.sumOf { it.grandTotal - it.balanceAmount }

        return CustomerJarStats(
            thisMonthJarCount = thisMonthJarCount,
            thisMonthJarAmount = thisMonthJarAmount,
            thisMonthPaid = thisMonthPaid,
            totalPaid = paidAmount,
            pendingBalance = pendingBalance,
            pendingReturnJars = pendingReturnJars,
            lastJarQuantity = lastJarQuantity,
            lastJarDate = lastJarDate,
            lastJarIsCustomerOwned = lastJarIsCustomerOwned
        )
    }

    /**
     * Get pending return jar order items for a customer (Water Jar, Our Jar only).
     */
    suspend fun getCustomerPendingJarReturns(customerId: Long): List<PendingJarReturn> {
        val allOrders = orderDao.getAllOrdersList()
            .filter { it.customerId == customerId && it.orderStatus in listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED) }
        val allOrderItems = orderDao.getAllOrderItemsList()
        val orderItemsByOrder = allOrderItems.groupBy { it.orderId }

        val results = mutableListOf<PendingJarReturn>()
        for (order in allOrders) {
            val items = orderItemsByOrder[order.id] ?: emptyList()
            for (item in items) {
                if (item.itemName.equals("Water Jar", ignoreCase = true) && !item.isCustomerOwned) {
                    val pending = item.quantity - item.returnedQuantity - item.damagedQuantity
                    if (pending > 0) {
                        results.add(PendingJarReturn(
                            orderId = order.id,
                            orderItemId = item.id,
                            deliveryDate = order.deliveryDate,
                            totalQuantity = item.quantity,
                            returnedQuantity = item.returnedQuantity,
                            damagedQuantity = item.damagedQuantity,
                            pendingQuantity = pending
                        ))
                    }
                }
            }
        }
        return results.sortedBy { it.deliveryDate }
    }

    /**
     * Get recent jar entries for a customer (for detail screen).
     */
    suspend fun getRecentJarEntries(customerId: Long, limit: Int = 20): List<JarEntry> {
        val allOrders = orderDao.getAllOrdersList()
            .filter { it.customerId == customerId && it.orderStatus != OrderStatus.CANCELLED }
            .sortedByDescending { it.deliveryDate }

        val allOrderItems = orderDao.getAllOrderItemsList()
        val orderItemsByOrder = allOrderItems.groupBy { it.orderId }

        val results = mutableListOf<JarEntry>()
        for (order in allOrders) {
            val items = orderItemsByOrder[order.id] ?: emptyList()
            val jarItem = items.find { it.itemName.equals("Water Jar", ignoreCase = true) }
            if (jarItem != null) {
                results.add(JarEntry(
                    orderId = order.id,
                    date = order.deliveryDate,
                    quantity = jarItem.quantity,
                    amount = jarItem.totalAmount,
                    isCustomerOwned = jarItem.isCustomerOwned,
                    paymentStatus = order.paymentStatus
                ))
                if (results.size >= limit) break
            }
        }
        return results
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

data class StockDetails(
    val itemId: Long,
    val totalStock: Int,
    val outQty: Int,
    val availableQty: Int,
    val riskQty: Int
)

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

/**
 * Customer jar summary stats.
 */
data class CustomerJarStats(
    val thisMonthJarCount: Int = 0,
    val thisMonthJarAmount: Double = 0.0,
    val thisMonthPaid: Double = 0.0,
    val totalPaid: Double = 0.0,
    val pendingBalance: Double = 0.0,
    val pendingReturnJars: Int = 0,
    val lastJarQuantity: Int = 0,
    val lastJarDate: Long = 0L,
    val lastJarIsCustomerOwned: Boolean = false
)

/**
 * Pending jar return entry for a customer.
 */
data class PendingJarReturn(
    val orderId: Long,
    val orderItemId: Long,
    val deliveryDate: Long,
    val totalQuantity: Int,
    val returnedQuantity: Int,
    val damagedQuantity: Int,
    val pendingQuantity: Int
)

/**
 * Jar entry for recent history display.
 */
data class JarEntry(
    val orderId: Long,
    val date: Long,
    val quantity: Int,
    val amount: Double,
    val isCustomerOwned: Boolean,
    val paymentStatus: String
)
