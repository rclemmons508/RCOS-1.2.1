package com.rcsolutions.rcosmobile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "rcos_preferences")

data class LocalPrefsState(
    val activeBusinessId: String = "",
    val biometricsEnabled: Boolean = false,
    val focusQuote: String = RcosDefaults.FOCUS_QUOTES.first(),
    val weatherCity: String = "",
    val pushAlertsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val automationMasterEnabled: Boolean = true,
    val disabledAutomationRuleIds: Set<String> = emptySet(),
    val customDocKnowledge: String = "",
)

class UserPreferencesRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val ACTIVE_BUSINESS_ID = stringPreferencesKey("active_business_id")
        val BIOMETRICS_ENABLED = booleanPreferencesKey("biometrics_enabled")
        val FOCUS_QUOTE = stringPreferencesKey("focus_quote")
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val PUSH_ALERTS_ENABLED = booleanPreferencesKey("push_alerts_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val AUTOMATION_MASTER = booleanPreferencesKey("automation_master_enabled")
        val DISABLED_RULES = stringPreferencesKey("disabled_automation_rule_ids")
        val CUSTOM_DOC_KNOWLEDGE = stringPreferencesKey("custom_doc_knowledge")
    }

    val prefsFlow: Flow<LocalPrefsState> = store.data.map { prefs ->
        val rawDisabled = prefs[Keys.DISABLED_RULES].orEmpty()
        LocalPrefsState(
            activeBusinessId = prefs[Keys.ACTIVE_BUSINESS_ID].orEmpty(),
            biometricsEnabled = prefs[Keys.BIOMETRICS_ENABLED] ?: false,
            focusQuote = prefs[Keys.FOCUS_QUOTE] ?: RcosDefaults.FOCUS_QUOTES.first(),
            weatherCity = prefs[Keys.WEATHER_CITY].orEmpty(),
            pushAlertsEnabled = prefs[Keys.PUSH_ALERTS_ENABLED] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            quietHoursEnabled = prefs[Keys.QUIET_HOURS_ENABLED] ?: false,
            automationMasterEnabled = prefs[Keys.AUTOMATION_MASTER] ?: true,
            disabledAutomationRuleIds = rawDisabled.split(",").filter { it.isNotBlank() }.toSet(),
            customDocKnowledge = prefs[Keys.CUSTOM_DOC_KNOWLEDGE].orEmpty(),
        )
    }

    suspend fun setActiveBusinessId(id: String) {
        store.edit { it[Keys.ACTIVE_BUSINESS_ID] = id }
    }

    suspend fun setBiometricsEnabled(enabled: Boolean) {
        store.edit { it[Keys.BIOMETRICS_ENABLED] = enabled }
    }

    suspend fun setFocusQuote(quote: String) {
        store.edit { it[Keys.FOCUS_QUOTE] = quote.trim().take(200) }
    }

    suspend fun setWeatherCity(city: String) {
        store.edit { it[Keys.WEATHER_CITY] = city.trim().take(80) }
    }

    suspend fun setNotificationSettings(
        pushAlerts: Boolean,
        sound: Boolean,
        vibration: Boolean,
        quietHours: Boolean
    ) {
        store.edit {
            it[Keys.PUSH_ALERTS_ENABLED] = pushAlerts
            it[Keys.SOUND_ENABLED] = sound
            it[Keys.VIBRATION_ENABLED] = vibration
            it[Keys.QUIET_HOURS_ENABLED] = quietHours
        }
    }

    suspend fun setAutomationMaster(enabled: Boolean) {
        store.edit { it[Keys.AUTOMATION_MASTER] = enabled }
    }

    suspend fun toggleAutomationRule(ruleId: String, enabled: Boolean) {
        store.edit { prefs ->
            val current = prefs[Keys.DISABLED_RULES].orEmpty()
                .split(",").filter { it.isNotBlank() }.toMutableSet()
            if (enabled) current.remove(ruleId) else current.add(ruleId)
            prefs[Keys.DISABLED_RULES] = current.joinToString(",")
        }
    }

    suspend fun setCustomDocKnowledge(text: String) {
        store.edit { it[Keys.CUSTOM_DOC_KNOWLEDGE] = text.take(8000) }
    }

    suspend fun clearAllOnSignOut() {
        store.edit {
            it.remove(Keys.ACTIVE_BUSINESS_ID)
            it.remove(Keys.BIOMETRICS_ENABLED)
        }
    }
}
