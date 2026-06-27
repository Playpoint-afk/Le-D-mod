package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.engine.LudoViewModel
import com.example.model.GamePhase
import com.example.ui.LudoHomeScreen
import com.example.ui.LudoGameplayScreen
import com.example.ui.LudoSetupScreen
import com.example.ui.theme.MyApplicationTheme
import android.os.Build
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private var isFirstLaunchTriggered = false

    override fun onStart() {
        super.onStart()
        if (!isFirstLaunchTriggered) {
            isFirstLaunchTriggered = true
            val sharedPrefs = getSharedPreferences("ludo_game_prefs", android.content.Context.MODE_PRIVATE)
            val isFirstRun = sharedPrefs.getBoolean("is_first_run_notification", true)
            if (isFirstRun) {
                com.example.notification.LudoNotifications.showNotification(
                    this,
                    "Le Dé Moderne",
                    "مرحبا بك في لعبة Le dé Moderne"
                )
                sharedPrefs.edit().putBoolean("is_first_run_notification", false).apply()
            } else {
                com.example.notification.LudoNotifications.showNotification(
                    this,
                    "Le Dé Moderne",
                    "نتمنا لك وقتا ممتعا"
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        com.example.notification.LudoNotifications.showNotification(
            this,
            "Le Dé Moderne",
            "نتمنا ان تعود في اقرب وقت!"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ -> }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Constructor injection of Application Context
                val viewModel = remember { LudoViewModel(applicationContext) }
                val state = viewModel.state.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val screenModifier = Modifier.padding(innerPadding)
                    
                    when (state.value.gamePhase) {
                        GamePhase.HOME -> {
                            LudoHomeScreen(
                                viewModel = viewModel,
                                state = state.value,
                                modifier = screenModifier
                            )
                        }
                        GamePhase.SETUP -> {
                            LudoSetupScreen(
                                currentSettings = state.value.settings,
                                onStartGame = { mode, players ->
                                    viewModel.initGame(mode, players)
                                },
                                onBackToHome = {
                                    viewModel.setGamePhase(GamePhase.HOME)
                                },
                                modifier = screenModifier
                            )
                        }
                        else -> {
                            LudoGameplayScreen(
                                viewModel = viewModel,
                                state = state.value,
                                modifier = screenModifier
                            )
                        }
                    }
                }
            }
        }
    }
}
