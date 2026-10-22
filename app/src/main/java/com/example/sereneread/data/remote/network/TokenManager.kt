package com.example.sereneread.data.remote.network

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("serene_prefs", Context.MODE_PRIVATE)
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvent = _sessionExpiredEvent.asSharedFlow()

    fun saveToken(token: String) {
        prefs.edit().putString("JWT_TOKEN", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("JWT_TOKEN", null)
    }

    fun clearToken() {
        prefs.edit().remove("JWT_TOKEN").apply()
    }
    
    // Email akun yang terakhir login, untuk mendeteksi pergantian akun
    fun saveAccount(email: String) {
        prefs.edit().putString("ACCOUNT_EMAIL", email).apply()
    }

    fun getAccount(): String? {
        return prefs.getString("ACCOUNT_EMAIL", null)
    }

    fun clearAccount() {
        prefs.edit().remove("ACCOUNT_EMAIL").apply()
    }

    fun triggerSessionExpired() {
        clearToken() // Hapus token yang sudah basi
        _sessionExpiredEvent.tryEmit(Unit) // Bunyikan alarm!
    }
}