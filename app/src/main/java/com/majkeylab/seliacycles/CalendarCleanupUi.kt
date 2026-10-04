package com.majkeylab.seliacycles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

@Composable
internal fun CalendarCleanupCalendarDialog(calendars: List<DeviceCalendar>, onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calendar_cleanup_choose)) },
        text = { LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.calendar_cleanup_scope)) }
            items(calendars, key = DeviceCalendar::id) { calendar ->
                TextButton(onClick = { onSelect(calendar.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(calendar.displayName)
                        Text(calendar.accountName, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
internal fun CalendarCleanupDialog(preview: CalendarCleanupPreview, busy: Boolean, onDismiss: () -> Unit, onDelete: (Set<Long>) -> Unit) {
    // Start unselected: title-only copies cannot be reliably assigned to a profile.
    var selected by remember(preview) { mutableStateOf(emptySet<Long>()) }
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateFormat.getDateInstance(DateFormat.MEDIUM, locale).apply { timeZone = TimeZone.getTimeZone("UTC") } }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.calendar_cleanup_review)) },
        text = { LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(preview.calendar.displayName, style = MaterialTheme.typography.titleMedium)
                Text(preview.calendar.accountName, style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.calendar_cleanup_scope))
                if (preview.events.isEmpty()) Text(stringResource(R.string.calendar_cleanup_empty)) else {
                    if (preview.events.any(CalendarCleanupEvent::legacy)) Text(stringResource(R.string.calendar_cleanup_legacy_warning))
                    Text(stringResource(R.string.calendar_cleanup_count, selected.size, preview.events.size))
                    TextButton(enabled = !busy, onClick = {
                        selected = if (selected.size == preview.events.size) emptySet() else preview.events.mapTo(mutableSetOf(), CalendarCleanupEvent::id)
                    }) { Text(stringResource(if (selected.size == preview.events.size) R.string.calendar_cleanup_select_none else R.string.calendar_cleanup_select_all)) }
                }
            } }
            items(preview.events, key = CalendarCleanupEvent::id) { event ->
                Row(Modifier.fillMaxWidth().toggleable(value = event.id in selected, enabled = !busy, role = Role.Checkbox) {
                    selected = if (it) selected + event.id else selected - event.id
                }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = event.id in selected, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(event.title.orEmpty())
                        val start = format.format(Date(event.startMillis))
                        val end = format.format(Date(event.endMillis - 1))
                        Text(if (start == end) start else "$start – $end", style = MaterialTheme.typography.bodySmall)
                        if (event.legacy) Text(stringResource(R.string.calendar_cleanup_legacy), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } },
        confirmButton = { TextButton(onClick = { onDelete(selected) }, enabled = !busy && selected.isNotEmpty()) {
            Text(stringResource(R.string.calendar_cleanup_delete_selected, selected.size))
        } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
}
