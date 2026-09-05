package com.majkeylab.seliacycles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
fun ProfileSwitcher(state: AppState, onSelect: (String) -> Unit, onManage: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val enabled = !state.busy && !state.loading && !state.loadFailed
    Box {
        TextButton(onClick = { expanded = true }, enabled = enabled) {
            Icon(Icons.Outlined.PersonOutline, contentDescription = stringResource(R.string.profiles_title))
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
                    modifier = Modifier.semantics { selected = profile.id == state.activeProfile.id },
                    onClick = {
                        expanded = false
                        onSelect(profile.id)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_manage)) },
                onClick = { expanded = false; onManage() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesSheet(
    state: AppState,
    onDismiss: () -> Unit,
    onCreate: (String, UiMode) -> Unit,
    onUpdate: (String, UiMode) -> Unit,
    onDelete: () -> Unit,
) {
    val profile = state.activeProfile
    var creating by rememberSaveable(profile.id) { mutableStateOf(false) }
    var name by rememberSaveable(profile, creating) { mutableStateOf(if (creating) "" else profile.name) }
    var mode by rememberSaveable(profile, creating) { mutableStateOf(if (creating) UiMode.STANDARD else profile.mode) }
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
                onClick = { if (creating) onCreate(trimmedName, mode) else onUpdate(trimmedName, mode) },
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
