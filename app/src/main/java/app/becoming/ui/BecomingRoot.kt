package app.becoming.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.becoming.ui.screens.CelebrationScreen
import app.becoming.ui.screens.HistoryScreen
import app.becoming.ui.screens.HomeScreen
import app.becoming.ui.screens.SetupScreen
import app.becoming.ui.theme.BecomingTheme

private const val ROUTE_MAIN = "main"
private const val ROUTE_HISTORY = "history"

/**
 * "main" shows Setup, Home or Celebration depending on the journey state, so finishing a level or a
 * journey moves the user to the right screen without manual navigation. "history" is the Legacy screen.
 */
@Composable
fun BecomingRoot(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val home by vm.home.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val greetBy by vm.greetBy.collectAsStateWithLifecycle()
    val openHistory = { nav.navigate(ROUTE_HISTORY) }

    Surface(Modifier.fillMaxSize(), color = BecomingTheme.colors.background) {
        NavHost(navController = nav, startDestination = ROUTE_MAIN) {
            composable(ROUTE_MAIN) {
                when (val ui = home) {
                    HomeUi.Loading -> Box(Modifier.fillMaxSize())
                    HomeUi.NoJourney -> SetupScreen(
                        suggestedName = history.firstOrNull()?.journey?.name.orEmpty(),
                        initialGreetBy = greetBy,
                        showHistory = history.isNotEmpty(),
                        onOpenHistory = openHistory,
                        onStart = vm::startJourney,
                    )
                    is HomeUi.Active -> {
                        val state = ui.state
                        val finished = state.completedLevel
                        if (state.pendingLevelComplete && finished != null) {
                            CelebrationScreen(
                                state = state,
                                summary = finished,
                                onUpskill = vm::upskill,
                                onComplete = vm::complete,
                            )
                        } else {
                            HomeScreen(
                                state = state,
                                onVote = vm::vote,
                                onUndo = vm::undo,
                                onToggleGreet = vm::toggleGreet,
                                onOpenHistory = openHistory,
                            )
                        }
                    }
                }
            }
            composable(ROUTE_HISTORY) {
                HistoryScreen(journeys = history, onBack = { nav.popBackStack() })
            }
        }
    }
}
