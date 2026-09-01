package com.rajratna.events.data.entity

import com.google.firebase.firestore.DocumentId

/**
 * Line item within an order.
 * Stores snapshot of item name/rate at time of order creation.
 * Stored in Firestore as subcollection "order_items" under each order document.
 */
data class OrderItem(
    @DocumentId
    val id: String = "",
    val orderId: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val quantity: Int = 0,
    val ratePerDay: Double = 0.0,
    val rentalDays: Int = 0,
    val totalAmount: Double = 0.0,
    val returnedQuantity: Int = 0,
    val damagedQuantity: Int = 0,
    val isCustomerOwned: Boolean = false
)
