package com.rajratna.events.data.entity

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Customer record – stores contact info and notebook baseline ledger values.
 * Stored in Firestore "customers" collection.
 */
data class Customer(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val mobileNumber: String = "",
    val address: String = "",
    val totalJars: Int = 0,
    val pendingReturnJars: Int = 0,
    val pendingAmount: Double = 0.0,
    val advanceBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("deleted")
    @set:PropertyName("deleted")
    var isDeleted: Boolean = false
)

