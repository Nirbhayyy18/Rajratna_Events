package com.rajratna.events.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rental item (e.g. Table, Chair, Water Jar).
 * Owner can add/edit items and change rates.
 */
@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ratePerDay: Double,
    val totalStock: Int = 0,
    val lowStockAlert: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
