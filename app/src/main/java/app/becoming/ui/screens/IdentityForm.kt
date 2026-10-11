package app.becoming.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.becoming.R
import app.becoming.domain.FieldError
import app.becoming.domain.GreetBy
import app.becoming.domain.HabitRules
import app.becoming.domain.IdentityIcons
import app.becoming.domain.IdentityLimits
import app.becoming.domain.suggestIconKey
import app.becoming.domain.validateIdentity
import app.becoming.ui.IdentityAvatar
import app.becoming.ui.identityIconRes
import app.becoming.ui.screen
import app.becoming.ui.theme.BecomingTheme
import kotlinx.coroutines.launch

enum class IdentityFormMode { SETUP, EDIT }

/**
 * What the identity form edits. [iconKey] is a preset picked by hand (null = suggest automatically
 * from the skill and title); [iconPath] is a photo (a pending cache file until saved).
 */
data class IdentityDraft(
    val name: String = "",
    val skill: String = "",
    val identityTitle: String = "",
    val greetBy: GreetBy = GreetBy.IDENTITY,
    val iconKey: String? = null,
    val iconPath: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityForm(
    mode: IdentityFormMode,
    initial: IdentityDraft,
    onSubmit: (IdentityDraft) -> Unit,
    onCancel: () -> Unit,
    importPhoto: suspend (Uri) -> String?,
    discardPhoto: (String?) -> Unit,
    /** Setup only; Edit draws its own back arrow (which also discards an unsaved photo). */
    topBar: @Composable () -> Unit = {},
) {
    val c = BecomingTheme.colors
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var skill by rememberSaveable { mutableStateOf(initial.skill) }
    var title by rememberSaveable { mutableStateOf(initial.identityTitle) }
    var greetBy by rememberSaveable { mutableStateOf(initial.greetBy) }
    var iconKey by rememberSaveable { mutableStateOf(initial.iconKey) }
    var iconPath by rememberSaveable { mutableStateOf(initial.iconPath) }
    // Once the user picks an icon or photo by hand, typing no longer changes it.
    var iconManual by rememberSaveable { mutableStateOf(initial.iconKey != null || initial.iconPath != null) }
    var showEmptyErrors by rememberSaveable { mutableStateOf(false) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var photoFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (mode == IdentityFormMode.SETUP) {
        // The suggested name (from a previous journey) can arrive after the first frame.
        LaunchedEffect(initial.name) { if (name.isBlank()) name = initial.name }
    }

    val autoKey = suggestIconKey(skill, title)
    val shownKey = if (iconManual) iconKey ?: autoKey else autoKey
    val validation = validateIdentity(name, skill, title)

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            importing = true
            photoFailed = false
            scope.launch {
                val imported = importPhoto(uri)
                importing = false
                if (imported != null) {
                    discardPhoto(iconPath) // only deletes an earlier, unsaved pick
                    iconPath = imported
                    iconManual = true
                } else {
                    photoFailed = true
                }
            }
        }
    }

    fun cancel() {
        if (iconPath != initial.iconPath) discardPhoto(iconPath)
        onCancel()
    }

    fun submit() {
        val v = validateIdentity(name, skill, title)
        if (!v.isValid) {
            showEmptyErrors = true
            return
        }
        onSubmit(
            IdentityDraft(
                name = v.name,
                skill = v.skill,
                identityTitle = v.identityTitle,
                greetBy = greetBy,
                iconKey = if (iconManual) iconKey else null,
                iconPath = iconPath,
            ),
        )
    }

    fun clearPhoto() {
        discardPhoto(iconPath)
        iconPath = null
    }

    BackHandler(enabled = mode == IdentityFormMode.EDIT) { cancel() }

    Column(
        Modifier
            .screen(c.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        if (mode == IdentityFormMode.EDIT) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { cancel() }) {
                    Icon(painterResource(R.drawable.ic_back), contentDescription = "Back", tint = c.text)
                }
            }
        } else {
            topBar()
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (mode == IdentityFormMode.SETUP) "Who are you becoming?" else "Edit your identity",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = c.text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (mode == IdentityFormMode.SETUP) {
                "Every day you show up is a vote for that person."
            } else {
                "Your votes, levels and dates stay exactly as they are."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = c.muted,
        )
        Spacer(Modifier.height(24.dp))

        IdentityField(
            value = name,
            onValueChange = { name = it },
            label = "Your name",
            placeholder = null,
            error = errorText(validation.nameError, IdentityLimits.NAME_MAX, "Enter your name", showEmptyErrors),
        )
        IdentityField(
            value = skill,
            onValueChange = { skill = it },
            label = "What do you want to master?",
            placeholder = "e.g. Badminton",
            error = errorText(validation.skillError, IdentityLimits.SKILL_MAX, "Enter what you want to master", showEmptyErrors),
        )
        IdentityField(
            value = title,
            onValueChange = { title = it },
            label = "Call yourself",
            placeholder = "e.g. Badminton Player",
            error = errorText(validation.identityTitleError, IdentityLimits.TITLE_MAX, "Enter what to call yourself", showEmptyErrors),
            last = true,
        )

        // Icon row
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IdentityAvatar(iconKey = shownKey, photoPath = iconPath, size = 56.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Icon", style = MaterialTheme.typography.labelLarge, color = c.muted)
                Text(
                    when {
                        importing -> "Adding photo…"
                        iconPath != null -> "Your photo"
                        iconManual && iconKey != null -> IdentityIcons.byKey(iconKey)?.label ?: "Default"
                        else -> "Auto · " + (IdentityIcons.byKey(autoKey)?.label ?: "Default")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.text,
                )
                if (photoFailed) {
                    Text("Couldn't use that photo. Try another one.", style = MaterialTheme.typography.bodySmall, color = c.violetText)
                }
            }
            OutlinedButton(
                onClick = { pickerOpen = true },
                border = BorderStroke(1.dp, c.borderStrong),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = c.text),
            ) { Text("Change") }
        }

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
        if (mode == IdentityFormMode.SETUP) {
            Text(
                "75 votes builds the habit. One vote a day. Missed days never take votes away.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.muted,
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { submit() },
                enabled = !importing,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = violetButtonColors(),
            ) {
                Text("Cast my first vote", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { cancel() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    border = BorderStroke(1.dp, c.borderStrong),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = c.text),
                ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = { submit() },
                    enabled = !importing,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = violetButtonColors(),
                ) { Text("Save", fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (pickerOpen) {
        IconPickerDialog(
            selectedKey = if (iconManual && iconPath == null) iconKey else null,
            autoSelected = !iconManual && iconPath == null,
            hasPhoto = iconPath != null,
            onUpload = {
                pickerOpen = false
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemovePhoto = {
                pickerOpen = false
                clearPhoto()
                iconManual = true
            },
            onAuto = {
                pickerOpen = false
                clearPhoto()
                iconKey = null
                iconManual = false
            },
            onPick = { key ->
                pickerOpen = false
                clearPhoto()
                iconKey = key
                iconManual = true
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

@Composable
private fun violetButtonColors() = ButtonDefaults.buttonColors(
    containerColor = BecomingTheme.colors.violet,
    contentColor = BecomingTheme.colors.onViolet,
    disabledContainerColor = BecomingTheme.colors.disabled,
    disabledContentColor = BecomingTheme.colors.disabledContent,
)

private fun errorText(error: FieldError?, max: Int, emptyMessage: String, showEmpty: Boolean): String? = when (error) {
    FieldError.TOO_LONG -> "Use $max characters or fewer"
    FieldError.EMPTY -> if (showEmpty) emptyMessage else null
    null -> null
}

@Composable
private fun IdentityField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String?,
    error: String?,
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
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
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
            .padding(bottom = 8.dp),
    )
}

/** 4-column grid: Upload photo, Remove photo (when set), Auto, then the 22 presets. */
@Composable
private fun IconPickerDialog(
    selectedKey: String?,
    autoSelected: Boolean,
    hasPhoto: Boolean,
    onUpload: () -> Unit,
    onRemovePhoto: () -> Unit,
    onAuto: () -> Unit,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = BecomingTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = c.surface) {
            Column(Modifier.padding(20.dp)) {
                Text("Choose an icon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = c.text)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item { PickerTile("Upload photo", R.drawable.ic_add_photo, selected = false, onClick = onUpload) }
                    if (hasPhoto) {
                        item { PickerTile("Remove photo", R.drawable.ic_delete, selected = false, onClick = onRemovePhoto) }
                    }
                    item { PickerTile("Auto", R.drawable.ic_auto, selected = autoSelected, onClick = onAuto) }
                    items(IdentityIcons.ALL, key = { it.key }) { icon ->
                        PickerTile(
                            label = icon.label,
                            icon = identityIconRes(icon.key),
                            selected = icon.key == selectedKey,
                            onClick = { onPick(icon.key) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close", color = c.violetText) }
                }
            }
        }
    }
}

@Composable
private fun PickerTile(label: String, @DrawableRes icon: Int, selected: Boolean, onClick: () -> Unit) {
    val c = BecomingTheme.colors
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) c.violet else c.identityTile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (selected) c.onViolet else c.identityIcon,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = c.text,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
