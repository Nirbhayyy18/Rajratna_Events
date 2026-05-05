package com.rajratna.events.data.dao

import androidx.room.*
import com.rajratna.events.data.entity.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE orderId = :orderId ORDER BY paymentDate DESC")
    fun getPaymentsByOrder(orderId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE paymentDate >= :start AND paymentDate < :end ORDER BY paymentDate DESC")
    fun getPaymentsInRange(start: Long, end: Long): Flow<List<Payment>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE orderId = :orderId")
    suspend fun getTotalPaidForOrder(orderId: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE paymentDate >= :start AND paymentDate < :end")
    suspend fun getTotalPaymentReceived(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments")
    suspend fun getOverallTotalReceived(): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Query("SELECT * FROM payments")
    suspend fun getAllPaymentsList(): List<Payment>
}
