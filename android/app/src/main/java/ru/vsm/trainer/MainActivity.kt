package ru.vsm.trainer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ru.vsm.trainer.presentation.login.LoginScreen
import ru.vsm.trainer.presentation.login.SessionViewModel
import ru.vsm.trainer.presentation.navigation.AppNavigation
import ru.vsm.trainer.presentation.theme.VsmTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VsmTheme {
                Surface(Modifier.fillMaxSize()) {
                    val session: SessionViewModel = hiltViewModel()
                    val state by session.state.collectAsStateWithLifecycle()
                    if (state.signedIn) {
                        LaunchedEffect(Unit) {
                            if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        AppNavigation(onSignOut = session::signOut)
                    } else {
                        Surface(Modifier.safeDrawingPadding()) {
                            LoginScreen(state, session::onNumberChange, session::onPasswordChange, session::submit)
                        }
                    }
                }
            }
        }
    }
}
