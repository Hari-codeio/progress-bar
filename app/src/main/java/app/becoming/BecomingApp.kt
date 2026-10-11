package app.becoming

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.updateAll
import app.becoming.data.BecomingDatabase
import app.becoming.data.HabitRepository
import app.becoming.data.IdentityPhotoStore
import app.becoming.data.SettingsStore
import app.becoming.widget.BecomingWidget
import app.becoming.work.MidnightWorker

/** Manual dependency injection: one instance of each collaborator per process. */
class AppContainer(private val context: Context) {
    val database: BecomingDatabase by lazy { BecomingDatabase.build(context) }
    val settings: SettingsStore by lazy { SettingsStore(context) }
    val photos: IdentityPhotoStore by lazy { IdentityPhotoStore(context) }
    val repository: HabitRepository by lazy { HabitRepository(database, settings, photos) }

    /** Re-render every placed widget after a change made inside the app. */
    suspend fun refreshWidgets() {
        BecomingWidget().updateAll(context)
    }
}

class BecomingApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        MidnightWorker.schedule(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as BecomingApp).container
