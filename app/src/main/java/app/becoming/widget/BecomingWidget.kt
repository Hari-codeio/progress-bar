package app.becoming.widget

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
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
import androidx.glance.layout.ContentScale
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
import app.becoming.ui.identityIconRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import androidx.glance.color.ColorProvider as DayNight

/** Everything one widget frame needs: the journey state, the theme setting and the decoded photo. */
private data class WidgetModel(val state: HomeState?, val dark: Boolean?, val photo: Bitmap?)

/**
 * Widget colours, the same hex values as the in-app theme. [mode] is the app's theme setting:
 * null follows the launcher's day/night; true/false force the night/day palette.
 */
private class WidgetPalette(private val mode: Boolean?) {
    private fun pick(day: Long, night: Long): ColorProvider = when (mode) {
        null -> DayNight(day = Color(day), night = Color(night))
        true -> ColorProvider(Color(night))
        false -> ColorProvider(Color(day))
    }

    val surface = pick(0xFFFFFFFF, 0xFF16181F)
    val border = pick(0xFFE4E4E9, 0xFF262933)
    val borderStrong = pick(0xFFD0D0D8, 0xFF383C48)
    val track = pick(0xFFE9E9EE, 0xFF23262F)
    val text = pick(0xFF0D0E12, 0xFFF7F7F8)
    val muted = pick(0xFF5B5B66, 0xFFA1A1AA)
    val violet = pick(0xFF6A45F0, 0xFF7050F5)
    val onViolet = pick(0xFFFFFFFF, 0xFFFFFFFF)
    val violetText = pick(0xFF5B3BE0, 0xFFA28BFF)
    val identityTile = pick(0xFFEEE9FF, 0xFF221A40)
    val identityIcon = pick(0xFF6A45F0, 0xFFA28BFF)
    val disabled = pick(0xFFEFEFF3, 0xFF262933)
    val disabledContent = pick(0xFFA1A1AA, 0xFF5A5D68)
    val lime = pick(0xFFC2F542, 0xFFC2F542)
    val onLime = pick(0xFF0D0E12, 0xFF0D0E12)
    val limeText = pick(0xFF4D7A00, 0xFFC2F542)

    /** Progress fill shifts green -> bright green -> lime with the level percent. */
    fun progress(pct: Int): ColorProvider = when (mode) {
        null -> DayNight(day = Color(progressColor(pct, night = false)), night = Color(progressColor(pct, night = true)))
        true -> ColorProvider(Color(progressColor(pct, night = true)))
        false -> ColorProvider(Color(progressColor(pct, night = false)))
    }

    /** Pre-API-31 shape drawable. Resource night qualifiers follow the system, so forced modes use explicit variants. */
    @DrawableRes
    fun shape(shape: WidgetShape): Int = when (mode) {
        null -> shape.auto
        true -> shape.dark
        false -> shape.light
    }
}

private enum class WidgetShape(@DrawableRes val auto: Int, @DrawableRes val light: Int, @DrawableRes val dark: Int) {
    BACKGROUND(R.drawable.widget_background, R.drawable.widget_background_light, R.drawable.widget_background_dark),
    BACKGROUND_WIN(R.drawable.widget_background_win, R.drawable.widget_background_win_light, R.drawable.widget_background_win_dark),
    TILE(R.drawable.widget_tile, R.drawable.widget_tile_light, R.drawable.widget_tile_dark),
    TILE_WIN(R.drawable.widget_tile_win, R.drawable.widget_tile_win, R.drawable.widget_tile_win),
    BUTTON_VIOLET(R.drawable.widget_btn_violet, R.drawable.widget_btn_violet_light, R.drawable.widget_btn_violet_dark),
    BUTTON_DISABLED(R.drawable.widget_btn_disabled, R.drawable.widget_btn_disabled_light, R.drawable.widget_btn_disabled_dark),
    BUTTON_OUTLINE(R.drawable.widget_btn_outline, R.drawable.widget_btn_outline_light, R.drawable.widget_btn_outline_dark),
    BUTTON_OUTLINE_DISABLED(
        R.drawable.widget_btn_outline_disabled,
        R.drawable.widget_btn_outline_disabled_light,
        R.drawable.widget_btn_outline_disabled_dark,
    ),
}

