package com.example.sereneread.domain.repository

import com.example.sereneread.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun login(email: String, password: String): Flow<Resource<Unit>>
}