package com.rajratna.events.data.entity

import com.google.firebase.firestore.DocumentId

/**
 * Order placed by a customer.
 * Contains denormalized customer info for quick display in lists.
 * Financial totals are stored here; individual items are in OrderItem subcollection.
 * Stored in Firestore "orders" collection.
 */
data class Order(
    @DocumentId
    val id: String = "",
    val billNumber: Int = 0,
    val customerId: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val customerAddress: String = "",
    val orderDate: Long = 0L,
    val deliveryDate: Long = 0L,
    val returnDate: Long = 0L,
    val rentalDays: Int = 0,
    val notes: String = "",
    val itemsTotal: Double = 0.0,
    val transportRent: Double = 0.0,
    val grandTotal: Double = 0.0,
    val advancePaid: Double = 0.0,
    val balanceAmount: Double = 0.0,
    val orderStatus: String = OrderStatus.PENDING,
    val paymentStatus: String = PaymentStatusType.UNPAID,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

/**
 * Order status constants.
 */
object OrderStatus {
    const val PENDING = "Pending"
    const val CONFIRMED = "Confirmed"
    const val DELIVERED = "Delivered"
    const val COMPLETED = "Completed"
    const val CANCELLED = "Cancelled"

    val all = listOf(PENDING, CONFIRMED, DELIVERED, COMPLETED, CANCELLED)
}

/**
 * Payment status constants.
 */
object PaymentStatusType {
    const val UNPAID = "Unpaid"
    const val PARTIALLY_PAID = "Partially Paid"
    const val PAID = "Paid"

    val all = listOf(UNPAID, PARTIALLY_PAID, PAID)
}
