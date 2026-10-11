package app.becoming.widget

import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.becoming.MainActivity
import app.becoming.R
import app.becoming.appContainer
import app.becoming.domain.HomeState
import app.becoming.domain.SubtitleKind
import app.becoming.domain.progressColor
import kotlinx.coroutines.flow.first
import androidx.glance.color.ColorProvider as DayNight

/** Widget palette: the same hex values as the in-app theme, resolved by the launcher's day/night mode. */
private object WidgetColors {
    val surface = DayNight(day = Color(0xFFFFFFFF), night = Color(0xFF16181F))
    val border = DayNight(day = Color(0xFFE4E4E9), night = Color(0xFF262933))
    val borderStrong = DayNight(day = Color(0xFFD0D0D8), night = Color(0xFF383C48))
    val track = DayNight(day = Color(0xFFE9E9EE), night = Color(0xFF23262F))
    val text = DayNight(day = Color(0xFF0D0E12), night = Color(0xFFF7F7F8))
    val muted = DayNight(day = Color(0xFF5B5B66), night = Color(0xFFA1A1AA))
    val violet = DayNight(day = Color(0xFF6A45F0), night = Color(0xFF7050F5))
    val onViolet: ColorProvider = ColorProvider(Color(0xFFFFFFFF))
    val violetText = DayNight(day = Color(0xFF5B3BE0), night = Color(0xFFA28BFF))
    val identityTile = DayNight(day = Color(0xFFEEE9FF), night = Color(0xFF221A40))
    val disabled = DayNight(day = Color(0xFFEFEFF3), night = Color(0xFF262933))
    val disabledContent = DayNight(day = Color(0xFFA1A1AA), night = Color(0xFF5A5D68))
    val lime: ColorProvider = ColorProvider(Color(0xFFC2F542))
    val limeText = DayNight(day = Color(0xFF4D7A00), night = Color(0xFFC2F542))

    /** Progress fill shifts green -> bright green -> lime with the level percent. */
    fun progress(pct: Int): ColorProvider =
        DayNight(day = Color(progressColor(pct, night = false)), night = Color(progressColor(pct, night = true)))
}

/**
 * Home-screen widget, top to bottom: identity card, progress bar, − / + buttons.
 * Content follows the repository's live HomeState, so in-app changes and widget taps both re-render it.
 */
class BecomingWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = context.appContainer.repository
        repository.refreshToday()
        val initial = repository.homeState.first()
        provideContent {
            val state by repository.homeState.collectAsState(initial = initial)
            WidgetRoot(state)
        }
    }
}

/** Rounded corners need API 31 in RemoteViews; older launchers get equivalent shape drawables. */
private val roundedCornersSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private fun GlanceModifier.rounded(color: ColorProvider, @DrawableRes fallback: Int, radius: Dp): GlanceModifier =
    if (roundedCornersSupported) background(color).cornerRadius(radius) else background(ImageProvider(fallback))

/** Outer frame: a 1dp neutral border normally, a 2dp lime border when a level is complete. */
private fun GlanceModifier.cardFrame(win: Boolean): GlanceModifier =
    if (roundedCornersSupported) {
        background(if (win) WidgetColors.lime else WidgetColors.border).cornerRadius(24.dp).padding(if (win) 2.dp else 1.dp)
    } else {
        background(ImageProvider(if (win) R.drawable.widget_background_win else R.drawable.widget_background))
    }

private fun GlanceModifier.cardSurface(): GlanceModifier =
    if (roundedCornersSupported) background(WidgetColors.surface).cornerRadius(23.dp) else this

@Composable
private fun WidgetRoot(state: HomeState?) {
    val compact = LocalSize.current.height < 160.dp
    val win = state?.pendingLevelComplete == true
    Box(GlanceModifier.fillMaxSize().cardFrame(win = win)) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cardSurface()
                .padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state == null) SetupPrompt() else ActiveContent(state, compact)
        }
    }
}

@Composable
private fun IdentityTile(size: Dp, win: Boolean) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .rounded(
                color = if (win) WidgetColors.lime else WidgetColors.identityTile,
                fallback = if (win) R.drawable.widget_tile_win else R.drawable.widget_tile,
                radius = 12.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(if (win) R.drawable.ic_identity_on_lime else R.drawable.ic_identity),
            contentDescription = null,
            modifier = GlanceModifier.size(size * 0.6f),
        )
    }
}

