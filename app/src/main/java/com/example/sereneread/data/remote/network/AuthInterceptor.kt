package com.example.sereneread.data.remote.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // Ambil token dari penyimpanan
        val token = tokenManager.getToken()

        // Jika token ada, sisipkan ke Header
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())    

        // DETEKSI TOKEN HABIS: Jika server menolak (401), picu alarm!
        if (response.code == 401) {
            tokenManager.triggerSessionExpired()
        }

        return response
    }
}