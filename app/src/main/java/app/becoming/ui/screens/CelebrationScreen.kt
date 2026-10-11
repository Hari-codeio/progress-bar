package app.becoming.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.becoming.domain.HomeState
import app.becoming.domain.LevelSummary
import app.becoming.domain.Levels
import app.becoming.domain.QUOTES
import app.becoming.ui.IdentityAvatar
import app.becoming.ui.screen
import app.becoming.ui.theme.BecomingTheme
import java.util.concurrent.TimeUnit
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter

@Composable
fun CelebrationScreen(
    state: HomeState,
    summary: LevelSummary,
    onUpskill: () -> Unit,
    onComplete: () -> Unit,
) {
    val c = BecomingTheme.colors
    var confirmComplete by rememberSaveable { mutableStateOf(false) }
    val parties = remember {
        listOf(
            Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                colors = listOf(0xFFC2F542, 0xFF7050F5, 0xFFA28BFF, 0xFF91EB48, 0xFFF7F7F8).map { it.toInt() },
                emitter = Emitter(duration = 120, TimeUnit.MILLISECONDS).max(140),
                position = Position.Relative(0.5, 0.25),
            ),
        )
    }

    Box(Modifier.screen(c.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            RingBadge(iconKey = state.displayIconKey, photoPath = state.iconPath)
            Spacer(Modifier.height(24.dp))
            Text(
                "Level ${summary.level} complete",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = c.limeText,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                Levels.youAre(summary.title),
                style = MaterialTheme.typography.titleMedium,
                color = c.violetText,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Votes", summary.votes)
                Stat("Days", summary.days)
                Stat("Missed", summary.missedDays)
            }
            Spacer(Modifier.height(24.dp))
            // The book's core line, fixed here (the Home card rotates daily).
            QuoteCard(QUOTES.first())
            Spacer(Modifier.height(28.dp))

            val violetButton = ButtonDefaults.buttonColors(containerColor = c.violet, contentColor = c.onViolet)
            if (!state.isFinalLevel) {
                val next = summary.level + 1
                Button(
                    onClick = onUpskill,
                    colors = violetButton,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Upskill to ${Levels.name(next)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Level $next · ${Levels.levelSize(next)} more votes",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { confirmComplete = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    border = BorderStroke(1.dp, c.borderStrong),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = c.text),
                ) {
                    Text("Complete my journey", fontWeight = FontWeight.SemiBold)
                }
            } else {
                // Level 5 (day 365): the journey is finished, so Complete is the only choice.
                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    border = BorderStroke(1.dp, c.borderStrong),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = c.text),
                ) {
                    Text("Complete my journey", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        KonfettiView(modifier = Modifier.fillMaxSize(), parties = parties)
    }

    if (confirmComplete) {
        AlertDialog(
            onDismissRequest = { confirmComplete = false },
            title = { Text("Complete this journey?") },
            text = {
                Text("You finish as ${Levels.article(summary.title)} ${summary.title}. Your history is kept and you can start a new identity.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmComplete = false
                    onComplete()
                }) { Text("Complete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmComplete = false }) { Text("Keep going") }
            },
        )
    }
}

/** Concentric violet rings around a lime (win) disc with the identity icon (dark) or photo. */
@Composable
private fun RingBadge(iconKey: String?, photoPath: String?) {
    val c = BecomingTheme.colors
    Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            drawCircle(color = c.ringOuter, radius = radius)
            drawCircle(color = c.ringInner, radius = radius * 0.76f)
            drawCircle(color = c.lime, radius = radius * 0.52f)
        }
        IdentityAvatar(iconKey = iconKey, photoPath = photoPath, size = 80.dp, tile = c.lime, tint = c.onLime)
    }
}

@Composable
private fun RowScope.Stat(label: String, value: Int) {
    val c = BecomingTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .weight(1f)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = c.text)
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.muted)
    }
}
