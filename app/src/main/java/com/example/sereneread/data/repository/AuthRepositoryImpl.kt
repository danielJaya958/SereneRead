package com.example.sereneread.data.repository

import com.example.sereneread.data.remote.LoginRequest
import com.example.sereneread.data.remote.SereneReadApi
import com.example.sereneread.data.remote.network.TokenManager
import com.example.sereneread.domain.repository.AuthRepository
import com.example.sereneread.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: SereneReadApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override fun login(email: String, password: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading()) // Beri tahu UI untuk menampilkan loading

        try {
            // Panggil API Golang
            val request = LoginRequest(email, password)
            val response = api.login(request)

            // Cek status dari response JSON Golang Anda
            if (response.status && response.data != null) {
                // Simpan token ke SharedPreferences
                tokenManager.saveToken(response.data.token)
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "Login gagal"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Terjadi kesalahan jaringan: ${e.message}"))
        }
    }
}