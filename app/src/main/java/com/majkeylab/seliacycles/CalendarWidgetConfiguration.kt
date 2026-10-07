package com.majkeylab.seliacycles

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarWidgetConfiguration : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (!CalendarWidget.ownsWidget(this, id)) { finish(); return }
        lifecycleScope.launch {
            val profiles = withContext(Dispatchers.IO) { runCatching { LocalProfiles(this@CalendarWidgetConfiguration).profiles() } }.getOrNull()
            if (profiles == null) { finish(); return@launch }
            val prefs = CalendarWidget.preferences(this@CalendarWidgetConfiguration)
            val initial = profiles.firstOrNull { it.id == prefs.getString("profile_$id", null) } ?: profiles.first()
            setContent {
                var selected by rememberSaveable { mutableStateOf(initial.id) }
                var details by rememberSaveable { mutableStateOf(prefs.getBoolean("details_$id", false)) }
                var widgetAppearance by rememberSaveable(stateSaver = widgetAppearanceSaver) {
                    mutableStateOf(CalendarWidget.appearance(this@CalendarWidgetConfiguration, id))
                }
                var settings by remember { mutableStateOf<AppSettings?>(null) }
                var busy by remember { mutableStateOf(false) }
                var failed by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(selected) {
                    settings = null
                    failed = false
                    try {
                        settings = withContext(Dispatchers.IO) {
                            CycleStore(this@CalendarWidgetConfiguration, selected).use { it.load().settings }
                        }
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        failed = true
                    }
                }
                val appearance = settings ?: AppSettings()
                SeliaCyclesTheme(appearance.theme, appearance.palette, appearance.customPalette) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(stringResource(R.string.widget_name), style = MaterialTheme.typography.headlineMedium)
                            Text(stringResource(R.string.widget_setup_body))
                            Column(Modifier.selectableGroup()) {
                                profiles.forEach { profile ->
                                    Row(Modifier.fillMaxWidth().selectable(selected == profile.id, enabled = !busy,
                                        role = Role.RadioButton, onClick = { selected = profile.id }).padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected == profile.id, onClick = null)
                                        Spacer(Modifier.width(12.dp))
                                        Icon(profileIconVector(profile.icon), contentDescription = null)
                                        Spacer(Modifier.width(12.dp))
                                        Text(profile.name.ifBlank { stringResource(R.string.profile_default_name) })
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                val label = stringResource(R.string.widget_show_details)
                                Text(label, Modifier.weight(1f))
                                Switch(details, onCheckedChange = { details = it }, enabled = !busy,
                                    modifier = Modifier.semantics { contentDescription = label })
                            }
                            Text(stringResource(R.string.widget_privacy), style = MaterialTheme.typography.bodySmall)
                            WidgetAppearanceSettings(widgetAppearance, busy, onChange = { widgetAppearance = it })
                            if (failed) Text(stringResource(R.string.operation_failed), color = MaterialTheme.colorScheme.error)
                            Button(modifier = Modifier.fillMaxWidth(), enabled = !busy && settings != null, onClick = {
                                busy = true
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { CalendarWidget.configure(this@CalendarWidgetConfiguration, id, selected, details, widgetAppearance) }
                                        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                                        finish()
                                    } catch (error: Exception) {
                                        if (error is CancellationException) throw error
                                        failed = true
                                        busy = false
                                    }
                                }
                            }) { Text(stringResource(R.string.save)) }
                            TextButton(onClick = { finish() }, enabled = !busy) { Text(stringResource(R.string.cancel)) }
                        }
                    }
                }
            }
        }
    }
}
