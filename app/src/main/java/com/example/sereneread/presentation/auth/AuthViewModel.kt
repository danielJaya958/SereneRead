package com.example.sereneread.presentation.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sereneread.domain.repository.AuthRepository
import com.example.sereneread.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableLiveData<Resource<Unit>>()
    val loginState: LiveData<Resource<Unit>> = _loginState

    fun login(email: String, password: String) {
        // Validasi input kosong
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = Resource.Error("Email dan Password tidak boleh kosong")
            return
        }

        // Eksekusi proses login ke Repository
        authRepository.login(email, password).onEach { result ->
            _loginState.value = result
        }.launchIn(viewModelScope)
    }
}