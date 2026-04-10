package com.factory.securesnapvault.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.securesnapvault.util.FileManager
import com.factory.securesnapvault.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isChangingPin: Boolean = false,
    val currentPinInput: String = "",
    val newPinInput: String = "",
    val confirmPinInput: String = "",
    val pinChangeStep: Int = 0,
    val pinChangeError: String? = null,
    val pinChangeSuccess: Boolean = false,
    val storageUsed: Long = 0
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val fileManager = FileManager(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val isBiometricEnabled: StateFlow<Boolean> = preferencesManager.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val deleteOriginal: StateFlow<Boolean> = preferencesManager.deleteOriginalAfterImport
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                storageUsed = fileManager.getVaultStorageUsed()
            )
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBiometricEnabled(enabled)
        }
    }

    fun toggleDeleteOriginal(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDeleteOriginal(enabled)
        }
    }

    fun startPinChange() {
        _uiState.value = _uiState.value.copy(
            isChangingPin = true,
            pinChangeStep = 0,
            currentPinInput = "",
            newPinInput = "",
            confirmPinInput = "",
            pinChangeError = null,
            pinChangeSuccess = false
        )
    }

    fun onPinChangeDigit(digit: String) {
        val current = _uiState.value
        when (current.pinChangeStep) {
            0 -> {
                if (current.currentPinInput.length < 4) {
                    val newInput = current.currentPinInput + digit
                    _uiState.value = current.copy(currentPinInput = newInput, pinChangeError = null)
                    if (newInput.length == 4) {
                        verifyCurrentPin(newInput)
                    }
                }
            }
            1 -> {
                if (current.newPinInput.length < 4) {
                    val newInput = current.newPinInput + digit
                    _uiState.value = current.copy(newPinInput = newInput, pinChangeError = null)
                    if (newInput.length == 4) {
                        _uiState.value = _uiState.value.copy(pinChangeStep = 2)
                    }
                }
            }
            2 -> {
                if (current.confirmPinInput.length < 4) {
                    val newInput = current.confirmPinInput + digit
                    _uiState.value = current.copy(confirmPinInput = newInput, pinChangeError = null)
                    if (newInput.length == 4) {
                        confirmNewPin(newInput)
                    }
                }
            }
        }
    }

    fun onPinChangeBackspace() {
        val current = _uiState.value
        when (current.pinChangeStep) {
            0 -> {
                if (current.currentPinInput.isNotEmpty()) {
                    _uiState.value = current.copy(currentPinInput = current.currentPinInput.dropLast(1))
                }
            }
            1 -> {
                if (current.newPinInput.isNotEmpty()) {
                    _uiState.value = current.copy(newPinInput = current.newPinInput.dropLast(1))
                }
            }
            2 -> {
                if (current.confirmPinInput.isNotEmpty()) {
                    _uiState.value = current.copy(confirmPinInput = current.confirmPinInput.dropLast(1))
                }
            }
        }
    }

    fun cancelPinChange() {
        _uiState.value = _uiState.value.copy(
            isChangingPin = false,
            pinChangeStep = 0,
            currentPinInput = "",
            newPinInput = "",
            confirmPinInput = "",
            pinChangeError = null
        )
    }

    fun dismissPinChangeSuccess() {
        _uiState.value = _uiState.value.copy(pinChangeSuccess = false)
    }

    private fun verifyCurrentPin(pin: String) {
        viewModelScope.launch {
            val isValid = preferencesManager.verifyPin(pin)
            if (isValid) {
                _uiState.value = _uiState.value.copy(pinChangeStep = 1)
            } else {
                _uiState.value = _uiState.value.copy(
                    currentPinInput = "",
                    pinChangeError = "Incorrect current PIN"
                )
            }
        }
    }

    private fun confirmNewPin(confirmPin: String) {
        val newPin = _uiState.value.newPinInput
        if (confirmPin == newPin) {
            viewModelScope.launch {
                preferencesManager.setPin(newPin)
                _uiState.value = _uiState.value.copy(
                    isChangingPin = false,
                    pinChangeSuccess = true,
                    pinChangeStep = 0,
                    currentPinInput = "",
                    newPinInput = "",
                    confirmPinInput = ""
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(
                confirmPinInput = "",
                pinChangeError = "PINs don't match. Try again.",
                pinChangeStep = 1,
                newPinInput = ""
            )
        }
    }
}
