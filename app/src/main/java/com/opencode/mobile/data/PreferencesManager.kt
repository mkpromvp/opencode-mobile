package com.opencode.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "opencode_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_PASSWORD = stringPreferencesKey("password")
        private val KEY_LAST_SESSION_ID = stringPreferencesKey("last_session_id")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { it[KEY_SERVER_URL] ?: "" }
    val username: Flow<String> = context.dataStore.data.map { it[KEY_USERNAME] ?: "" }
    val password: Flow<String> = context.dataStore.data.map { it[KEY_PASSWORD] ?: "" }
    val lastSessionId: Flow<String> = context.dataStore.data.map { it[KEY_LAST_SESSION_ID] ?: "" }

    suspend fun saveConnection(url: String, user: String, pass: String) {
        context.dataStore.edit {
            it[KEY_SERVER_URL] = url
            it[KEY_USERNAME] = user
            it[KEY_PASSWORD] = pass
        }
    }

    suspend fun saveLastSessionId(sessionId: String) {
        context.dataStore.edit {
            it[KEY_LAST_SESSION_ID] = sessionId
        }
    }

    suspend fun clearConnection() {
        context.dataStore.edit {
            it.remove(KEY_SERVER_URL)
            it.remove(KEY_USERNAME)
            it.remove(KEY_PASSWORD)
            it.remove(KEY_LAST_SESSION_ID)
        }
    }
}
