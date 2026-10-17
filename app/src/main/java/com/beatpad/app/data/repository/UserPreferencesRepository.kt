package com.beatpad.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "beatpad_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val LAST_PACK_ID = stringPreferencesKey("last_pack_id")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val QUANTIZE_DEFAULT = booleanPreferencesKey("quantize_default")
        val LATENCY_OFFSET_MS = intPreferencesKey("latency_offset_ms")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val lastPackId: Flow<String> = context.userDataStore.data.map {
        it[PreferencesKeys.LAST_PACK_ID] ?: "trap"
    }

    val hapticsEnabled: Flow<Boolean> = context.userDataStore.data.map {
        it[PreferencesKeys.HAPTICS_ENABLED] ?: true
    }

    val quantizeDefault: Flow<Boolean> = context.userDataStore.data.map {
        it[PreferencesKeys.QUANTIZE_DEFAULT] ?: true
    }

    val latencyOffsetMs: Flow<Int> = context.userDataStore.data.map {
        it[PreferencesKeys.LATENCY_OFFSET_MS] ?: 0
    }

    val appLanguage: Flow<String> = context.userDataStore.data.map {
        it[PreferencesKeys.APP_LANGUAGE] ?: "SYSTEM"
    }

    suspend fun setLastPackId(packId: String) {
        context.userDataStore.edit { it[PreferencesKeys.LAST_PACK_ID] = packId }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.userDataStore.edit { it[PreferencesKeys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setQuantizeDefault(enabled: Boolean) {
        context.userDataStore.edit { it[PreferencesKeys.QUANTIZE_DEFAULT] = enabled }
    }

    suspend fun setLatencyOffsetMs(offsetMs: Int) {
        context.userDataStore.edit { it[PreferencesKeys.LATENCY_OFFSET_MS] = offsetMs.coerceIn(-100, 100) }
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.userDataStore.edit { it[PreferencesKeys.APP_LANGUAGE] = languageCode }
    }
}
