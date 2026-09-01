package com.rajratna.events.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rajratna.events.data.entity.User
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling Firebase Authentication and User Role management.
 */
class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /**
     * Get the currently logged in Firebase user UID.
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Fetch the User document from Firestore to determine role and details.
     */
    suspend fun getCurrentUser(): User? {
        val uid = getCurrentUserId() ?: return null
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        auth.signOut()
    }
}