@Composable
private fun SetupPrompt() {
    Row(
        modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IdentityTile(size = 40.dp, win = false)
        Spacer(GlanceModifier.width(12.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                "Set up your identity",
                style = TextStyle(color = WidgetColors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                "Who are you becoming? Tap to begin.",
                style = TextStyle(color = WidgetColors.muted, fontSize = 12.sp),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ActiveContent(s: HomeState, compact: Boolean) {
    val openApp = actionStartActivity<MainActivity>()
    val win = s.pendingLevelComplete

    // 1. Identity card (tap opens the app): greeting + level-complete / nudge / welcome / daily quote.
    Row(
        modifier = GlanceModifier.fillMaxWidth().clickable(openApp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IdentityTile(size = if (compact) 30.dp else 36.dp, win = win)
        Spacer(GlanceModifier.width(10.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                s.greeting,
                style = TextStyle(color = WidgetColors.text, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            val subtitle = s.widgetSubtitle
            val style = when (subtitle.kind) {
                SubtitleKind.WIN -> TextStyle(color = WidgetColors.limeText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                SubtitleKind.NUDGE -> TextStyle(color = WidgetColors.violetText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                SubtitleKind.WELCOME -> TextStyle(color = WidgetColors.text, fontSize = 12.sp)
                SubtitleKind.QUOTE -> TextStyle(color = WidgetColors.muted, fontSize = 12.sp, fontStyle = FontStyle.Italic)
            }
            Text(
                subtitle.text,
                style = style,
                maxLines = if (subtitle.kind == SubtitleKind.QUOTE && !compact) 2 else 1,
            )
        }
    }

    // 2. Progress bar + "Level 2 · 20 votes cast ... 27%".
    Spacer(GlanceModifier.height(if (compact) 6.dp else 10.dp))
    LinearProgressIndicator(
        progress = s.progress,
        modifier = GlanceModifier.fillMaxWidth().height(6.dp),
        color = WidgetColors.progress(s.progressPercent),
        backgroundColor = WidgetColors.track,
    )
    Spacer(GlanceModifier.height(4.dp))
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            s.widgetProgressLabel,
            style = TextStyle(color = WidgetColors.muted, fontSize = 11.sp),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            "${s.progressPercent}%",
            style = TextStyle(color = WidgetColors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }

    // 3. − / + buttons.
    Spacer(GlanceModifier.height(if (compact) 6.dp else 10.dp))
    val buttonHeight = if (compact) 32.dp else 38.dp
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedWidgetButton(
            label = "−",
            enabled = s.canUndo,
            action = actionRunCallback<UndoAction>(),
            modifier = GlanceModifier.width(64.dp),
            height = buttonHeight,
        )
        Spacer(GlanceModifier.width(8.dp))
        when {
            s.pendingLevelComplete -> FilledWidgetButton(
                label = "Level up",
                enabled = true,
                action = openApp,
                modifier = GlanceModifier.defaultWeight(),
                height = buttonHeight,
            )
            s.votedToday -> FilledWidgetButton(
                label = "✓  Voted today",
                enabled = false,
                action = null,
                modifier = GlanceModifier.defaultWeight(),
                height = buttonHeight,
            )
            else -> FilledWidgetButton(
                label = "+  Vote",
                enabled = true,
                action = actionRunCallback<VoteAction>(),
                modifier = GlanceModifier.defaultWeight(),
                height = buttonHeight,
            )
        }
    }
}

/** Violet filled button (+ / Level up); disabled style when [enabled] is false. */
@Composable
private fun FilledWidgetButton(label: String, enabled: Boolean, action: Action?, modifier: GlanceModifier, height: Dp) {
    var m = modifier.height(height).rounded(
        color = if (enabled) WidgetColors.violet else WidgetColors.disabled,
        fallback = if (enabled) R.drawable.widget_btn_violet else R.drawable.widget_btn_disabled,
        radius = 12.dp,
    )
    if (enabled && action != null) m = m.clickable(action)
    Box(modifier = m, contentAlignment = Alignment.Center) {
        ButtonLabel(label, if (enabled) WidgetColors.onViolet else WidgetColors.disabledContent)
    }
}

/** Neutral outlined button (−). Glance has no border modifier, so the outline is a 1dp frame. */
@Composable
private fun OutlinedWidgetButton(label: String, enabled: Boolean, action: Action, modifier: GlanceModifier, height: Dp) {
    val content = if (enabled) WidgetColors.text else WidgetColors.disabledContent
    var outer = modifier.height(height)
    if (enabled) outer = outer.clickable(action)
    if (roundedCornersSupported) {
        Box(
            modifier = outer
                .background(if (enabled) WidgetColors.borderStrong else WidgetColors.border)
                .cornerRadius(12.dp)
                .padding(1.dp),
        ) {
            Box(
                modifier = GlanceModifier.fillMaxSize().background(WidgetColors.surface).cornerRadius(11.dp),
                contentAlignment = Alignment.Center,
            ) { ButtonLabel(label, content) }
        }
    } else {
        Box(
            modifier = outer.background(
                ImageProvider(if (enabled) R.drawable.widget_btn_outline else R.drawable.widget_btn_outline_disabled),
            ),
            contentAlignment = Alignment.Center,
        ) { ButtonLabel(label, content) }
    }
}

@Composable
private fun ButtonLabel(label: String, color: ColorProvider) {
    Text(
        label,
        style = TextStyle(color = color, fontSize = if (label.length == 1) 20.sp else 14.sp, fontWeight = FontWeight.Bold),
        maxLines = 1,
    )
}
