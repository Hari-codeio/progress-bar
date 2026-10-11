package app.becoming.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.becoming.BecomingApp
import app.becoming.data.JourneyHistory
import app.becoming.domain.GreetBy
import app.becoming.domain.HomeState
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
    fun startJourney(name: String, skill: String, identityTitle: String, greetBy: GreetBy) = mutate {
        repository.setGreetBy(greetBy)
        repository.startJourney(name, skill, identityTitle, castFirstVote = true)
    }

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
