package com.qrhealthcare.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "qrhealth_session")

/**
 * Persists the logged-in user's data across app restarts.
 *
 * Stores: userId, email, fullName, role, and the JWT issued by the backend's
 * /auth/login or /auth/register endpoint. The JWT is hydrated into
 * ApiClient.authToken on app start so authenticated requests work after
 * a process kill.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_FULL_NAME = stringPreferencesKey("full_name")
        private val KEY_ADDRESS = stringPreferencesKey("address")
        private val KEY_ROLE = stringPreferencesKey("role")
        private val KEY_TOKEN = stringPreferencesKey("token") // JWT in production
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    suspend fun saveSession(
        userId: String,
        email: String,
        fullName: String,
        address: String,
        role: String,
        token: String  // JWT from POST /auth/login or /auth/register
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_EMAIL] = email
            prefs[KEY_FULL_NAME] = fullName
            prefs[KEY_ADDRESS] = address
            prefs[KEY_ROLE] = role
            prefs[KEY_TOKEN] = token
        }
    }

    /** Patch just the address field after a successful PUT /users/:id. */
    suspend fun saveAddress(address: String) {
        context.dataStore.edit { prefs -> prefs[KEY_ADDRESS] = address }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    val userId: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val email: Flow<String?> = context.dataStore.data.map { it[KEY_EMAIL] }
    val fullName: Flow<String?> = context.dataStore.data.map { it[KEY_FULL_NAME] }
    val address: Flow<String?> = context.dataStore.data.map { it[KEY_ADDRESS] }
    val role: Flow<String?> = context.dataStore.data.map { it[KEY_ROLE] }
    val token: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[KEY_USER_ID].isNullOrBlank()
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
