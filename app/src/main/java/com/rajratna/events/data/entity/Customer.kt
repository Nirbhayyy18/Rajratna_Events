package com.rajratna.events.data.entity

import com.google.firebase.firestore.DocumentId

/**
 * Customer record – stores contact info.
 * Aggregated stats (total orders, amounts) are computed from orders.
 * Stored in Firestore "customers" collection.
 */
data class Customer(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val mobileNumber: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
