package com.mystockmanager.app.ui.login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mystockmanager.app.R
import com.mystockmanager.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val success = authRepository.login(email, password)
                if (success) {
                    _uiState.value = LoginUiState.Success
                } else {
                    _uiState.value = LoginUiState.Error(R.string.error_invalid_credentials)
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(R.string.error_generic)
            }
        }
    }

    fun register(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val recoveryCode = authRepository.register(email, password, firstName, lastName)
                _uiState.value = LoginUiState.RegisterSuccess(recoveryCode)
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(
                    if (e.message == "EMAIL_EXISTS") R.string.error_email_exists
                    else R.string.error_registration_failed
                )
            }
        }
    }

    fun recoverPassword(email: String, recoveryCode: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val success = authRepository.recoverPassword(email, recoveryCode, newPassword)
                _uiState.value = if (success) {
                    LoginUiState.Success
                } else {
                    LoginUiState.Error(R.string.error_recovery_invalid)
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(R.string.error_generic)
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class RegisterSuccess(val recoveryCode: String) : LoginUiState()
    data class Error(@StringRes val messageRes: Int) : LoginUiState()
}
