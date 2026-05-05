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

    @Query("""
        SELECT oi.itemId, COALESCE(SUM(oi.quantity), 0) AS totalRented
        FROM order_items oi
        INNER JOIN orders o ON oi.orderId = o.id
        WHERE o.orderStatus IN ('Confirmed', 'Delivered')
        GROUP BY oi.itemId
    """)
    suspend fun getRentedQuantities(): List<RentedQuantity>

    // ── Dashboard: Pending Returns ──────────────────────────

    @Query("""
        SELECT * FROM orders
        WHERE orderStatus IN ('Confirmed', 'Delivered')
        AND returnDate <= :endOfToday
        ORDER BY returnDate ASC
        LIMIT :limit
    """)
    suspend fun getPendingReturnOrders(endOfToday: Long, limit: Int = 3): List<Order>

    @Query("""
        SELECT COUNT(*) FROM orders
        WHERE orderStatus IN ('Confirmed', 'Delivered')
        AND returnDate <= :endOfToday
    """)
    suspend fun getPendingReturnCount(endOfToday: Long): Int

    // ── Dashboard: Today's Order Counts ─────────────────────

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
}

data class RentedQuantity(val itemId: Long, val totalRented: Int)

