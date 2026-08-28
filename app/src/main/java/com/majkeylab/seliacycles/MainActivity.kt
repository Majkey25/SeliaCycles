package com.majkeylab.seliacycles

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        val accountManager = GoogleAccountManager(this)
        viewModel.initializeCloud(accountManager.isConfigured, accountManager.currentAccount())
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            SeliaCyclesTheme(state.backup.settings.theme) {
                SeliaCyclesApp(
                    state = state,
                    viewModel = viewModel,
                    onGoogleSignIn = { viewModel.signIn(accountManager::signIn) },
                    onGoogleSignOut = { viewModel.signOut(accountManager::signOut) },
                )
            }
        }
    }
}

class PermissionsRationaleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeliaCyclesTheme(AppTheme.SYSTEM) {
                Scaffold { padding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text(stringResource(R.string.privacy), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = ::finish) { Text(stringResource(R.string.close)) }
                    }
                }
            }
        }
    }
}
