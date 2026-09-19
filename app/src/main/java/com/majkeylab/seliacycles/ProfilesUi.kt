package com.majkeylab.seliacycles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ProfileSwitcher(state: AppState, onSelect: (String) -> Unit, onManage: () -> Unit, onCreate: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val enabled = !state.busy && !state.loading && !state.loadFailed
    Box {
        TextButton(onClick = { expanded = true }, enabled = enabled) {
            Icon(profileIconVector(state.activeProfile.icon), contentDescription = stringResource(R.string.profiles_title))
            Text(
                state.activeProfile.name.ifBlank { stringResource(R.string.profile_default_name) },
                modifier = Modifier.widthIn(max = 180.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            state.profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(profile.name.ifBlank { stringResource(R.string.profile_default_name) }) },
                    leadingIcon = { Icon(profileIconVector(profile.icon), contentDescription = null) },
                    trailingIcon = if (profile.id == state.activeProfile.id) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else null,
                    modifier = Modifier.semantics { selected = profile.id == state.activeProfile.id },
                    onClick = {
                        expanded = false
                        onSelect(profile.id)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_create)) },
                leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                enabled = state.profiles.size < LocalProfiles.MAX_PROFILES,
                onClick = { expanded = false; onCreate() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_manage)) },
                onClick = { expanded = false; onManage() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfilesSheet(
    state: AppState,
    onDismiss: () -> Unit,
    onCreate: (String, UiMode, ProfileIcon) -> Unit,
    onUpdate: (String, UiMode, ProfileIcon) -> Unit,
    onDelete: () -> Unit,
    startCreating: Boolean = false,
) {
    val profile = state.activeProfile
    var creating by rememberSaveable(profile.id, startCreating) { mutableStateOf(startCreating) }
    var name by rememberSaveable(profile, creating) { mutableStateOf(if (creating) "" else profile.name) }
    var mode by rememberSaveable(profile, creating) { mutableStateOf(if (creating) UiMode.STANDARD else profile.mode) }
    var icon by rememberSaveable(profile, creating) { mutableStateOf(if (creating) ProfileIcon.PERSON else profile.icon) }
    var confirmDelete by rememberSaveable(profile.id) { mutableStateOf(false) }
    val enabled = !state.busy && !state.loading
    val trimmedName = name.trim()
    val validName = trimmedName.length <= LocalProfiles.MAX_NAME_LENGTH &&
        trimmedName.none(Char::isISOControl) &&
        (trimmedName.isNotBlank() || (!creating && profile.id == LocalProfiles.DEFAULT_ID))

    ModalBottomSheet(
        onDismissRequest = { if (enabled) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        sheetGesturesEnabled = false,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (creating) R.string.profile_create else R.string.profiles_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(onClick = onDismiss, enabled = enabled) { Text(stringResource(R.string.close)) }
            }
            Text(
                stringResource(R.string.profiles_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= LocalProfiles.MAX_NAME_LENGTH) name = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                label = { Text(stringResource(R.string.profile_name)) },
                placeholder = { Text(stringResource(R.string.profile_default_name)) },
                isError = name.isNotEmpty() && !validName,
                supportingText = {
                    Text(stringResource(
                        if (name.isNotEmpty() && !validName) R.string.profile_name_invalid else R.string.profile_name_hint,
                        LocalProfiles.MAX_NAME_LENGTH,
                    ))
                },
            )
            Text(stringResource(R.string.profile_icon), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileIcon.entries.forEach { option ->
                    FilterChip(
                        selected = icon == option,
                        enabled = enabled,
                        onClick = { icon = option },
                        label = { Text(stringResource(profileIconLabel(option))) },
                        leadingIcon = { Icon(profileIconVector(option), contentDescription = null) },
                    )
                }
            }
            Text(stringResource(R.string.ui_mode_label), style = MaterialTheme.typography.titleMedium)
            Column(Modifier.selectableGroup()) {
                UiMode.entries.forEach { option ->
                    val title = when (option) {
                        UiMode.SIMPLE -> R.string.ui_mode_simple
                        UiMode.STANDARD -> R.string.ui_mode_standard
                        UiMode.DETAILED -> R.string.ui_mode_detailed
                    }
                    val description = when (option) {
                        UiMode.SIMPLE -> R.string.ui_mode_simple_description
                        UiMode.STANDARD -> R.string.ui_mode_standard_description
                        UiMode.DETAILED -> R.string.ui_mode_detailed_description
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().selectable(
                            selected = mode == option,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { mode = option },
                        ).padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = mode == option, onClick = null, enabled = enabled)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Button(
                onClick = { if (creating) onCreate(trimmedName, mode, icon) else onUpdate(trimmedName, mode, icon) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && validName && (!creating || state.profiles.size < LocalProfiles.MAX_PROFILES),
            ) {
                Text(stringResource(if (creating) R.string.profile_create else R.string.save))
            }
            if (creating) {
                TextButton(onClick = { creating = false }, enabled = enabled) { Text(stringResource(R.string.cancel)) }
            } else {
                TextButton(
                    onClick = { creating = true },
                    enabled = enabled && state.profiles.size < LocalProfiles.MAX_PROFILES,
                ) { Text(stringResource(R.string.profile_create)) }
                if (state.profiles.size >= LocalProfiles.MAX_PROFILES) {
                    Text(stringResource(R.string.profile_limit, LocalProfiles.MAX_PROFILES), style = MaterialTheme.typography.bodySmall)
                }
                if (profile.id != LocalProfiles.DEFAULT_ID) {
                    TextButton(onClick = { confirmDelete = true }, enabled = enabled) {
                        Text(stringResource(R.string.profile_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (enabled) confirmDelete = false },
            title = { Text(stringResource(R.string.profile_delete_title)) },
            text = { Text(stringResource(R.string.profile_delete_body, profile.name)) },
            confirmButton = {
                TextButton(
                    onClick = { confirmDelete = false; onDelete() },
                    enabled = enabled,
                ) { Text(stringResource(R.string.profile_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }, enabled = enabled) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private fun profileIconVector(icon: ProfileIcon) = when (icon) {
    ProfileIcon.PERSON -> Icons.Outlined.PersonOutline
    ProfileIcon.FLOWER -> Icons.Outlined.LocalFlorist
    ProfileIcon.STAR -> Icons.Outlined.StarBorder
    ProfileIcon.HEART -> Icons.Outlined.FavoriteBorder
    ProfileIcon.SUN -> Icons.Outlined.WbSunny
}

private fun profileIconLabel(icon: ProfileIcon) = when (icon) {
    ProfileIcon.PERSON -> R.string.profile_icon_person
    ProfileIcon.FLOWER -> R.string.profile_icon_flower
    ProfileIcon.STAR -> R.string.profile_icon_star
    ProfileIcon.HEART -> R.string.profile_icon_heart
    ProfileIcon.SUN -> R.string.profile_icon_sun
}
