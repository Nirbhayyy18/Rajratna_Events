package com.rajratna.events

import android.app.Application
import com.rajratna.events.data.database.AppDatabase
import com.rajratna.events.data.repository.AppRepository

/**
 * Application class - provides database and repository singletons.
 */
class RajratnaApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }

    val repository by lazy {
        AppRepository(
            itemDao = database.itemDao(),
            customerDao = database.customerDao(),
            orderDao = database.orderDao(),
            paymentDao = database.paymentDao()
        )
    }
}
