package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_DEFAULT_FOLDER = longPreferencesKey("default_folder_id")
        val KEY_DEFAULT_TAGS = stringPreferencesKey("default_tags")
        val KEY_DUPLICATE_BEHAVIOR = stringPreferencesKey("duplicate_behavior") // skip, keep_both, ask
        val KEY_QUICK_SAVE = booleanPreferencesKey("quick_save_enabled")
        val KEY_SERVER_PORT = intPreferencesKey("server_port")
        val KEY_SERVER_PIN = stringPreferencesKey("server_pin")
        val KEY_READ_ONLY = booleanPreferencesKey("read_only_mode")
        val KEY_ALLOW_UPLOADS = booleanPreferencesKey("allow_uploads")
        val KEY_APP_THEME = stringPreferencesKey("app_theme") // system, light, dark
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    val serverPort: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_PORT] ?: 8080
    }

    val serverPin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_PIN] ?: generatePin()
    }

    val readOnlyMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_READ_ONLY] ?: false
    }

    val allowUploads: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ALLOW_UPLOADS] ?: true
    }

    val duplicateBehavior: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DUPLICATE_BEHAVIOR] ?: "skip"
    }

    val quickSaveEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_QUICK_SAVE] ?: true
    }

    val appTheme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_THEME] ?: "system"
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: true
    }

    suspend fun setServerPort(port: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_SERVER_PORT] = port }
    }

    suspend fun setServerPin(pin: String) {
        context.dataStore.edit { prefs -> prefs[KEY_SERVER_PIN] = pin }
    }

    suspend fun regeneratePin(): String {
        val newPin = generatePin()
        setServerPin(newPin)
        return newPin
    }

    suspend fun setReadOnlyMode(readOnly: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_READ_ONLY] = readOnly }
    }

    suspend fun setAllowUploads(allow: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_ALLOW_UPLOADS] = allow }
    }

    suspend fun setDuplicateBehavior(behavior: String) {
        context.dataStore.edit { prefs -> prefs[KEY_DUPLICATE_BEHAVIOR] = behavior }
    }

    suspend fun setQuickSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_QUICK_SAVE] = enabled }
    }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { prefs -> prefs[KEY_APP_THEME] = theme }
    }

    suspend fun setDynamicColor(dynamic: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_DYNAMIC_COLOR] = dynamic }
    }

    private fun generatePin(): String {
        val number = Random.nextInt(100000, 999999)
        return number.toString()
    }
}
