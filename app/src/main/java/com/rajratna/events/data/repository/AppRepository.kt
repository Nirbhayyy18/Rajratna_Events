package com.rajratna.events.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rajratna.events.data.entity.*
import com.rajratna.events.util.DateUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Single repository that wraps all Firestore operations.
 * Provides a clean API for ViewModels — same surface as the old Room-based AppRepository.
 *
 * Firestore structure:
 *   /items/{itemId}
 *   /customers/{customerId}
 *   /orders/{orderId}
 *   /orders/{orderId}/order_items/{orderItemId}
 *   /orders/{orderId}/payments/{paymentId}
 *   /counters/billNumber   (stores { value: Int } for auto-incrementing bill numbers)
 */
class AppRepository(
    private val db: FirebaseFirestore
) {

    // Collection references
    private val itemsCol = db.collection("items")
    private val customersCol = db.collection("customers")
    private val ordersCol = db.collection("orders")
    private val countersCol = db.collection("counters")

    // ══════════════════════════════════════════════════════════
    // ITEMS
    // ══════════════════════════════════════════════════════════

    fun getAllItems(): Flow<List<Item>> = callbackFlow {
        val registration = itemsCol
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("AppRepository", "getAllItems error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(Item::class.java) ?: emptyList()
                android.util.Log.d("AppRepository", "getAllItems: received ${items.size} items")
                trySend(items.sortedBy { it.name.lowercase() })
            }
        awaitClose { registration.remove() }
    }

    fun getActiveItems(): Flow<List<Item>> = callbackFlow {
        val registration = itemsCol
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(Item::class.java) ?: emptyList()
                trySend(items.filter { it.isActive }.sortedBy { it.name.lowercase() })
            }
        awaitClose { registration.remove() }
    }

    suspend fun getItemById(id: String): Item? {
        return try {
            val doc = itemsCol.document(id).get().await()
            doc.toObject(Item::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun insertItem(item: Item): String {
        val docRef = itemsCol.document()
        val newItem = item.copy(id = docRef.id)
        docRef.set(newItem).await()
        return docRef.id
    }

    suspend fun updateItem(item: Item) {
        if (item.id.isNotEmpty()) {
            itemsCol.document(item.id).set(item).await()
        }
    }

    suspend fun setItemActive(id: String, isActive: Boolean) {
        itemsCol.document(id).update("active", isActive).await()
    }

    suspend fun getAllItemsList(): List<Item> {
        return try {
            val snapshot = itemsCol.whereEqualTo("deleted", false).get().await()
            snapshot.toObjects(Item::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getItemUsageCount(itemId: String): Int {
        // Count how many order_items reference this itemId across all orders
        val allOrderItems = getAllOrderItemsList()
        return allOrderItems.count { it.itemId == itemId }
    }

    suspend fun deleteItem(item: Item) {
        // Soft delete
        if (item.id.isNotEmpty()) {
            itemsCol.document(item.id).update("deleted", true).await()
        }
    }

    // ══════════════════════════════════════════════════════════
    // CUSTOMERS
    // ══════════════════════════════════════════════════════════

    fun getAllCustomers(): Flow<List<Customer>> = callbackFlow {
        val registration = customersCol
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val customers = snapshot?.toObjects(Customer::class.java)
                    ?.filter { !it.isDeleted } ?: emptyList()
                trySend(customers.sortedBy { it.name.lowercase() })
            }
        awaitClose { registration.remove() }
    }

    suspend fun getAllCustomersList(): List<Customer> {
        return try {
            val snapshot = customersCol.get().await()
            snapshot.toObjects(Customer::class.java).filter { !it.isDeleted }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCustomerById(id: String): Customer? {
        return try {
            val doc = customersCol.document(id).get().await()
            val cust = doc.toObject(Customer::class.java)
            if (cust?.isDeleted == true) null else cust
        } catch (e: Exception) {
            null
        }
    }

    fun searchCustomers(query: String): Flow<List<Customer>> = callbackFlow {
        val registration = customersCol
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val all = snapshot?.toObjects(Customer::class.java)
                    ?.filter { !it.isDeleted } ?: emptyList()
                val lowerQuery = query.trim().lowercase()
                val filtered = all.filter {
                    it.name.lowercase().contains(lowerQuery) ||
                    (it.mobileNumber.isNotBlank() && it.mobileNumber.contains(query.trim()))
                }.sortedBy { it.name }
                trySend(filtered)
            }
        awaitClose { registration.remove() }
    }

    suspend fun getCustomerByMobile(mobile: String): Customer? {
        if (mobile.isBlank()) return null
        val cleanMobile = mobile.trim()
        return try {
            val snapshot = customersCol.get().await()
            snapshot.toObjects(Customer::class.java)
                .firstOrNull { !it.isDeleted && it.mobileNumber.trim() == cleanMobile }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCustomerByName(name: String): Customer? {
        if (name.isBlank()) return null
        val cleanName = name.trim().lowercase()
        return try {
            val snapshot = customersCol.get().await()
            snapshot.toObjects(Customer::class.java)
                .firstOrNull { !it.isDeleted && it.name.trim().lowercase() == cleanName }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun insertCustomer(customer: Customer): String {
        val docRef = customersCol.document()
        val newCustomer = customer.copy(id = docRef.id, isDeleted = false)
        docRef.set(newCustomer).await()
        return docRef.id
    }

    suspend fun updateCustomer(customer: Customer) {
        if (customer.id.isNotEmpty()) {
            customersCol.document(customer.id).set(customer).await()
        }
    }

    suspend fun deleteCustomer(customerId: String) {
        if (customerId.isNotEmpty()) {
            customersCol.document(customerId).update("deleted", true).await()
        }
    }

    // ══════════════════════════════════════════════════════════
    // ORDERS
    // ══════════════════════════════════════════════════════════

    fun getAllOrders(): Flow<List<Order>> = callbackFlow {
        val registration = ordersCol
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orders = snapshot?.toObjects(Order::class.java) ?: emptyList()
                trySend(orders.sortedByDescending { it.createdAt })
            }
        awaitClose { registration.remove() }
    }

    suspend fun getOrderById(id: String): Order? {
        return try {
            val doc = ordersCol.document(id).get().await()
            doc.toObject(Order::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getOrderByIdFlow(id: String): Flow<Order?> = callbackFlow {
        val registration = ordersCol.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val order = snapshot?.toObject(Order::class.java)
                trySend(order)
            }
        awaitClose { registration.remove() }
    }

    fun getOrdersByDate(startOfDay: Long, endOfDay: Long): Flow<List<Order>> = callbackFlow {
        val registration = ordersCol
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orders = snapshot?.toObjects(Order::class.java) ?: emptyList()
                trySend(orders.filter { it.orderDate >= startOfDay && it.orderDate < endOfDay }
                    .sortedByDescending { it.createdAt })
            }
        awaitClose { registration.remove() }
    }

    fun getOrdersByDeliveryDate(startOfDay: Long, endOfDay: Long): Flow<List<Order>> = callbackFlow {
        val registration = ordersCol
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orders = snapshot?.toObjects(Order::class.java) ?: emptyList()
                trySend(orders.filter { it.deliveryDate >= startOfDay && it.deliveryDate < endOfDay }
                    .sortedBy { it.deliveryDate })
            }
        awaitClose { registration.remove() }
    }

    fun getOrdersInRange(start: Long, end: Long): Flow<List<Order>> = callbackFlow {
        val registration = ordersCol
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orders = snapshot?.toObjects(Order::class.java) ?: emptyList()
                trySend(orders.filter { it.orderDate >= start && it.orderDate < end }
                    .sortedByDescending { it.createdAt })
            }
        awaitClose { registration.remove() }
    }

    fun getOrdersByStatus(status: String): Flow<List<Order>> = callbackFlow {
        val registration = ordersCol
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orders = snapshot?.toObjects(Order::class.java) ?: emptyList()
                trySend(orders.filter { it.orderStatus == status }
                    .sortedByDescending { it.createdAt })
            }
        awaitClose { registration.remove() }
    }

    fun getOrdersByPaymentStatus(status: String): Flow<List<Order>> = callbackFlow {
        val registration = ordersCol
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orders = snapshot?.toObjects(Order::class.java) ?: emptyList()
                trySend(orders.filter { it.paymentStatus == status }
                    .sortedByDescending { it.createdAt })
            }
        awaitClose { registration.remove() }
    }

    fun getOrdersByCustomer(customerId: String): Flow<List<Order>> = callbackFlow {
        val registration = ordersCol
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orders = snapshot?.toObjects(Order::class.java) ?: emptyList()
                trySend(orders.filter { it.customerId == customerId }
                    .sortedByDescending { it.createdAt })
            }
        awaitClose { registration.remove() }
    }

    fun searchOrders(query: String): Flow<List<Order>> = callbackFlow {
        // Firestore doesn't support LIKE queries — fetch all and filter client-side.
        val registration = ordersCol
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val all = snapshot?.toObjects(Order::class.java) ?: emptyList()
                val lowerQuery = query.lowercase()
                val filtered = all.filter {
                    it.customerName.lowercase().contains(lowerQuery) ||
                    it.customerMobile.contains(query) ||
                    it.billNumber.toString().contains(query)
                }.sortedByDescending { it.createdAt }
                trySend(filtered)
            }
        awaitClose { registration.remove() }
    }

    suspend fun createOrder(order: Order, items: List<OrderItem>): String {
        // Get next bill number using a Firestore transaction
        val billNumber = db.runTransaction { transaction ->
            val counterRef = countersCol.document("billNumber")
            val snap = transaction.get(counterRef)
            val currentVal = snap.getLong("value") ?: 0L
            val nextVal = currentVal + 1
            transaction.set(counterRef, mapOf("value" to nextVal))
            nextVal.toInt()
        }.await()

        val docRef = ordersCol.document()
        val orderId = docRef.id
        val newOrder = order.copy(id = orderId, billNumber = billNumber)
        docRef.set(newOrder).await()

        // Insert order items as subcollection
        val batch = db.batch()
        for (item in items) {
            val itemRef = ordersCol.document(orderId).collection("order_items").document()
            batch.set(itemRef, item.copy(id = itemRef.id, orderId = orderId))
        }
        batch.commit().await()

        return orderId
    }

    suspend fun updateOrder(order: Order, items: List<OrderItem>) {
        val orderId = order.id
        ordersCol.document(orderId).set(order.copy(updatedAt = System.currentTimeMillis())).await()

        // Delete existing order items
        val existingItems = ordersCol.document(orderId).collection("order_items").get().await()
        val deleteBatch = db.batch()
        for (doc in existingItems.documents) {
            deleteBatch.delete(doc.reference)
        }
        deleteBatch.commit().await()

        // Insert new order items
        val insertBatch = db.batch()
        for (item in items) {
            val itemRef = ordersCol.document(orderId).collection("order_items").document()
            insertBatch.set(itemRef, item.copy(id = itemRef.id, orderId = orderId))
        }
        insertBatch.commit().await()
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        ordersCol.document(orderId).update(
            mapOf(
                "orderStatus" to status,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun deleteOrder(orderId: String) {
        if (orderId.isNotEmpty()) {
            ordersCol.document(orderId).update(
                mapOf(
                    "deleted" to true,
                    "orderStatus" to OrderStatus.CANCELLED,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    // ── Dashboard Aggregations ──────────────────────────────
    // Firestore doesn't support server-side aggregations like SUM/COUNT with filters,
    // so we fetch the relevant documents and compute client-side.

    suspend fun getOrderCount(start: Long, end: Long): Int {
        val snapshot = ordersCol
            .whereEqualTo("deleted", false)
            .get().await()
        val orders = snapshot.toObjects(Order::class.java)
        return orders.count { it.orderDate >= start && it.orderDate < end }
    }

    suspend fun getOrderCountByStatus(status: String, start: Long, end: Long): Int {
        val snapshot = ordersCol
            .whereEqualTo("deleted", false)
            .get().await()
        val orders = snapshot.toObjects(Order::class.java)
        return orders.count { it.orderStatus == status && it.orderDate >= start && it.orderDate < end }
    }

    suspend fun getTotalOrderAmount(start: Long, end: Long): Double {
        val orders = getOrdersInRangeList(start, end)
        return orders.filter { it.orderStatus != OrderStatus.CANCELLED }.sumOf { it.grandTotal }
    }

    suspend fun getTotalItemsAmount(start: Long, end: Long): Double {
        val orders = getOrdersInRangeList(start, end)
        return orders.filter { it.orderStatus != OrderStatus.CANCELLED }.sumOf { it.itemsTotal }
    }

    suspend fun getTotalTransportRent(start: Long, end: Long): Double {
        val orders = getOrdersInRangeList(start, end)
        return orders.filter { it.orderStatus != OrderStatus.CANCELLED }.sumOf { it.transportRent }
    }

    suspend fun getTotalPendingBalance(start: Long, end: Long): Double {
        val orders = getOrdersInRangeList(start, end)
        return orders.filter { it.orderStatus != OrderStatus.CANCELLED }.sumOf { it.balanceAmount }
    }

    suspend fun getOverallPendingBalance(): Double {
        val snapshot = ordersCol
            .whereEqualTo("deleted", false)
            .get().await()
        val orders = snapshot.toObjects(Order::class.java)
        val ordersPending = orders.filter { it.orderStatus != OrderStatus.CANCELLED }.sumOf { it.balanceAmount }

        val custSnapshot = customersCol
            .whereEqualTo("deleted", false)
            .get().await()
        val customers = custSnapshot.toObjects(Customer::class.java)
        val customersPendingNet = customers.sumOf { it.pendingAmount - it.advanceBalance }

        return maxOf(0.0, ordersPending + customersPendingNet)
    }

    suspend fun getActiveOrderCount(): Int {
        val snapshot = ordersCol
            .whereEqualTo("deleted", false)
            .get().await()
        val orders = snapshot.toObjects(Order::class.java)
        return orders.count { it.orderStatus in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.DELIVERED) }
    }

    // Helper: get orders list in a date range by orderDate
    private suspend fun getOrdersInRangeList(start: Long, end: Long): List<Order> {
        val snapshot = ordersCol
            .whereEqualTo("deleted", false)
            .get().await()
        return snapshot.toObjects(Order::class.java)
            .filter { it.orderDate >= start && it.orderDate < end }
    }

    // ── Order Items ─────────────────────────────────────────

    fun getOrderItems(orderId: String): Flow<List<OrderItem>> = callbackFlow {
        val registration = ordersCol.document(orderId).collection("order_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(OrderItem::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    suspend fun getOrderItemsList(orderId: String): List<OrderItem> {
        return try {
            val snapshot = ordersCol.document(orderId).collection("order_items").get().await()
            snapshot.toObjects(OrderItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Dashboard Helpers ───────────────────────────────────

    suspend fun getPendingReturnOrders(endOfToday: Long, limit: Int = 3): List<Order> {
        val orders = getOrdersWithPendingReturns()
        return orders
            .filter { it.returnDate <= endOfToday }
            .sortedBy { it.returnDate }
            .take(limit)
    }

    suspend fun getPendingReturnCount(endOfToday: Long): Int {
        val orders = getOrdersWithPendingReturns()
        return orders.count { it.returnDate <= endOfToday }
    }

    suspend fun getReturnedTodayCount(start: Long, end: Long): Int {
        val snapshot = ordersCol
            .whereEqualTo("deleted", false)
            .get().await()
        val orders = snapshot.toObjects(Order::class.java)
        return orders.count { it.orderStatus == OrderStatus.COMPLETED && it.updatedAt >= start && it.updatedAt < end }
    }

    suspend fun getTodayActiveCount(start: Long, end: Long): Int {
        val snapshot = ordersCol
            .whereEqualTo("deleted", false)
            .get().await()
        val orders = snapshot.toObjects(Order::class.java)
        return orders.count {
            it.orderDate >= start && it.orderDate < end &&
            it.orderStatus in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.DELIVERED)
        }
    }

    suspend fun getTodayReturnedCount(start: Long, end: Long): Int {
        val snapshot = ordersCol
            .whereEqualTo("deleted", false)
            .get().await()
        val orders = snapshot.toObjects(Order::class.java)
        return orders.count { it.orderStatus == OrderStatus.COMPLETED && it.orderDate >= start && it.orderDate < end }
    }

    suspend fun getFirstOrderItem(orderId: String): OrderItem? {
        return try {
            val snapshot = ordersCol.document(orderId).collection("order_items")
                .limit(1).get().await()
            snapshot.toObjects(OrderItem::class.java).firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    // ── Rented Quantities (for stock tracking) ──────────────

    suspend fun getRentedQuantities(): List<RentedQuantity> {
        val allOrders = getAllOrdersList()
        val activeOrderIds = allOrders.filter {
            it.orderStatus in listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED)
        }.map { it.id }.toSet()

        val allCustomers = getAllCustomersList()
        val customerPendingJars = allCustomers.sumOf { it.pendingReturnJars }
        val waterJar = getWaterJarItem()

        val allOrderItems = if (activeOrderIds.isNotEmpty()) {
            getAllOrderItemsList().filter { it.orderId in activeOrderIds }
        } else emptyList()

        val orderRentedMap = allOrderItems
            .filter { !it.isCustomerOwned && (it.quantity - it.returnedQuantity - it.damagedQuantity) > 0 }
            .groupBy { it.itemId }
            .mapValues { (_, items) -> items.sumOf { it.quantity - it.returnedQuantity - it.damagedQuantity } }
            .toMutableMap()

        if (waterJar != null && customerPendingJars > 0) {
            orderRentedMap[waterJar.id] = (orderRentedMap[waterJar.id] ?: 0) + customerPendingJars
        }

        return orderRentedMap.map { (itemId, totalRented) ->
            RentedQuantity(
                itemId = itemId,
                totalRented = totalRented
            )
        }
    }

    // ── Date-Wise Stock ─────────────────────────────────────

    suspend fun getAllOrdersList(): List<Order> {
        return try {
            val snapshot = ordersCol.whereEqualTo("deleted", false).get().await()
            snapshot.toObjects(Order::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllOrderItemsList(): List<OrderItem> {
        return try {
            val snapshot = db.collectionGroup("order_items").get().await()
            snapshot.toObjects(OrderItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStockDetailsForDate(
        selectedDate: Long,
        excludeOrderId: String? = null
    ): List<StockDetails> {
        val items = getAllItemsList().filter { it.isActive }
        val activeOrders = getAllOrdersList().filter {
            it.orderStatus == OrderStatus.CONFIRMED || it.orderStatus == OrderStatus.DELIVERED
        }
        val allOrderItems = getAllOrderItemsList().filter { !it.isCustomerOwned }
        val itemsByOrder = allOrderItems.groupBy { it.orderId }

        val allCustomers = getAllCustomersList()
        val customerPendingJars = allCustomers.sumOf { it.pendingReturnJars }
        val waterJar = getWaterJarItem()

        val today = DateUtils.startOfToday()
        val isToday = selectedDate == today

        return items.map { item ->
            val isWaterJar = (waterJar != null && item.id == waterJar.id) ||
                    item.name.equals("Water Jar", ignoreCase = true) ||
                    item.name.contains("jar", ignoreCase = true)
            val customerExtra = if (isWaterJar) customerPendingJars else 0

            if (isToday) {
                var physicalOut = customerExtra
                activeOrders.forEach { order ->
                    val orderDeliveryStart = DateUtils.startOfDay(order.deliveryDate)
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
                var scheduledOut = customerExtra
                var risk = 0
                activeOrders.forEach { order ->
                    if (order.id != excludeOrderId) {
                        val orderItems = itemsByOrder[order.id] ?: emptyList()
                        val match = orderItems.find { it.itemId == item.id }
                        if (match != null) {
                            val pending = match.quantity - match.returnedQuantity - match.damagedQuantity
                            if (pending > 0) {
                                val orderDeliveryStart = DateUtils.startOfDay(order.deliveryDate)
                                val orderReturnStart = DateUtils.startOfDay(order.returnDate)
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
        excludeOrderId: String? = null
    ): Map<String, StockDetails> {
        val days = getDaysInRange(deliveryDate, returnDate)
        if (days.isEmpty()) return emptyMap()

        val items = getAllItemsList().filter { it.isActive }
        val activeOrders = getAllOrdersList().filter {
            it.orderStatus == OrderStatus.CONFIRMED || it.orderStatus == OrderStatus.DELIVERED
        }
        val allOrderItems = getAllOrderItemsList().filter { !it.isCustomerOwned }
        val itemsByOrder = allOrderItems.groupBy { it.orderId }

        val allCustomers = getAllCustomersList()
        val customerPendingJars = allCustomers.sumOf { it.pendingReturnJars }
        val waterJar = getWaterJarItem()

        val today = DateUtils.startOfToday()

        return items.associate { item ->
            val isWaterJar = (waterJar != null && item.id == waterJar.id) ||
                    item.name.equals("Water Jar", ignoreCase = true) ||
                    item.name.contains("jar", ignoreCase = true)
            val customerExtra = if (isWaterJar) customerPendingJars else 0

            var minAvailable = Int.MAX_VALUE
            var riskOnDelivery = 0
            var outOnDelivery = 0

            days.forEachIndexed { index, day ->
                val isToday = day == today
                var outQty = customerExtra
                var riskQty = 0

                activeOrders.forEach { order ->
                    if (order.id != excludeOrderId) {
                        val orderItems = itemsByOrder[order.id] ?: emptyList()
                        val match = orderItems.find { it.itemId == item.id }
                        if (match != null) {
                            val pending = match.quantity - match.returnedQuantity - match.damagedQuantity
                            if (pending > 0) {
                                val orderDeliveryStart = DateUtils.startOfDay(order.deliveryDate)
                                val orderReturnStart = DateUtils.startOfDay(order.returnDate)
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
        cal.set(java.util.Calendar.MILLISECOND, 0)

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
        excludeOrderId: String? = null
    ): Map<String, Int> {
        return getStockDetailsForDate(selectedDate, excludeOrderId).associate { it.itemId to it.availableQty }
    }

    suspend fun getDateWiseRentedQuantities(selectedDate: Long): List<RentedQuantity> {
        val allOrders = getAllOrdersList()
        val activeOrders = allOrders.filter {
            it.orderStatus in listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED)
        }
        val filteredOrderIds = activeOrders.filter {
            DateUtils.startOfDay(it.deliveryDate) <= selectedDate
        }.map { it.id }.toSet()

        if (filteredOrderIds.isEmpty()) return emptyList()

        val allOrderItems = getAllOrderItemsList().filter { it.orderId in filteredOrderIds }
        return allOrderItems
            .filter { !it.isCustomerOwned && (it.quantity - it.returnedQuantity - it.damagedQuantity) > 0 }
            .groupBy { it.itemId }
            .map { (itemId, items) ->
                RentedQuantity(
                    itemId = itemId,
                    totalRented = items.sumOf { it.quantity - it.returnedQuantity - it.damagedQuantity }
                )
            }
    }

    // ── Reports ─────────────────────────────────────────────

    suspend fun getItemWiseIncome(start: Long, end: Long): List<ItemIncome> {
        val orders = getAllOrdersList().filter {
            it.deliveryDate >= start && it.deliveryDate < end && it.orderStatus != OrderStatus.CANCELLED
        }
        if (orders.isEmpty()) return emptyList()

        val orderIds = orders.map { it.id }.toSet()
        val allItems = getAllOrderItemsList().filter { it.orderId in orderIds }
        return allItems
            .groupBy { it.itemName }
            .map { (name, items) ->
                ItemIncome(
                    itemName = name,
                    totalIncome = items.sumOf { it.totalAmount }
                )
            }
            .sortedByDescending { it.totalIncome }
    }

    suspend fun getTotalIncomeByDelivery(start: Long, end: Long): Double {
        val orders = getAllOrdersList().filter {
            it.deliveryDate >= start && it.deliveryDate < end && it.orderStatus != OrderStatus.CANCELLED
        }
        return orders.sumOf { it.grandTotal }
    }

    suspend fun getOrderCountByDelivery(start: Long, end: Long): Int {
        val orders = getAllOrdersList().filter {
            it.deliveryDate >= start && it.deliveryDate < end && it.orderStatus != OrderStatus.CANCELLED
        }
        return orders.size
    }

    suspend fun getPendingBalanceByDelivery(start: Long, end: Long): Double {
        val orders = getAllOrdersList().filter {
            it.deliveryDate >= start && it.deliveryDate < end && it.orderStatus != OrderStatus.CANCELLED
        }
        return orders.sumOf { it.balanceAmount }
    }

    // ══════════════════════════════════════════════════════════
    // RETURNS
    // ══════════════════════════════════════════════════════════

    suspend fun getOrdersWithPendingReturns(): List<Order> {
        val allOrders = getAllOrdersList().filter {
            it.orderStatus in listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED, OrderStatus.COMPLETED)
        }
        if (allOrders.isEmpty()) return emptyList()
        val allItems = getAllOrderItemsList().filter { !it.isCustomerOwned }
        val itemsByOrder = allItems.groupBy { it.orderId }
        return allOrders.filter { order ->
            val items = itemsByOrder[order.id] ?: emptyList()
            items.any { it.quantity > (it.returnedQuantity + it.damagedQuantity) }
        }.sortedBy { it.returnDate }
    }

    suspend fun getReturnedOrders(): List<Order> {
        val snapshot = ordersCol
            .whereEqualTo("deleted", false)
            .get().await()
        return snapshot.toObjects(Order::class.java)
            .filter { it.orderStatus == OrderStatus.COMPLETED }
            .sortedByDescending { it.updatedAt }
    }

    suspend fun getReturnedOrdersInRange(start: Long, end: Long): List<Order> {
        val snapshot = ordersCol
            .whereEqualTo("deleted", false)
            .get().await()
        return snapshot.toObjects(Order::class.java)
            .filter { it.orderStatus == OrderStatus.COMPLETED && it.updatedAt >= start && it.updatedAt < end }
            .sortedByDescending { it.updatedAt }
    }

    suspend fun recordReturn(orderId: String, returnEntries: Map<String, Int>) {
        val items = getOrderItemsList(orderId)
        for ((orderItemId, returnedNow) in returnEntries) {
            if (returnedNow <= 0) continue
            val item = items.find { it.id == orderItemId } ?: continue
            val newReturned = (item.returnedQuantity + returnedNow).coerceAtMost(item.quantity)
            ordersCol.document(orderId).collection("order_items")
                .document(orderItemId)
                .update("returnedQuantity", newReturned)
                .await()
        }

        // Check if all items are fully returned
        if (areAllItemsReturned(orderId)) {
            updateOrderStatus(orderId, OrderStatus.COMPLETED)
        }
    }

    suspend fun recordReturnWithDamaged(
        orderId: String,
        returnEntries: Map<String, Int>,
        damagedEntries: Map<String, Int>
    ) {
        val items = getOrderItemsList(orderId)

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
            ordersCol.document(orderId).collection("order_items")
                .document(item.id)
                .update(
                    mapOf(
                        "returnedQuantity" to newReturned,
                        "damagedQuantity" to newDamaged
                    )
                ).await()

            // Reduce physical stock for damaged jars
            if (actualDamaged > 0 && !item.isCustomerOwned) {
                val dbItem = getItemById(item.itemId)
                if (dbItem != null) {
                    val newStock = maxOf(0, dbItem.totalStock - actualDamaged)
                    updateItem(dbItem.copy(totalStock = newStock))
                }
            }
        }

        // Check if all items are fully returned/accounted for
        if (areAllItemsReturned(orderId)) {
            updateOrderStatus(orderId, OrderStatus.COMPLETED)
        }
    }

    private suspend fun areAllItemsReturned(orderId: String): Boolean {
        val items = getOrderItemsList(orderId)
        return items.filter { !it.isCustomerOwned }.all {
            it.quantity <= (it.returnedQuantity + it.damagedQuantity)
        }
    }

    // ══════════════════════════════════════════════════════════
    // QUICK JAR ENTRY
    // ══════════════════════════════════════════════════════════

    suspend fun getWaterJarItem(): Item? {
        val allItems = getAllItemsList()
        return allItems.find { it.name.equals("Water Jar", ignoreCase = true) }
    }

    suspend fun saveQuickJarEntry(
        customer: Customer,
        quantity: Int,
        isCustomerOwned: Boolean,
        paidAmount: Double,
        deliveryDate: Long,
        waterJarItem: Item,
        customRate: Double? = null
    ): String {
        val effectiveRate = customRate ?: waterJarItem.ratePerDay
        val totalAmount = quantity * effectiveRate

        val freshCustomer = getCustomerById(customer.id) ?: customer
        var currentAdvance = freshCustomer.advanceBalance

        // Calculate how much advance credit can be used for this order
        val balanceNeeded = maxOf(0.0, totalAmount - paidAmount)
        val creditToUse = if (balanceNeeded > 0 && currentAdvance > 0) {
            minOf(currentAdvance, balanceNeeded)
        } else 0.0

        val effectivePaid = paidAmount + creditToUse
        currentAdvance -= creditToUse

        // If user paid more in cash than totalAmount (excess cash), add to advance credit
        if (paidAmount > totalAmount) {
            val excessCash = paidAmount - totalAmount
            currentAdvance += excessCash
        }

        // Update customer advance balance if changed
        if (currentAdvance != freshCustomer.advanceBalance) {
            updateCustomer(freshCustomer.copy(advanceBalance = currentAdvance))
        }

        val paymentStatus = when {
            effectivePaid >= totalAmount -> PaymentStatusType.PAID
            effectivePaid > 0 -> PaymentStatusType.PARTIALLY_PAID
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
            discountAmount = 0.0,
            grandTotal = totalAmount,
            advancePaid = minOf(effectivePaid, totalAmount),
            balanceAmount = maxOf(0.0, totalAmount - effectivePaid),
            orderStatus = OrderStatus.DELIVERED,
            paymentStatus = paymentStatus
        )

        val orderItem = OrderItem(
            itemId = waterJarItem.id,
            itemName = waterJarItem.name,
            quantity = quantity,
            ratePerDay = effectiveRate,
            rentalDays = 1,
            totalAmount = totalAmount,
            isCustomerOwned = isCustomerOwned
        )

        val orderId = createOrder(order, listOf(orderItem))

        // Record payment for advance credit used
        if (creditToUse > 0) {
            recordPayment(
                Payment(
                    orderId = orderId,
                    customerName = customer.name,
                    customerMobile = customer.mobileNumber,
                    amount = creditToUse,
                    paymentDate = System.currentTimeMillis(),
                    paymentMethod = "Advance Credit",
                    notes = "Adjusted from Advance Credit"
                )
            )
        }

        // Record payment for cash paid (capped at totalAmount - creditToUse if excess paid)
        val cashRecordedForOrder = minOf(paidAmount, maxOf(0.0, totalAmount - creditToUse))
        if (cashRecordedForOrder > 0) {
            recordPayment(
                Payment(
                    orderId = orderId,
                    customerName = customer.name,
                    customerMobile = customer.mobileNumber,
                    amount = cashRecordedForOrder,
                    paymentDate = System.currentTimeMillis(),
                    paymentMethod = PaymentMethod.CASH,
                    notes = "Quick Jar Entry payment"
                )
            )
        }

        return orderId
    }

    suspend fun recordLumpSumPayment(
        customer: Customer,
        amount: Double,
        paymentMethod: String
    ) {
        recordLumpSumPaymentAndGetRemaining(customer, amount, paymentMethod)
    }

    /**
     * Pays off order-based balances (oldest first) and returns any remaining amount
     * that was not applied to any order. The caller can then deduct this from
     * the customer's baseline pendingAmount.
     */
    suspend fun recordLumpSumPaymentAndGetRemaining(
        customer: Customer,
        amount: Double,
        paymentMethod: String
    ): Double {
        val customerOrders = getAllOrdersList()
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

        return remaining
    }

    suspend fun getCustomerJarStats(customerId: String): CustomerJarStats {
        val monthStart = DateUtils.startOfThisMonth()
        val monthEnd = DateUtils.endOfThisMonth()

        val customer = getCustomerById(customerId)

        val allOrders = getAllOrdersList()
            .filter { it.customerId == customerId && it.orderStatus != OrderStatus.CANCELLED }

        val monthOrders = allOrders.filter { it.deliveryDate in monthStart until monthEnd }

        val allOrderIds = allOrders.map { it.id }.toSet()
        val allOrderItems = if (allOrderIds.isNotEmpty()) {
            getAllOrderItemsList().filter { it.orderId in allOrderIds }
        } else emptyList()
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
        val pendingBalance = (customer?.pendingAmount ?: 0.0) + allOrders.sumOf { it.balanceAmount }
        val paidAmount = totalOrderAmount - allOrders.sumOf { it.balanceAmount }

        // Pending return jars (Our Jar only, across all active orders + notebook baseline)
        var pendingReturnJars = customer?.pendingReturnJars ?: 0
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
        val advanceBalance = customer?.advanceBalance ?: 0.0

        return CustomerJarStats(
            thisMonthJarCount = thisMonthJarCount,
            thisMonthJarAmount = thisMonthJarAmount,
            thisMonthPaid = thisMonthPaid,
            totalPaid = paidAmount,
            pendingBalance = pendingBalance,
            advanceBalance = advanceBalance,
            pendingReturnJars = pendingReturnJars,
            lastJarQuantity = lastJarQuantity,
            lastJarDate = lastJarDate,
            lastJarIsCustomerOwned = lastJarIsCustomerOwned
        )
    }

    suspend fun getCustomerPendingJarReturns(customerId: String): List<PendingJarReturn> {
        val allOrders = getAllOrdersList()
            .filter { it.customerId == customerId && it.orderStatus in listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED) }

        if (allOrders.isEmpty()) return emptyList()

        val activeOrderIds = allOrders.map { it.id }.toSet()
        val allOrderItems = getAllOrderItemsList().filter { it.orderId in activeOrderIds }
        val itemsByOrder = allOrderItems.groupBy { it.orderId }

        val results = mutableListOf<PendingJarReturn>()
        for (order in allOrders) {
            val items = itemsByOrder[order.id] ?: emptyList()
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

    suspend fun getRecentJarEntries(customerId: String, limit: Int = 20): List<JarEntry> {
        val allOrders = getAllOrdersList()
            .filter { it.customerId == customerId && it.orderStatus != OrderStatus.CANCELLED }
            .sortedByDescending { it.deliveryDate }

        if (allOrders.isEmpty()) return emptyList()

        val candidateOrderIds = allOrders.map { it.id }.toSet()
        val allOrderItems = getAllOrderItemsList().filter { it.orderId in candidateOrderIds }
        val itemsByOrder = allOrderItems.groupBy { it.orderId }

        val results = mutableListOf<JarEntry>()
        for (order in allOrders) {
            val items = itemsByOrder[order.id] ?: emptyList()
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

    fun getAllPayments(): Flow<List<Payment>> = callbackFlow {
        // Listen to all orders and collect their payments subcollections
        val registration = ordersCol
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val orderIds = snapshot?.documents?.map { it.id } ?: emptyList()
                // We can't easily listen to all subcollections at once,
                // so we do a one-time fetch for the payment list
                val allPayments = mutableListOf<Payment>()
                // This is called reactively when orders change
                launch(kotlinx.coroutines.Dispatchers.IO) {
                    for (orderId in orderIds) {
                        try {
                            val paySnap = ordersCol.document(orderId).collection("payments").get().await()
                            allPayments.addAll(paySnap.toObjects(Payment::class.java))
                        } catch (_: Exception) {}
                    }
                    trySend(allPayments.sortedByDescending { it.paymentDate })
                }
            }
        awaitClose { registration.remove() }
    }

    fun getPaymentsByOrder(orderId: String): Flow<List<Payment>> = callbackFlow {
        val registration = ordersCol.document(orderId).collection("payments")
            .orderBy("paymentDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val payments = snapshot?.toObjects(Payment::class.java) ?: emptyList()
                trySend(payments)
            }
        awaitClose { registration.remove() }
    }

    fun getAllPaymentsFlow(): Flow<List<Payment>> = callbackFlow {
        val registration = db.collectionGroup("payments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val payments = snapshot?.toObjects(Payment::class.java) ?: emptyList()
                trySend(payments.sortedByDescending { it.paymentDate })
            }
        awaitClose { registration.remove() }
    }

    fun getPaymentsInRange(start: Long, end: Long): Flow<List<Payment>> = flow {
        try {
            val snap = db.collectionGroup("payments").get().await()
            val payments = snap.toObjects(Payment::class.java)
                .filter { it.paymentDate in start until end }
            emit(payments.sortedByDescending { it.paymentDate })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun getPaymentsInRangeList(start: Long, end: Long): List<Payment> {
        return try {
            val snap = db.collectionGroup("payments").get().await()
            val list = snap.toObjects(Payment::class.java).filter { it.paymentDate in start until end }
            if (list.isNotEmpty()) return list

            // Fallback: check orders created/active in range with payments
            val orders = getAllOrdersList()
            orders.filter { it.orderDate in start until end && it.advancePaid > 0 }
                .map {
                    Payment(
                        orderId = it.id,
                        customerName = it.customerName,
                        customerMobile = it.customerMobile,
                        amount = it.advancePaid,
                        paymentDate = it.orderDate,
                        paymentMethod = PaymentMethod.CASH
                    )
                }
        } catch (e: Exception) {
            val orders = try { getAllOrdersList() } catch (_: Exception) { emptyList() }
            orders.filter { it.orderDate in start until end && it.advancePaid > 0 }
                .map {
                    Payment(
                        orderId = it.id,
                        customerName = it.customerName,
                        customerMobile = it.customerMobile,
                        amount = it.advancePaid,
                        paymentDate = it.orderDate,
                        paymentMethod = PaymentMethod.CASH
                    )
                }
        }
    }

    suspend fun getTotalPaidForOrder(orderId: String): Double {
        return try {
            val snapshot = ordersCol.document(orderId).collection("payments").get().await()
            val payments = snapshot.toObjects(Payment::class.java)
            payments.sumOf { it.amount }
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun getTotalPaymentReceived(start: Long, end: Long): Double {
        return try {
            val snap = db.collectionGroup("payments").get().await()
            snap.toObjects(Payment::class.java)
                .filter { it.paymentDate in start until end }
                .sumOf { it.amount }
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun getOverallTotalReceived(): Double {
        return try {
            val snap = db.collectionGroup("payments").get().await()
            snap.toObjects(Payment::class.java).sumOf { it.amount }
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun recordPayment(payment: Payment) {
        val orderId = payment.orderId
        val paymentRef = ordersCol.document(orderId).collection("payments").document()
        val newPayment = payment.copy(id = paymentRef.id)
        paymentRef.set(newPayment).await()

        // Recalculate and update order payment status
        val totalPaid = getTotalPaidForOrder(orderId)
        val order = getOrderById(orderId) ?: return
        val newPaymentStatus = when {
            totalPaid >= order.grandTotal -> PaymentStatusType.PAID
            totalPaid > 0 -> PaymentStatusType.PARTIALLY_PAID
            else -> PaymentStatusType.UNPAID
        }
        val balance = maxOf(0.0, order.grandTotal - totalPaid)
        val effectiveAdvance = minOf(totalPaid, order.grandTotal)

        ordersCol.document(orderId).update(
            mapOf(
                "balanceAmount" to balance,
                "paymentStatus" to newPaymentStatus,
                "advancePaid" to effectiveAdvance,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()

        // If excess was paid beyond order grandTotal, credit customer ledger
        val previousTotalPaid = maxOf(0.0, totalPaid - payment.amount)
        val balanceBeforeThisPayment = maxOf(0.0, order.grandTotal - previousTotalPaid)
        var excess = maxOf(0.0, payment.amount - balanceBeforeThisPayment)

        if (excess > 0.0 && order.customerId.isNotBlank()) {
            val customer = getCustomerById(order.customerId)
            if (customer != null) {
                var currentCustomer = customer
                // Deduct from baseline pendingAmount if any exists
                if (excess > 0.0 && currentCustomer.pendingAmount > 0.0) {
                    val deductFromBaseline = minOf(excess, currentCustomer.pendingAmount)
                    val newBaseline = maxOf(0.0, currentCustomer.pendingAmount - deductFromBaseline)
                    currentCustomer = currentCustomer.copy(pendingAmount = newBaseline)
                    excess -= deductFromBaseline
                }
                // Add remainder to advance credit (जमा)
                if (excess > 0.0) {
                    currentCustomer = currentCustomer.copy(advanceBalance = currentCustomer.advanceBalance + excess)
                }
                updateCustomer(currentCustomer)
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // BACKUP / RESTORE (Cloud-native — exports all data)
    // ══════════════════════════════════════════════════════════

    suspend fun getAllDataForBackup(): BackupData {
        return BackupData(
            items = getAllItemsList(),
            customers = try {
                customersCol.whereEqualTo("deleted", false).get().await()
                    .toObjects(Customer::class.java)
            } catch (e: Exception) { emptyList() },
            orders = getAllOrdersList(),
            orderItems = getAllOrderItemsList(),
            payments = try {
                val allPayments = mutableListOf<Payment>()
                for (order in getAllOrdersList()) {
                    val snap = ordersCol.document(order.id).collection("payments").get().await()
                    allPayments.addAll(snap.toObjects(Payment::class.java))
                }
                allPayments
            } catch (e: Exception) { emptyList() }
        )
    }

    suspend fun restoreFromBackup(data: BackupData) {
        // Insert items
        for (item in data.items) {
            val docRef = if (item.id.isNotEmpty()) itemsCol.document(item.id) else itemsCol.document()
            docRef.set(item.copy(id = docRef.id)).await()
        }
        // Insert customers
        for (customer in data.customers) {
            val docRef = if (customer.id.isNotEmpty()) customersCol.document(customer.id) else customersCol.document()
            docRef.set(customer.copy(id = docRef.id)).await()
        }
        // Insert orders with their order_items and payments
        for (order in data.orders) {
            val docRef = if (order.id.isNotEmpty()) ordersCol.document(order.id) else ordersCol.document()
            docRef.set(order.copy(id = docRef.id)).await()

            // Insert order items for this order
            val orderItems = data.orderItems.filter { it.orderId == order.id }
            for (oi in orderItems) {
                val oiRef = docRef.collection("order_items").document()
                oiRef.set(oi.copy(id = oiRef.id, orderId = docRef.id)).await()
            }

            // Insert payments for this order
            val payments = data.payments.filter { it.orderId == order.id }
            for (p in payments) {
                val pRef = docRef.collection("payments").document()
                pRef.set(p.copy(id = pRef.id, orderId = docRef.id)).await()
            }
        }
    }

    /**
     * Seed default items if the items collection is empty.
     * Called once on first app launch after Firebase setup.
     */
    suspend fun seedDefaultItemsIfNeeded() {
        try {
            val snapshot = itemsCol.limit(1).get().await()
            if (snapshot.isEmpty) {
                insertItem(Item(name = "Table", ratePerDay = 30.0, totalStock = 100, lowStockAlert = 10))
                insertItem(Item(name = "Chair", ratePerDay = 5.0, totalStock = 500, lowStockAlert = 50))
                insertItem(Item(name = "Water Jar", ratePerDay = 30.0, totalStock = 50, lowStockAlert = 5))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class StockDetails(
    val itemId: String,
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
    val advanceBalance: Double = 0.0,
    val pendingReturnJars: Int = 0,
    val lastJarQuantity: Int = 0,
    val lastJarDate: Long = 0L,
    val lastJarIsCustomerOwned: Boolean = false
)

/**
 * Pending jar return entry for a customer.
 */
data class PendingJarReturn(
    val orderId: String,
    val orderItemId: String,
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
    val orderId: String,
    val date: Long,
    val quantity: Int,
    val amount: Double,
    val isCustomerOwned: Boolean,
    val paymentStatus: String
)

/**
 * Rented quantity per item (replaces Room DAO result class).
 */
data class RentedQuantity(
    val itemId: String,
    val totalRented: Int
)

/**
 * Item income for reports (replaces Room DAO result class).
 */
data class ItemIncome(
    val itemName: String,
    val totalIncome: Double
)