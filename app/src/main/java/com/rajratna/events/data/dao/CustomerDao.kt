package com.rajratna.events.data.dao

import androidx.room.*
import com.rajratna.events.data.entity.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("""
        SELECT * FROM customers 
        WHERE name LIKE '%' || :query || '%' 
        OR mobileNumber LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun getCustomerByMobile(mobile: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersList(): List<Customer>
}
