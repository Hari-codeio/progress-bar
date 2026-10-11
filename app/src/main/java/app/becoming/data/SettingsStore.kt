package app.becoming.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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

/** App settings. Only the greeting preference lives here; the user's name lives on the journey. */
class SettingsStore(context: Context) {
    private val store = context.applicationContext.settingsDataStore
    private val greetKey = stringPreferencesKey("greet_by")

    val greetBy: Flow<GreetBy> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> GreetBy.entries.firstOrNull { it.name == prefs[greetKey] } ?: GreetBy.IDENTITY }
        .distinctUntilChanged()

    suspend fun setGreetBy(value: GreetBy) {
        store.edit { it[greetKey] = value.name }
    }
}
