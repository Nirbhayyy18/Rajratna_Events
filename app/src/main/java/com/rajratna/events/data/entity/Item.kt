package com.rajratna.events.data.entity

import com.google.firebase.firestore.DocumentId

/**
 * Rental item (e.g. Table, Chair, Water Jar).
 * Owner can add/edit items and change rates.
 * Stored in Firestore "items" collection.
 */
data class Item(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val ratePerDay: Double = 0.0,
    val totalStock: Int = 0,
    val lowStockAlert: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
