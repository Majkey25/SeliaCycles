package com.majkeylab.seliacycles

import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val viewModel by lazy { ViewModelProvider(this)[MainViewModel::class.java] }
    private val dateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { viewModel.refreshForToday() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) routeProfile(intent)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            SeliaCyclesTheme(state.backup.settings) {
                val lightBars = MaterialTheme.colorScheme.background.contrastColor() == Color.Black
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = lightBars
                        isAppearanceLightNavigationBars = lightBars
                    }
                }
                SeliaCyclesApp(state, viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshForToday()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(this, dateReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        unregisterReceiver(dateReceiver)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeProfile(intent)
    }

    private fun routeProfile(intent: Intent) {
        val id = intent.getStringExtra(ReminderWorker.PROFILE_ID_EXTRA) ?: return
        val day = intent.getStringExtra(CalendarWidget.DAY_EXTRA)?.let {
            runCatching { java.time.LocalDate.parse(it) }.getOrNull()
        }
        if (day != null) viewModel.openCalendar(id, day) else viewModel.selectProfile(id)
    }
}
