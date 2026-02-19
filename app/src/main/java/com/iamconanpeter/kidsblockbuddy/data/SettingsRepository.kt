package com.iamconanpeter.kidsblockbuddy.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "kids_block_buddy_settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { pref ->
        AppSettings(
            largerControls = pref[Keys.LARGER_CONTROLS] ?: false,
            cameraSensitivity = pref[Keys.CAMERA_SENSITIVITY] ?: 5,
            blueprintAssist = pref[Keys.BLUEPRINT_ASSIST] ?: true,
            dailyChallengeMode = pref[Keys.DAILY_CHALLENGE_MODE] ?: false,
            feedbackCuesEnabled = pref[Keys.FEEDBACK_CUES_ENABLED] ?: true,
            sensoryCalmMode = pref[Keys.SENSORY_CALM_MODE] ?: false
        )
    }

    suspend fun setLargerControls(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.LARGER_CONTROLS] = enabled }
    }

    suspend fun setCameraSensitivity(value: Int) {
        context.settingsDataStore.edit { it[Keys.CAMERA_SENSITIVITY] = value.coerceIn(1, 10) }
    }

    suspend fun setBlueprintAssist(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.BLUEPRINT_ASSIST] = enabled }
    }

    suspend fun setDailyChallengeMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DAILY_CHALLENGE_MODE] = enabled }
    }

    suspend fun setFeedbackCuesEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.FEEDBACK_CUES_ENABLED] = enabled }
    }

    suspend fun setSensoryCalmMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SENSORY_CALM_MODE] = enabled }
    }

    private object Keys {
        val LARGER_CONTROLS = booleanPreferencesKey("larger_controls")
        val CAMERA_SENSITIVITY = intPreferencesKey("camera_sensitivity")
        val BLUEPRINT_ASSIST = booleanPreferencesKey("blueprint_assist")
        val DAILY_CHALLENGE_MODE = booleanPreferencesKey("daily_challenge_mode")
        val FEEDBACK_CUES_ENABLED = booleanPreferencesKey("feedback_cues_enabled")
        val SENSORY_CALM_MODE = booleanPreferencesKey("sensory_calm_mode")
    }
}

data class AppSettings(
    val largerControls: Boolean = false,
    val cameraSensitivity: Int = 5,
    val blueprintAssist: Boolean = true,
    val dailyChallengeMode: Boolean = false,
    val feedbackCuesEnabled: Boolean = true,
    val sensoryCalmMode: Boolean = false
)
