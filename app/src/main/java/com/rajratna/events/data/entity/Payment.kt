package com.rajratna.events.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Payment record for an order.
 * Multiple payments can be recorded against one order.
 */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: Long,
    val customerName: String,
    val customerMobile: String,
    val amount: Double,
    val paymentDate: Long,
    val paymentMethod: String = PaymentMethod.CASH,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Payment method constants.
 */
object PaymentMethod {
    const val CASH = "Cash"
    const val UPI = "UPI"
    const val BANK_TRANSFER = "Bank Transfer"
    const val OTHER = "Other"

    val all = listOf(CASH, UPI, BANK_TRANSFER, OTHER)
}
