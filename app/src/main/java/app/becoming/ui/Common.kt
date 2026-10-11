package app.becoming.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.becoming.domain.progressColor
import app.becoming.ui.theme.BecomingTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Full-screen page background that stays clear of system bars, cutouts and the keyboard. */
fun Modifier.screen(color: Color): Modifier =
    fillMaxSize().background(color).safeDrawingPadding()

/** Rounded progress bar whose fill shifts green -> bright green -> lime with the level percent. */
@Composable
fun LevelProgressBar(fraction: Float, pct: Int, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val c = BecomingTheme.colors
    val shape = RoundedCornerShape(percent = 50)
    val animatedFraction by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "progressFraction")
    val fill by animateColorAsState(Color(progressColor(pct, c.isDark)), label = "progressColor")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(c.track),
    ) {
        if (animatedFraction > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(shape)
                    .background(fill),
            )
        }
    }
}

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

fun formatDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(DAY_FORMAT)
