package com.rajratna.events.data.entity

/**
 * Represents a user of the multi-device system.
 * Stored in Firestore "users" collection.
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val mobileNumber: String = "",
    val role: String = Role.STAFF,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

object Role {
    const val ADMIN = "Admin"
    const val STAFF = "Staff"
}
