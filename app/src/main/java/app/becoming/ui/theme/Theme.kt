package app.becoming.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 60-30-10 palette: neutrals 60%, violet 30% (identity and actions), green to lime 10% (progress
 * fills via progressColor, and lime for wins). The widget uses the same hex values (WidgetColors and
 * the colors.xml files under res/values and res/values-night).
 */
@Immutable
data class BecomingColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val border: Color,
    val borderStrong: Color,
    val track: Color,
    val text: Color,
    val muted: Color,
    /** + button, primary buttons, segmented selection, current level, Level up. */
    val violet: Color,
    val onViolet: Color,
    /** Violet for text and the "never miss twice" nudge. */
    val violetText: Color,
    val identityTile: Color,
    val identityIcon: Color,
    val disabled: Color,
    val disabledContent: Color,
    /** Wins only: completed levels, celebration badge, level-complete widget state. */
    val lime: Color,
    val onLime: Color,
    val limeText: Color,
    val heatVoted: Color,
    /** Celebration badge rings (outer, inner). */
    val ringOuter: Color,
    val ringInner: Color,
)

val NightColors = BecomingColors(
    isDark = true,
    background = Color(0xFF0D0E12),
    surface = Color(0xFF16181F),
    border = Color(0xFF262933),
    borderStrong = Color(0xFF383C48),
    track = Color(0xFF23262F),
    text = Color(0xFFF7F7F8),
    muted = Color(0xFFA1A1AA),
    violet = Color(0xFF7050F5),
    onViolet = Color(0xFFFFFFFF),
    violetText = Color(0xFFA28BFF),
    identityTile = Color(0xFF221A40),
    identityIcon = Color(0xFFA28BFF),
    disabled = Color(0xFF262933),
    disabledContent = Color(0xFF5A5D68),
    lime = Color(0xFFC2F542),
    onLime = Color(0xFF0D0E12),
    limeText = Color(0xFFC2F542),
    heatVoted = Color(0xFF91EB48),
    ringOuter = Color(0xFF2A2150),
    ringInner = Color(0xFF4B3A99),
)

val DayColors = BecomingColors(
    isDark = false,
    background = Color(0xFFF7F7F9),
    surface = Color(0xFFFFFFFF),
    border = Color(0xFFE4E4E9),
    borderStrong = Color(0xFFD0D0D8),
    track = Color(0xFFE9E9EE),
    text = Color(0xFF0D0E12),
    muted = Color(0xFF5B5B66),
    violet = Color(0xFF6A45F0),
    onViolet = Color(0xFFFFFFFF),
    violetText = Color(0xFF5B3BE0),
    identityTile = Color(0xFFEEE9FF),
    identityIcon = Color(0xFF6A45F0),
    disabled = Color(0xFFEFEFF3),
    disabledContent = Color(0xFFA1A1AA),
    lime = Color(0xFFC2F542),
    onLime = Color(0xFF0D0E12),
    limeText = Color(0xFF4D7A00),
    heatVoted = Color(0xFF4CC25A),
    // Light-mode ring tints (not in the palette spec): lighter violets so the badge is not heavy on white.
    ringOuter = Color(0xFFE6DFFF),
    ringInner = Color(0xFFBBA9FF),
)

private val LocalBecomingColors = staticCompositionLocalOf { NightColors }

@Composable
fun BecomingTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) NightColors else DayColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.violet, onPrimary = c.onViolet,
            secondary = c.violet, onSecondary = c.onViolet,
            secondaryContainer = c.identityTile, onSecondaryContainer = c.text,
            background = c.background, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.track, onSurfaceVariant = c.muted,
            outline = c.borderStrong, outlineVariant = c.border,
        )
    } else {
        lightColorScheme(
            primary = c.violet, onPrimary = c.onViolet,
            secondary = c.violet, onSecondary = c.onViolet,
            secondaryContainer = c.identityTile, onSecondaryContainer = c.text,
            background = c.background, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.track, onSurfaceVariant = c.muted,
            outline = c.borderStrong, outlineVariant = c.border,
        )
    }
    CompositionLocalProvider(LocalBecomingColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

object BecomingTheme {
    val colors: BecomingColors
        @Composable @ReadOnlyComposable get() = LocalBecomingColors.current
}
