package com.rajratna.events.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rajratna.events.RajratnaApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    
    private val auth = FirebaseAuth.getInstance()
    private val authRepository = (application as RajratnaApp).authRepository
    
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _state.update { it.copy(error = "Email and Password cannot be empty") }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    _state.update { 
                        it.copy(isLoading = false, error = task.exception?.message ?: "Login failed") 
                    }
                }
            }
    }
}
