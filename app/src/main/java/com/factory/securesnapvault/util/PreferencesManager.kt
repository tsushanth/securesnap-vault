package com.factory.securesnapvault.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vault_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        private val PIN_SET_KEY = booleanPreferencesKey("pin_set")
        private val DELETE_ORIGINAL_KEY = booleanPreferencesKey("delete_original")
    }

    val isPinSet: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PIN_SET_KEY] ?: false
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED_KEY] ?: false
    }

    val deleteOriginalAfterImport: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DELETE_ORIGINAL_KEY] ?: false
    }

    suspend fun setPin(pin: String) {
        val hash = hashPin(pin)
        context.dataStore.edit { prefs ->
            prefs[PIN_HASH_KEY] = hash
            prefs[PIN_SET_KEY] = true
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val storedHash = prefs[PIN_HASH_KEY] ?: ""
        return storedHash == hashPin(pin)
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }

    suspend fun setDeleteOriginal(delete: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DELETE_ORIGINAL_KEY] = delete
        }
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
