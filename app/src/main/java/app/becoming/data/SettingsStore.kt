package app.becoming.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.becoming.domain.GreetBy
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** The stored theme choice, wrapped so "not loaded yet" (a null wrapper) differs from "follow system". */
data class ThemeSetting(val dark: Boolean?)

/** App settings: the greeting preference and the theme choice. The user's name lives on the journey. */
class SettingsStore(context: Context) {
    private val store = context.applicationContext.settingsDataStore
    private val greetKey = stringPreferencesKey("greet_by")
    private val darkKey = booleanPreferencesKey("dark_mode")

    private val prefs: Flow<Preferences> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val greetBy: Flow<GreetBy> = prefs
        .map { p -> GreetBy.entries.firstOrNull { it.name == p[greetKey] } ?: GreetBy.IDENTITY }
        .distinctUntilChanged()

    /** null (default) = follow the system; true/false = forced dark/light. */
    val darkMode: Flow<Boolean?> = prefs
        .map { p -> p[darkKey] }
        .distinctUntilChanged()

    val themeSetting: Flow<ThemeSetting> = darkMode.map { ThemeSetting(it) }

    suspend fun setGreetBy(value: GreetBy) {
        store.edit { it[greetKey] = value.name }
    }

    suspend fun setDarkMode(value: Boolean?) {
        store.edit { p ->
            p.remove(darkKey)
            if (value != null) p[darkKey] = value
        }
    }
}
