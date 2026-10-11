package app.becoming.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.becoming.R
import app.becoming.domain.GreetBy
import app.becoming.domain.HomeState
import app.becoming.ui.theme.BecomingTheme

/** First run (and after completing a journey): create the identity and cast the first vote. */
@Composable
fun SetupScreen(
    suggestedName: String,
    initialGreetBy: GreetBy,
    showHistory: Boolean,
    onOpenHistory: () -> Unit,
    onStart: (IdentityDraft) -> Unit,
    importPhoto: suspend (Uri) -> String?,
    discardPhoto: (String?) -> Unit,
) {
    val c = BecomingTheme.colors
    IdentityForm(
        mode = IdentityFormMode.SETUP,
        initial = IdentityDraft(name = suggestedName, greetBy = initialGreetBy),
        onSubmit = onStart,
        onCancel = {},
        importPhoto = importPhoto,
        discardPhoto = discardPhoto,
        topBar = {
            Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End) {
                if (showHistory) {
                    IconButton(onClick = onOpenHistory) {
                        Icon(painterResource(R.drawable.ic_history), contentDescription = "History", tint = c.text)
                    }
                }
            }
        },
    )
}

/** Edit the active journey's name, skill, title, icon and greeting. Votes and dates are untouched. */
@Composable
fun EditIdentityScreen(
    state: HomeState,
    greetBy: GreetBy,
    onSave: (IdentityDraft) -> Unit,
    onCancel: () -> Unit,
    importPhoto: suspend (Uri) -> String?,
    discardPhoto: (String?) -> Unit,
) {
    IdentityForm(
        mode = IdentityFormMode.EDIT,
        initial = IdentityDraft(
            name = state.name,
            skill = state.skill,
            identityTitle = state.identityTitle,
            greetBy = greetBy,
            iconKey = state.iconKey,
            iconPath = state.iconPath,
        ),
        onSubmit = onSave,
        onCancel = onCancel,
        importPhoto = importPhoto,
        discardPhoto = discardPhoto,
    )
}
