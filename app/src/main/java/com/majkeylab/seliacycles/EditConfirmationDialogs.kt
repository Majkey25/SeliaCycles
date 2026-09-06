package com.majkeylab.seliacycles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberEditorSheetState(hasChanges: () -> Boolean, onDiscardRequest: () -> Unit): SheetState {
    val currentHasChanges by rememberUpdatedState(hasChanges)
    val currentRequest by rememberUpdatedState(onDiscardRequest)
    val confirmChange = remember {
        { value: SheetValue ->
            if (value == SheetValue.Hidden && currentHasChanges()) {
                currentRequest()
                false
            } else true
        }
    }
    // Veto hiding before the sheet disappears, so Keep editing can return to it.
    return rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = confirmChange)
}

@Composable
internal fun DiscardChangesDialog(onKeepEditing: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
        title = { Text(stringResource(R.string.unsaved_changes_title)) },
        text = { Text(stringResource(R.string.unsaved_changes_body)) },
        confirmButton = {
            TextButton(onClick = onDiscard) { Text(stringResource(R.string.discard_changes), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) { Text(stringResource(R.string.keep_editing)) }
        },
    )
}

@Composable
internal fun DeleteInformationDialog(dateLabel: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.delete_information_title)) },
        text = { Text(stringResource(R.string.delete_information_body, dateLabel)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.confirm_delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}
