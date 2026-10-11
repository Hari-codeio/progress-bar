package app.becoming.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.becoming.R
import app.becoming.domain.GreetBy
import app.becoming.domain.HabitRules
import app.becoming.ui.screen
import app.becoming.ui.theme.BecomingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    suggestedName: String,
    initialGreetBy: GreetBy,
    showHistory: Boolean,
    onOpenHistory: () -> Unit,
    onStart: (name: String, skill: String, identityTitle: String, greetBy: GreetBy) -> Unit,
) {
    val c = BecomingTheme.colors
    var name by rememberSaveable { mutableStateOf(suggestedName) }
    var skill by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var greetBy by rememberSaveable { mutableStateOf(initialGreetBy) }
    LaunchedEffect(suggestedName) { if (name.isBlank()) name = suggestedName }
    val ready = name.isNotBlank() && skill.isNotBlank() && title.isNotBlank()

    Column(
        Modifier
            .screen(c.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End) {
            if (showHistory) {
                IconButton(onClick = onOpenHistory) {
                    Icon(painterResource(R.drawable.ic_history), contentDescription = "History", tint = c.text)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Who are you becoming?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = c.text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Every day you show up is a vote for that person.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.muted,
        )
        Spacer(Modifier.height(28.dp))

        SetupField(value = name, onValueChange = { name = it }, label = "Your name", placeholder = null)
        SetupField(value = skill, onValueChange = { skill = it }, label = "What do you want to master?", placeholder = "e.g. Badminton")
        SetupField(value = title, onValueChange = { title = it }, label = "Call yourself", placeholder = "e.g. Badminton Player", last = true)

        Spacer(Modifier.height(20.dp))
        Text("Greet me as", style = MaterialTheme.typography.labelLarge, color = c.muted)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            GreetBy.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = greetBy == option,
                    onClick = { greetBy = option },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = GreetBy.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = c.violet,
                        activeContentColor = c.onViolet,
                        activeBorderColor = c.violet,
                        inactiveContainerColor = c.surface,
                        inactiveContentColor = c.muted,
                        inactiveBorderColor = c.borderStrong,
                    ),
                    label = { Text(if (option == GreetBy.IDENTITY) "Identity" else "Name") },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            HabitRules.greeting(greetBy, name.ifBlank { "…" }, title.ifBlank { "…" }),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = c.violetText,
        )

        Spacer(Modifier.height(20.dp))
        Text(
            "75 votes builds the habit. One vote a day. Missed days never take votes away.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.muted,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { onStart(name.trim(), skill.trim(), title.trim(), greetBy) },
            enabled = ready,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = c.violet,
                contentColor = c.onViolet,
                disabledContainerColor = c.disabled,
                disabledContentColor = c.disabledContent,
            ),
        ) {
            Text("Cast my first vote", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SetupField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String?,
    last: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = if (last) ImeAction.Done else ImeAction.Next,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    )
}
