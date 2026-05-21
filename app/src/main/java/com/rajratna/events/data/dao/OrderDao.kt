package com.rajratna.events.data.dao

import androidx.room.*
import com.rajratna.events.data.entity.Order
import com.rajratna.events.data.entity.OrderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    // ── Orders ──────────────────────────────────────────────

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderById(id: Long): Order?

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getOrderByIdFlow(id: Long): Flow<Order?>

    @Query("""
        SELECT * FROM orders 
        WHERE orderDate >= :startOfDay AND orderDate < :endOfDay 
        ORDER BY createdAt DESC
    """)
    fun getOrdersByDate(startOfDay: Long, endOfDay: Long): Flow<List<Order>>

    @Query("""
        SELECT * FROM orders 
        WHERE deliveryDate >= :startOfDay AND deliveryDate < :endOfDay 
        ORDER BY deliveryDate ASC
    """)
    fun getOrdersByDeliveryDate(startOfDay: Long, endOfDay: Long): Flow<List<Order>>

    @Query("""
        SELECT * FROM orders 
        WHERE orderDate >= :start AND orderDate < :end 
        ORDER BY createdAt DESC
    """)
    fun getOrdersInRange(start: Long, end: Long): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE orderStatus = :status ORDER BY createdAt DESC")
    fun getOrdersByStatus(status: String): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE paymentStatus = :status ORDER BY createdAt DESC")
    fun getOrdersByPaymentStatus(status: String): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getOrdersByCustomer(customerId: Long): Flow<List<Order>>

    @Query("""
        SELECT * FROM orders 
        WHERE customerName LIKE '%' || :query || '%'
        OR customerMobile LIKE '%' || :query || '%'
        OR CAST(billNumber AS TEXT) LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchOrders(query: String): Flow<List<Order>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Query("UPDATE orders SET orderStatus = :status, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE orders SET 
        balanceAmount = grandTotal - :totalPaid,
        paymentStatus = CASE 
            WHEN :totalPaid >= grandTotal THEN 'Paid'
            WHEN :totalPaid > 0 THEN 'Partially Paid'
            ELSE 'Unpaid'
        END,
        updatedAt = :updatedAt
        WHERE id = :orderId
    """)
    suspend fun updatePaymentInfo(orderId: Long, totalPaid: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COALESCE(MAX(billNumber), 0) FROM orders")
    suspend fun getMaxBillNumber(): Int

    // ── Aggregations for Dashboard ──────────────────────────

    @Query("SELECT COUNT(*) FROM orders WHERE orderDate >= :start AND orderDate < :end")
    suspend fun getOrderCount(start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM orders WHERE orderStatus = :status AND orderDate >= :start AND orderDate < :end")
    suspend fun getOrderCountByStatus(status: String, start: Long, end: Long): Int

    @Query("SELECT COALESCE(SUM(grandTotal), 0.0) FROM orders WHERE orderDate >= :start AND orderDate < :end AND orderStatus != 'Cancelled'")
    suspend fun getTotalOrderAmount(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(itemsTotal), 0.0) FROM orders WHERE orderDate >= :start AND orderDate < :end AND orderStatus != 'Cancelled'")
    suspend fun getTotalItemsAmount(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(transportRent), 0.0) FROM orders WHERE orderDate >= :start AND orderDate < :end AND orderStatus != 'Cancelled'")
    suspend fun getTotalTransportRent(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(balanceAmount), 0.0) FROM orders WHERE orderDate >= :start AND orderDate < :end AND orderStatus != 'Cancelled'")
    suspend fun getTotalPendingBalance(start: Long, end: Long): Double

    // Overall pending
    @Query("SELECT COALESCE(SUM(balanceAmount), 0.0) FROM orders WHERE orderStatus != 'Cancelled'")
    suspend fun getOverallPendingBalance(): Double

    @Query("SELECT * FROM orders")
    suspend fun getAllOrdersList(): List<Order>

    // ── Order Items ─────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItem>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: Long): Flow<List<OrderItem>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsList(orderId: Long): List<OrderItem>

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItems(orderId: Long)

    @Query("SELECT * FROM order_items")
    suspend fun getAllOrderItemsList(): List<OrderItem>

    /**
     * Rented quantities per item — subtracts returnedQuantity.
     * Only counts orders that are active (not completed/cancelled).
     */
    @Query("""
        SELECT oi.itemId, COALESCE(SUM(oi.quantity - oi.returnedQuantity), 0) AS totalRented
        FROM order_items oi
        INNER JOIN orders o ON oi.orderId = o.id
        WHERE o.orderStatus IN ('Confirmed', 'Delivered')
        AND (oi.quantity - oi.returnedQuantity) > 0
        AND oi.isCustomerOwned = 0
        GROUP BY oi.itemId
    """)
    suspend fun getRentedQuantities(): List<RentedQuantity>

    // ── Return Tracking ─────────────────────────────────────

    /**
     * Update the returned quantity for a specific order item.
     */
    @Query("UPDATE order_items SET returnedQuantity = :returnedQuantity WHERE id = :orderItemId")
    suspend fun updateReturnedQuantity(orderItemId: Long, returnedQuantity: Int)

    /**
     * Check if all items in an order are fully returned.
     */
    @Query("""
        SELECT CASE WHEN COUNT(*) = 0 THEN 1 ELSE 0 END
        FROM order_items
        WHERE orderId = :orderId AND quantity > returnedQuantity AND isCustomerOwned = 0
    """)
    suspend fun areAllItemsReturned(orderId: Long): Boolean

    /**
     * Orders with pending return items (not cancelled, has unreturned items).
     * Ordered by return date ascending (most urgent first).
     */
    @Query("""
        SELECT DISTINCT o.* FROM orders o
        INNER JOIN order_items oi ON o.id = oi.orderId
        WHERE o.orderStatus IN ('Confirmed', 'Delivered')
        AND oi.quantity > oi.returnedQuantity
        AND oi.isCustomerOwned = 0
        ORDER BY o.returnDate ASC
    """)
    suspend fun getOrdersWithPendingReturns(): List<Order>

    /**
     * Orders with pending returns, limited for dashboard preview.
     */
    @Query("""
        SELECT DISTINCT o.* FROM orders o
        INNER JOIN order_items oi ON o.id = oi.orderId
        WHERE o.orderStatus IN ('Confirmed', 'Delivered')
        AND oi.quantity > oi.returnedQuantity
        AND o.returnDate <= :endOfToday
        AND oi.isCustomerOwned = 0
        ORDER BY o.returnDate ASC
        LIMIT :limit
    """)
    suspend fun getPendingReturnOrders(endOfToday: Long, limit: Int = 3): List<Order>

    /**
     * Count of orders with pending returns (due today or overdue).
     */
    @Query("""
        SELECT COUNT(DISTINCT o.id) FROM orders o
        INNER JOIN order_items oi ON o.id = oi.orderId
        WHERE o.orderStatus IN ('Confirmed', 'Delivered')
        AND oi.quantity > oi.returnedQuantity
        AND o.returnDate <= :endOfToday
        AND oi.isCustomerOwned = 0
    """)
    suspend fun getPendingReturnCount(endOfToday: Long): Int

    /**
     * Orders that were completed (all items returned) today.
     */
    @Query("""
        SELECT COUNT(*) FROM orders
        WHERE orderStatus = 'Completed'
        AND updatedAt >= :start AND updatedAt < :end
    """)
    suspend fun getReturnedTodayCount(start: Long, end: Long): Int

    /**
     * Completed orders (used for Returned tab in Returns screen).
     */
    @Query("""
        SELECT * FROM orders
        WHERE orderStatus = 'Completed'
        ORDER BY updatedAt DESC
    """)
    suspend fun getReturnedOrders(): List<Order>

    /**
     * Completed orders within a date range.
     */
    @Query("""
        SELECT * FROM orders
        WHERE orderStatus = 'Completed'
        AND updatedAt >= :start AND updatedAt < :end
        ORDER BY updatedAt DESC
    """)
    suspend fun getReturnedOrdersInRange(start: Long, end: Long): List<Order>

    // ── Dashboard: Today's Order Counts ─────────────────────

    /**
     * Count of active orders created today (Pending/Confirmed/Delivered).
     */
    @Query("""
        SELECT COUNT(*) FROM orders
        WHERE orderStatus IN ('Pending', 'Confirmed', 'Delivered')
    """)
    suspend fun getActiveOrderCount(): Int

    @Query("""
        SELECT COUNT(*) FROM orders
        WHERE orderDate >= :start AND orderDate < :end
        AND orderStatus IN ('Pending', 'Confirmed', 'Delivered')
    """)
    suspend fun getTodayActiveCount(start: Long, end: Long): Int

    @Query("""
        SELECT COUNT(*) FROM orders
        WHERE orderDate >= :start AND orderDate < :end
        AND orderStatus = 'Completed'
    """)
    suspend fun getTodayReturnedCount(start: Long, end: Long): Int

    // ── Dashboard: Order Items Summary ──────────────────────

    @Query("SELECT * FROM order_items WHERE orderId = :orderId LIMIT 1")
    suspend fun getFirstOrderItem(orderId: Long): OrderItem?

    // ── Date-Wise Stock Queries ──────────────────────────────

    /**
     * Date-wise rented quantities.
     * For a selected date, sum unreturned item quantities from non-cancelled orders
     * where selected date falls within the rental period (delivery to return).
     */
    @Query("""
        SELECT oi.itemId, COALESCE(SUM(oi.quantity - oi.returnedQuantity), 0) AS totalRented
        FROM order_items oi
        INNER JOIN orders o ON oi.orderId = o.id
        WHERE o.orderStatus IN ('Confirmed', 'Delivered')
        AND o.deliveryDate <= :selectedDate
        AND (oi.quantity - oi.returnedQuantity) > 0
        AND oi.isCustomerOwned = 0
        GROUP BY oi.itemId
    """)
    suspend fun getDateWiseRentedQuantities(selectedDate: Long): List<RentedQuantity>

    /**
     * Date-wise rented quantities excluding a specific order (for edit mode validation).
     */
    @Query("""
        SELECT oi.itemId, COALESCE(SUM(oi.quantity - oi.returnedQuantity), 0) AS totalRented
        FROM order_items oi
        INNER JOIN orders o ON oi.orderId = o.id
        WHERE o.orderStatus IN ('Confirmed', 'Delivered')
        AND o.id != :excludeOrderId
        AND o.deliveryDate <= :selectedDate
        AND (oi.quantity - oi.returnedQuantity) > 0
        AND oi.isCustomerOwned = 0
        GROUP BY oi.itemId
    """)
    suspend fun getDateWiseRentedQuantitiesExcluding(selectedDate: Long, excludeOrderId: Long): List<RentedQuantity>

    // ── Reports: Item-wise Income ────────────────────────────

    /**
     * Item-wise income within a date range (based on delivery date).
     * Excludes cancelled orders.
     */
    @Query("""
        SELECT oi.itemName, COALESCE(SUM(oi.totalAmount), 0.0) AS totalIncome
        FROM order_items oi
        INNER JOIN orders o ON oi.orderId = o.id
        WHERE o.deliveryDate >= :start AND o.deliveryDate < :end
        AND o.orderStatus != 'Cancelled'
        GROUP BY oi.itemName
        ORDER BY totalIncome DESC
    """)
    suspend fun getItemWiseIncome(start: Long, end: Long): List<ItemIncome>

    /**
     * Total income (grand total) based on delivery date, excluding cancelled.
     */
    @Query("SELECT COALESCE(SUM(grandTotal), 0.0) FROM orders WHERE deliveryDate >= :start AND deliveryDate < :end AND orderStatus != 'Cancelled'")
    suspend fun getTotalIncomeByDelivery(start: Long, end: Long): Double

    /**
     * Order count based on delivery date, excluding cancelled.
     */
    @Query("SELECT COUNT(*) FROM orders WHERE deliveryDate >= :start AND deliveryDate < :end AND orderStatus != 'Cancelled'")
    suspend fun getOrderCountByDelivery(start: Long, end: Long): Int

    /**
     * Pending balance based on delivery date, excluding cancelled.
     */
    @Query("SELECT COALESCE(SUM(balanceAmount), 0.0) FROM orders WHERE deliveryDate >= :start AND deliveryDate < :end AND orderStatus != 'Cancelled'")
    suspend fun getPendingBalanceByDelivery(start: Long, end: Long): Double
}

data class RentedQuantity(val itemId: Long, val totalRented: Int)

data class ItemIncome(val itemName: String, val totalIncome: Double)