/**
 * Home-screen widget, top to bottom: identity card, progress bar, − / + buttons.
 * Content follows the repository's live HomeState and the theme setting, so in-app changes and
 * widget taps both re-render it.
 */
class BecomingWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = context.appContainer
        val repository = container.repository
        repository.refreshToday()
        val models = combine(repository.homeState, repository.darkMode) { state, dark ->
            WidgetModel(state, dark, container.photos.loadBitmap(state?.iconPath))
        }.flowOn(Dispatchers.IO)
        val initial = models.first()
        provideContent {
            val model by models.collectAsState(initial = initial)
            WidgetRoot(model)
        }
    }
}

/** Rounded corners need API 31 in RemoteViews; older launchers get equivalent shape drawables. */
private val roundedCornersSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private fun GlanceModifier.rounded(color: ColorProvider, @DrawableRes fallback: Int, radius: Dp): GlanceModifier =
    if (roundedCornersSupported) background(color).cornerRadius(radius) else background(ImageProvider(fallback))

@Composable
private fun WidgetRoot(model: WidgetModel) {
    val p = WidgetPalette(model.dark)
    val state = model.state
    val compact = LocalSize.current.height < 160.dp
    val win = state?.pendingLevelComplete == true
    // Outer frame: a 1dp neutral border normally, a 2dp lime border when a level is complete.
    val frame = if (roundedCornersSupported) {
        GlanceModifier.fillMaxSize()
            .background(if (win) p.lime else p.border)
            .cornerRadius(24.dp)
            .padding(if (win) 2.dp else 1.dp)
    } else {
        GlanceModifier.fillMaxSize()
            .background(ImageProvider(p.shape(if (win) WidgetShape.BACKGROUND_WIN else WidgetShape.BACKGROUND)))
    }
    val surface = if (roundedCornersSupported) {
        GlanceModifier.fillMaxSize().background(p.surface).cornerRadius(23.dp)
    } else {
        GlanceModifier.fillMaxSize()
    }
    Box(frame) {
        Column(
            modifier = surface.padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state == null) SetupPrompt(p) else ActiveContent(p, state, model.photo, compact)
        }
    }
}

/** Photo (rounded on API 31+) or the preset icon tinted on its tile; lime tile when a level is complete. */
@Composable
private fun IdentityTile(p: WidgetPalette, iconKey: String?, photo: Bitmap?, size: Dp, win: Boolean) {
    if (photo != null) {
        val shaped = if (roundedCornersSupported) GlanceModifier.size(size).cornerRadius(size / 2) else GlanceModifier.size(size)
        Image(
            provider = ImageProvider(photo),
            contentDescription = "Identity photo",
            contentScale = ContentScale.Crop,
            modifier = shaped,
        )
    } else {
        Box(
            modifier = GlanceModifier
                .size(size)
                .rounded(
                    color = if (win) p.lime else p.identityTile,
                    fallback = p.shape(if (win) WidgetShape.TILE_WIN else WidgetShape.TILE),
                    radius = 12.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(identityIconRes(iconKey)),
                contentDescription = null,
                modifier = GlanceModifier.size(size * 0.6f),
                colorFilter = ColorFilter.tint(if (win) p.onLime else p.identityIcon),
            )
        }
    }
}

