package com.rajratna.events.data.entity

import com.google.firebase.firestore.DocumentId

/**
 * Payment record for an order.
 * Multiple payments can be recorded against one order.
 * Stored in Firestore as subcollection "payments" under each order document.
 */
data class Payment(
    @DocumentId
    val id: String = "",
    val orderId: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val amount: Double = 0.0,
    val paymentDate: Long = 0L,
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
