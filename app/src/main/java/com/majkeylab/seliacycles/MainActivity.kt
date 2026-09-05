package com.majkeylab.seliacycles

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {
    private val viewModel by lazy { ViewModelProvider(this)[MainViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) routeProfile(intent)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val dark = state.backup.settings.theme == AppTheme.DARK ||
                state.backup.settings.theme == AppTheme.SYSTEM && isSystemInDarkTheme()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            SeliaCyclesTheme(
                state.backup.settings.theme,
                state.backup.settings.palette,
                state.backup.settings.customPalette,
            ) {
                SeliaCyclesApp(state, viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshForToday()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeProfile(intent)
    }

    private fun routeProfile(intent: Intent) {
        val id = intent.getStringExtra(ReminderWorker.PROFILE_ID_EXTRA) ?: return
        viewModel.selectProfile(id)
    }
}
