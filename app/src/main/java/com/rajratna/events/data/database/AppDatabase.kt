package com.rajratna.events.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rajratna.events.data.dao.CustomerDao
import com.rajratna.events.data.dao.ItemDao
import com.rajratna.events.data.dao.OrderDao
import com.rajratna.events.data.dao.PaymentDao
import com.rajratna.events.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Item::class, Customer::class, Order::class, OrderItem::class, Payment::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun customerDao(): CustomerDao
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from version 1 to 2: adds stock management columns to items table.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN totalStock INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE items ADD COLUMN lowStockAlert INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration from version 2 to 3: adds returnedQuantity column to order_items table
         * for item-level return tracking.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE order_items ADD COLUMN returnedQuantity INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration from version 3 to 4: adds isCustomerOwned column to order_items table
         * for customer-owned water jar tracking.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE order_items ADD COLUMN isCustomerOwned INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration from version 4 to 5: adds damagedQuantity column to order_items table
         * for tracking damaged/missing jars during returns.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE order_items ADD COLUMN damagedQuantity INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rajratna_events_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Resets the singleton instance (used during restore to reload fresh DB).
         */
        fun resetInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }

    /**
     * Seeds the database with default rental items on first creation.
     */
    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val itemDao = database.itemDao()
                    // Insert default items only if table is empty
                    if (itemDao.getItemCount() == 0) {
                        itemDao.insertItem(Item(name = "Table", ratePerDay = 30.0, totalStock = 100, lowStockAlert = 10))
                        itemDao.insertItem(Item(name = "Chair", ratePerDay = 5.0, totalStock = 500, lowStockAlert = 50))
                        itemDao.insertItem(Item(name = "Water Jar", ratePerDay = 30.0, totalStock = 50, lowStockAlert = 5))
                    }
                }
            }
        }
    }
}