@Composable
private fun SetupPrompt(p: WidgetPalette) {
    Row(
        modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IdentityTile(p, iconKey = null, photo = null, size = 40.dp, win = false)
        Spacer(GlanceModifier.width(12.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                "Set up your identity",
                style = TextStyle(color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                "Who are you becoming? Tap to begin.",
                style = TextStyle(color = p.muted, fontSize = 12.sp),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ActiveContent(p: WidgetPalette, s: HomeState, photo: Bitmap?, compact: Boolean) {
    val openApp = actionStartActivity<MainActivity>()
    val win = s.pendingLevelComplete

    // 1. Identity card (tap opens the app): greeting + level-complete / nudge / welcome / daily quote.
    Row(
        modifier = GlanceModifier.fillMaxWidth().clickable(openApp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IdentityTile(p, iconKey = s.displayIconKey, photo = photo, size = if (compact) 30.dp else 36.dp, win = win)
        Spacer(GlanceModifier.width(10.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                s.greeting,
                style = TextStyle(color = p.text, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            val subtitle = s.widgetSubtitle
            val style = when (subtitle.kind) {
                SubtitleKind.WIN -> TextStyle(color = p.limeText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                SubtitleKind.NUDGE -> TextStyle(color = p.violetText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                SubtitleKind.WELCOME -> TextStyle(color = p.text, fontSize = 12.sp)
                SubtitleKind.QUOTE -> TextStyle(color = p.muted, fontSize = 12.sp, fontStyle = FontStyle.Italic)
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
        color = p.progress(s.progressPercent),
        backgroundColor = p.track,
    )
    Spacer(GlanceModifier.height(4.dp))
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            s.widgetProgressLabel,
            style = TextStyle(color = p.muted, fontSize = 11.sp),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            "${s.progressPercent}%",
            style = TextStyle(color = p.text, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }

    // 3. − / + buttons.
    Spacer(GlanceModifier.height(if (compact) 6.dp else 10.dp))
    val buttonHeight = if (compact) 32.dp else 38.dp
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedWidgetButton(
            p = p,
            label = "−",
            enabled = s.canUndo,
            action = actionRunCallback<UndoAction>(),
            modifier = GlanceModifier.width(64.dp),
            height = buttonHeight,
        )
        Spacer(GlanceModifier.width(8.dp))
        when {
            s.pendingLevelComplete -> FilledWidgetButton(
                p = p,
                label = "Level up",
                enabled = true,
                action = openApp,
                modifier = GlanceModifier.defaultWeight(),
                height = buttonHeight,
            )
            s.votedToday -> FilledWidgetButton(
                p = p,
                label = "✓  Voted today",
                enabled = false,
                action = null,
                modifier = GlanceModifier.defaultWeight(),
                height = buttonHeight,
            )
            else -> FilledWidgetButton(
                p = p,
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
private fun FilledWidgetButton(
    p: WidgetPalette,
    label: String,
    enabled: Boolean,
    action: Action?,
    modifier: GlanceModifier,
    height: Dp,
) {
    var m = modifier.height(height).rounded(
        color = if (enabled) p.violet else p.disabled,
        fallback = p.shape(if (enabled) WidgetShape.BUTTON_VIOLET else WidgetShape.BUTTON_DISABLED),
        radius = 12.dp,
    )
    if (enabled && action != null) m = m.clickable(action)
    Box(modifier = m, contentAlignment = Alignment.Center) {
        ButtonLabel(label, if (enabled) p.onViolet else p.disabledContent)
    }
}

/** Neutral outlined button (−). Glance has no border modifier, so the outline is a 1dp frame. */
@Composable
private fun OutlinedWidgetButton(
    p: WidgetPalette,
    label: String,
    enabled: Boolean,
    action: Action,
    modifier: GlanceModifier,
    height: Dp,
) {
    val content = if (enabled) p.text else p.disabledContent
    var outer = modifier.height(height)
    if (enabled) outer = outer.clickable(action)
    if (roundedCornersSupported) {
        Box(
            modifier = outer
                .background(if (enabled) p.borderStrong else p.border)
                .cornerRadius(12.dp)
                .padding(1.dp),
        ) {
            Box(
                modifier = GlanceModifier.fillMaxSize().background(p.surface).cornerRadius(11.dp),
                contentAlignment = Alignment.Center,
            ) { ButtonLabel(label, content) }
        }
    } else {
        Box(
            modifier = outer.background(
                ImageProvider(p.shape(if (enabled) WidgetShape.BUTTON_OUTLINE else WidgetShape.BUTTON_OUTLINE_DISABLED)),
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
