package app.becoming.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.becoming.R
import app.becoming.domain.GreetBy
import app.becoming.domain.HomeState
import app.becoming.domain.Levels
import app.becoming.domain.QUOTE_AUTHOR
import app.becoming.ui.LevelProgressBar
import app.becoming.ui.screen
import app.becoming.ui.theme.BecomingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    onVote: () -> Unit,
    onUndo: () -> Unit,
    onToggleGreet: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val c = BecomingTheme.colors
    Column(
        Modifier
            .screen(c.background)
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = onToggleGreet,
                label = {
                    Text(
                        if (state.greetBy == GreetBy.IDENTITY) "Greet: identity" else "Greet: name",
                        color = c.muted,
                    )
                },
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenHistory) {
                Icon(painterResource(R.drawable.ic_history), contentDescription = "History", tint = c.text)
            }
        }

        // Middle content: vertically centred when it fits, scrollable on short screens.
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val viewportHeight = maxHeight
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = viewportHeight)
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    HomeContent(state)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onUndo,
                enabled = state.canUndo,
                modifier = Modifier.weight(1f).height(64.dp),
                border = BorderStroke(1.dp, if (state.canUndo) c.borderStrong else c.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = c.text,
                    disabledContentColor = c.disabledContent,
                ),
            ) {
                Icon(painterResource(R.drawable.ic_minus), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Undo today", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onVote,
                enabled = state.canVote,
                modifier = Modifier.weight(1.4f).height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.violet,
                    contentColor = c.onViolet,
                    disabledContainerColor = c.disabled,
                    disabledContentColor = c.disabledContent,
                ),
            ) {
                val voted = state.votedToday
                Icon(
                    painterResource(if (voted) R.drawable.ic_check else R.drawable.ic_plus),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(if (voted) "Voted today" else "Vote", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HomeContent(state: HomeState) {
    val c = BecomingTheme.colors
    Text(
        state.greeting,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = c.text,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(8.dp))
    Text(state.levelLabel, style = MaterialTheme.typography.titleMedium, color = c.violetText)
    Spacer(Modifier.height(16.dp))
    LevelSteps(level = state.level, levelComplete = state.pendingLevelComplete)

    Spacer(Modifier.height(28.dp))
    Text(
        "${state.progressPercent}%",
        style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, letterSpacing = (-1).sp),
        fontWeight = FontWeight.Bold,
        color = c.text,
    )
    Spacer(Modifier.height(12.dp))
    LevelProgressBar(fraction = state.progress, pct = state.progressPercent)
    Spacer(Modifier.height(10.dp))
    Text(state.homeProgressLabel, style = MaterialTheme.typography.bodyMedium, color = c.muted)
    Spacer(Modifier.height(20.dp))
    Text(
        state.message,
        style = MaterialTheme.typography.bodyLarge,
        // Violet: the gentle "never miss twice" nudge.
        color = if (state.isNudge) c.violetText else c.text,
    )
    Spacer(Modifier.height(16.dp))
    QuoteCard(state.quote)
}

/** Today's quote: italic text plus attribution, on a neutral card. */
@Composable
fun QuoteCard(quote: String, modifier: Modifier = Modifier) {
    val c = BecomingTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .padding(16.dp),
    ) {
        Text(
            "“$quote”",
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            color = c.text,
        )
        Spacer(Modifier.height(6.dp))
        Text("— $QUOTE_AUTHOR", style = MaterialTheme.typography.labelLarge, color = c.muted)
    }
}

/** Five segments, one per level: completed = lime (win), current = violet, future = track. */
@Composable
private fun LevelSteps(level: Int, levelComplete: Boolean) {
    val c = BecomingTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (l in 1..Levels.MAX_LEVEL) {
            val done = l < level || (l == level && levelComplete)
            val current = l == level && !levelComplete
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            when {
                                done -> c.lime
                                current -> c.violet
                                else -> c.track
                            },
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    Levels.name(l),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (done || current) c.text else c.muted,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}
