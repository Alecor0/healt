package com.healmate.domain.repository

import com.healmate.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface IAuthRepository {
    val currentUser: StateFlow<User?>
    val isLoggedIn: StateFlow<Boolean>
    fun login(email: String, pass: String): Boolean
    fun register(nombre: String, email: String, pass: String): Boolean
    fun logout()
    fun updateProfile(updatedUser: User)
}
