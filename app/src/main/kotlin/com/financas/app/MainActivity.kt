package com.financas.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.core.security.SessionLockPolicy
import com.financas.app.ui.components.FinflowStartupSplash
import com.financas.app.ui.navigation.MainScaffold
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.DarkBackground
import com.financas.app.ui.theme.FinancasTheme
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val sessionLocked = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        val appContainer = (application as FinancasApplication).container
        val biometricGate = appContainer.biometricLockGate
        val userProfileStore = appContainer.userProfileStore
        sessionLocked.value = biometricGate.isBiometricsEnabled()

        setContent {
            val themeMode by userProfileStore.themeModeFlow.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "LIGHT" -> false
                "SYSTEM" -> isSystemDark
                else -> true
            }

            FinancasTheme(darkTheme = isDark) {
                val isLocked = sessionLocked.value == true
                var showSplash by rememberSaveable { mutableStateOf(true) }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (!isLocked) {
                        MainScaffold(container = appContainer)
                    } else {
                        BiometricLockScreen(
                            canPrompt = !showSplash,
                            onAuthenticate = {
                                biometricGate.promptBiometric(
                                    activity = this@MainActivity,
                                    onSuccess = { sessionLocked.value = false },
                                    onError = { }
                                )
                            }
                        )
                    }

                    if (showSplash) {
                        FinflowStartupSplash(
                            onAnimationComplete = { showSplash = false }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (sessionLocked.value == false) {
            val app = application as FinancasApplication
            app.container.sessionBackgroundClock.lastBackgroundAtMs = System.currentTimeMillis()
        }
    }

    override fun onResume() {
        super.onResume()
        com.financas.app.data.service.FinancialNotificationListenerService.requestRebindIfGranted(this)

        val app = application as FinancasApplication
        val clock = app.container.sessionBackgroundClock
        val lastBackgroundAtMs = clock.lastBackgroundAtMs
        if (lastBackgroundAtMs > 0L) {
            clock.lastBackgroundAtMs = 0L
            val elapsed = System.currentTimeMillis() - lastBackgroundAtMs
            val biometricsEnabled = app.container.biometricLockGate.isBiometricsEnabled()
            if (SessionLockPolicy.shouldLock(elapsed, biometricsEnabled)) {
                sessionLocked.value = true
            }
        }
    }
}

@Composable
fun BiometricLockScreen(
    canPrompt: Boolean = true,
    onAuthenticate: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(canPrompt) {
        if (canPrompt) {
            delay(350)
            onAuthenticate()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometria",
                    tint = AccentBlue,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Aplicação Bloqueada",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Autentique-se com a sua biometria para aceder às suas finanças.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onAuthenticate,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Desbloquear", color = Color.White)
            }
        }
    }
}
