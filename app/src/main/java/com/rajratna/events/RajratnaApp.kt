package com.rajratna.events

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rajratna.events.data.repository.AppRepository
import com.rajratna.events.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Application class - provides Firestore repository and Auth singletons.
 * Room database has been replaced by Firebase Firestore.
 */
class RajratnaApp : Application() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    val repository by lazy { AppRepository(firestore) }

    val authRepository by lazy {
        AuthRepository(
            FirebaseAuth.getInstance(),
            firestore
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Enable Firestore offline persistence (enabled by default, but explicit for clarity)
        firestore.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        // Seed default items if the database is empty (first launch)
        CoroutineScope(Dispatchers.IO).launch {
            repository.seedDefaultItemsIfNeeded()
        }
    }
}
