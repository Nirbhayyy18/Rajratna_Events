package com.rajratna.events.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Line item within an order.
 * Stores snapshot of item name/rate at time of order creation.
 */
@Entity(
    tableName = "order_items",
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
data class OrderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: Long,
    val itemId: Long,
    val itemName: String,
    val quantity: Int,
    val ratePerDay: Double,
    val rentalDays: Int,
    val totalAmount: Double,
    val returnedQuantity: Int = 0,
    val isCustomerOwned: Boolean = false
)
