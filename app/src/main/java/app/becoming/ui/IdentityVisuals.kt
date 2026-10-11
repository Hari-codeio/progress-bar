package app.becoming.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import app.becoming.R
import app.becoming.appContainer
import app.becoming.ui.theme.BecomingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Drawable for a preset key from domain.IdentityIcons; null or unknown keys get the default mark. */
@DrawableRes
fun identityIconRes(key: String?): Int = when (key) {
    "gym" -> R.drawable.ic_id_gym
    "athlete" -> R.drawable.ic_id_athlete
    "badminton" -> R.drawable.ic_id_badminton
    "cricket" -> R.drawable.ic_id_cricket
    "football" -> R.drawable.ic_id_football
    "tennis" -> R.drawable.ic_id_tennis
    "swimming" -> R.drawable.ic_id_swimming
    "cycling" -> R.drawable.ic_id_cycling
    "yoga" -> R.drawable.ic_id_yoga
    "meditation" -> R.drawable.ic_id_meditation
    "developer" -> R.drawable.ic_id_developer
    "reader" -> R.drawable.ic_id_reader
    "writer" -> R.drawable.ic_id_writer
    "musician" -> R.drawable.ic_id_musician
    "singer" -> R.drawable.ic_id_singer
    "artist" -> R.drawable.ic_id_artist
    "photographer" -> R.drawable.ic_id_photographer
    "chef" -> R.drawable.ic_id_chef
    "student" -> R.drawable.ic_id_student
    "language" -> R.drawable.ic_id_language
    "entrepreneur" -> R.drawable.ic_id_entrepreneur
    "speaker" -> R.drawable.ic_id_speaker
    else -> R.drawable.ic_identity
}

/** Loads an identity photo off the main thread; null while loading, when unset, or when the file is gone. */
@Composable
fun rememberIdentityPhoto(path: String?): ImageBitmap? {
    val context = LocalContext.current
    val photo by produceState<ImageBitmap?>(initialValue = null, path) {
        value = if (path == null) {
            null
        } else {
            withContext(Dispatchers.IO) { context.appContainer.photos.loadBitmap(path)?.asImageBitmap() }
        }
    }
    return photo
}

/** The identity mark: the photo as a circle, otherwise the preset icon on a rounded tile. */
@Composable
fun IdentityAvatar(
    iconKey: String?,
    photoPath: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    tile: Color = BecomingTheme.colors.identityTile,
    tint: Color = BecomingTheme.colors.identityIcon,
) {
    val photo = rememberIdentityPhoto(photoPath)
    if (photo != null) {
        Image(
            bitmap = photo,
            contentDescription = "Identity photo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.3f))
                .background(tile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(identityIconRes(iconKey)),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size * 0.56f),
            )
        }
    }
}
