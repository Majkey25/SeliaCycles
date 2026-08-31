package com.majkeylab.seliacycles

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            SeliaCyclesTheme(
                state.backup.settings.theme,
                state.backup.settings.palette,
                state.backup.settings.customPalette,
            ) {
                SeliaCyclesApp(state, viewModel)
            }
        }
    }
}
