package app.becoming.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.becoming.BecomingApp
import app.becoming.data.JourneyHistory
import app.becoming.domain.GreetBy
import app.becoming.domain.HomeState
import app.becoming.ui.screens.IdentityDraft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUi {
    data object Loading : HomeUi
    data object NoJourney : HomeUi
    data class Active(val state: HomeState) : HomeUi
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = getApplication<BecomingApp>().container
    private val repository = container.repository

    val home: StateFlow<HomeUi> = repository.homeState
        .map { state -> if (state == null) HomeUi.NoJourney else HomeUi.Active(state) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUi.Loading)

    val history: StateFlow<List<JourneyHistory>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val greetBy: StateFlow<GreetBy> = repository.greetBy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GreetBy.IDENTITY)

    /** "Cast my first vote": saves the greeting choice, creates the journey and casts today's vote. */
    fun startJourney(draft: IdentityDraft) = mutate {
        repository.setGreetBy(draft.greetBy)
        repository.startJourney(
            draft.name, draft.skill, draft.identityTitle, draft.iconKey, draft.iconPath, castFirstVote = true,
        )
    }

    /** Edit identity: name, skill, title (past badge titles follow), icon/photo and greeting. */
    fun saveIdentity(draft: IdentityDraft) = mutate {
        repository.setGreetBy(draft.greetBy)
        repository.updateIdentity(draft.name, draft.skill, draft.identityTitle, draft.iconKey, draft.iconPath)
    }

    /** Copies a picked image into a pending 256 px square PNG; returns its path or null on failure. */
    suspend fun importPhoto(uri: Uri): String? = container.photos.importPending(uri)

    /** Deletes a photo only if it is an unsaved (pending) pick. */
    fun discardPendingPhoto(path: String?) = container.photos.deleteIfPending(path)

    /** Stores an explicit theme; the app and the widget both follow it. */
    fun setDarkMode(dark: Boolean) = mutate { repository.setDarkMode(dark) }

    fun vote() = mutate { repository.voteToday() }

    fun undo() = mutate { repository.undoToday() }

    fun upskill() = mutate { repository.upskill() }

    fun complete() = mutate { repository.completeJourney() }

    fun toggleGreet() = mutate {
        val current = repository.greetBy.first()
        repository.setGreetBy(if (current == GreetBy.IDENTITY) GreetBy.NAME else GreetBy.IDENTITY)
    }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            container.refreshWidgets()
        }
    }
}
