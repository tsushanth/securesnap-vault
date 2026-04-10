package com.factory.securesnapvault.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.securesnapvault.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val enteredPin: String = "",
    val confirmPin: String = "",
    val isSettingUp: Boolean = false,
    val isConfirming: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isPinSet: StateFlow<Boolean> = preferencesManager.isPinSet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBiometricEnabled: StateFlow<Boolean> = preferencesManager.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onPinDigitEntered(digit: String) {
        val current = _uiState.value
        if (current.isConfirming) {
            if (current.confirmPin.length < 4) {
                _uiState.value = current.copy(
                    confirmPin = current.confirmPin + digit,
                    error = null
                )
                if (current.confirmPin.length + 1 == 4) {
                    val newConfirm = current.confirmPin + digit
                    if (newConfirm == current.enteredPin) {
                        viewModelScope.launch {
                            preferencesManager.setPin(newConfirm)
                            _uiState.value = _uiState.value.copy(isAuthenticated = true)
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            confirmPin = "",
                            enteredPin = "",
                            isConfirming = false,
                            error = "PINs don't match. Try again."
                        )
                    }
                }
            }
        } else {
            if (current.enteredPin.length < 4) {
                _uiState.value = current.copy(
                    enteredPin = current.enteredPin + digit,
                    error = null
                )
                if (current.enteredPin.length + 1 == 4) {
                    val fullPin = current.enteredPin + digit
                    if (current.isSettingUp) {
                        _uiState.value = _uiState.value.copy(isConfirming = true)
                    } else {
                        verifyPin(fullPin)
                    }
                }
            }
        }
    }

    fun onPinBackspace() {
        val current = _uiState.value
        if (current.isConfirming && current.confirmPin.isNotEmpty()) {
            _uiState.value = current.copy(confirmPin = current.confirmPin.dropLast(1))
        } else if (!current.isConfirming && current.enteredPin.isNotEmpty()) {
            _uiState.value = current.copy(enteredPin = current.enteredPin.dropLast(1))
        }
    }

    fun startSetup() {
        _uiState.value = AuthUiState(isSettingUp = true)
    }

    fun startLogin() {
        _uiState.value = AuthUiState(isSettingUp = false)
    }

    fun onBiometricSuccess() {
        _uiState.value = _uiState.value.copy(isAuthenticated = true)
    }

    fun onBiometricError(errorCode: Int, message: String) {
        val ignoredCodes = setOf(
            10, // ERROR_USER_CANCELED
            13  // ERROR_NEGATIVE_BUTTON ("Use PIN")
        )
        if (errorCode !in ignoredCodes && message.isNotBlank()) {
            _uiState.value = _uiState.value.copy(error = message)
        }
    }

    fun onBiometricFailed() {
        _uiState.value = _uiState.value.copy(error = "Fingerprint not recognized. Try again.")
    }

    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            val isValid = preferencesManager.verifyPin(pin)
            if (isValid) {
                _uiState.value = _uiState.value.copy(isAuthenticated = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    enteredPin = "",
                    error = "Incorrect PIN. Try again."
                )
            }
        }
    }
}
