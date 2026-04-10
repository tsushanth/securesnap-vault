package com.factory.securesnapvault.billing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.premiumDataStore by preferencesDataStore(name = "premium_prefs")

class PremiumManager(private val context: Context) {

    companion object {
        private val IS_PREMIUM_KEY = booleanPreferencesKey("is_premium")
    }

    val isPremium: Flow<Boolean> = context.premiumDataStore.data.map { prefs ->
        prefs[IS_PREMIUM_KEY] ?: false
    }

    suspend fun setPremium(premium: Boolean) {
        context.premiumDataStore.edit { prefs ->
            prefs[IS_PREMIUM_KEY] = premium
        }
    }

    suspend fun isPremiumSync(): Boolean {
        return context.premiumDataStore.data.first()[IS_PREMIUM_KEY] ?: false
    }
}
