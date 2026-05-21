package com.rajratna.events.data.dao

import androidx.room.*
import com.rajratna.events.data.entity.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Update
    suspend fun updateItem(item: Item)

    @Query("UPDATE items SET isActive = :isActive WHERE id = :id")
    suspend fun setItemActive(id: Long, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM items")
    suspend fun getItemCount(): Int

    @Query("SELECT * FROM items")
    suspend fun getAllItemsList(): List<Item>

    @Query("SELECT COUNT(*) FROM order_items WHERE itemId = :itemId")
    suspend fun getItemUsageCount(itemId: Long): Int

    @Delete
    suspend fun deleteItem(item: Item)
}
