package com.rajratna.events.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Customer record – stores contact info.
 * Aggregated stats (total orders, amounts) are computed from orders.
 */
@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mobileNumber: String,
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
