package ru.vsm.trainer

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import ru.vsm.trainer.data.local.AppPreferences
import ru.vsm.trainer.data.local.ThemeMode
import ru.vsm.trainer.design.VsmTheme
import ru.vsm.trainer.feature.auth.LoginScreen
import ru.vsm.trainer.feature.auth.SessionViewModel
import ru.vsm.trainer.navigation.AppNavigation

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferences: AppPreferences
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by preferences.theme.collectAsStateWithLifecycle()
            val reduceMotion by preferences.reduceMotion.collectAsStateWithLifecycle()
            // Значки статус-бара следуют теме приложения, а не системной: иначе на тёмном фоне они не видны
            LaunchedEffect(theme) {
                val style = if (theme == ThemeMode.DARK) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            VsmTheme(dark = theme == ThemeMode.DARK, reduceMotion = reduceMotion) {
                val session: SessionViewModel = hiltViewModel()
                val state by session.state.collectAsStateWithLifecycle()
                if (state.signedIn) {
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    AppNavigation(onLogout = session::signOut)
                } else {
                    LoginScreen(state, session::onNumberChange, session::onPasswordChange, session::submit)
                }
            }
        }
    }
}
