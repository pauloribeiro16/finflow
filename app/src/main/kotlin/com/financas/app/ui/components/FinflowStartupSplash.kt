package com.financas.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.financas.app.R
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FinflowStartupSplash(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val orbScale = remember { Animatable(0f) }
    val orbAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val glowPulse = remember { Animatable(0.9f) }
    val burstScale = remember { Animatable(1f) }
    val shockwave1Scale = remember { Animatable(1f) }
    val shockwave1Alpha = remember { Animatable(0f) }
    val shockwave2Scale = remember { Animatable(1f) }
    val shockwave2Alpha = remember { Animatable(0f) }
    val backdropAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Fase 1: Entrada da Bola no centro com mola elástica
        launch {
            orbAlpha.animateTo(1f, tween(260))
        }
        launch {
            orbScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            delay(180)
            contentAlpha.animateTo(1f, tween(320))
        }

        // Fase 2: Pulso e brilho da aura (respiração da esfera)
        launch {
            glowPulse.animateTo(1.3f, tween(450, easing = FastOutSlowInEasing))
        }

        delay(700)

        // Micro-compressão de antecipação
        orbScale.animateTo(0.94f, tween(110, easing = FastOutSlowInEasing))

        // Fase 3: A Expansão / Explosão ("a bola depois a aparecer para todo o lado")
        coroutineScope {
            // A esfera expande dramaticamente preenchendo todo o ecrã
            launch {
                burstScale.animateTo(
                    targetValue = 35f,
                    animationSpec = tween(550, easing = FastOutLinearInEasing)
                )
            }
            // Anel de choque 1 (Cyan / Esmeralda)
            launch {
                shockwave1Alpha.snapTo(0.85f)
                launch { shockwave1Scale.animateTo(40f, tween(580, easing = FastOutLinearInEasing)) }
                launch { shockwave1Alpha.animateTo(0f, tween(480, easing = LinearEasing)) }
            }
            // Anel de choque 2 (Indigo / Violeta elétrico)
            launch {
                delay(60)
                shockwave2Alpha.snapTo(0.7f)
                launch { shockwave2Scale.animateTo(45f, tween(600, easing = FastOutLinearInEasing)) }
                launch { shockwave2Alpha.animateTo(0f, tween(520, easing = LinearEasing)) }
            }
            // Texto esvanece rapidamente no início da expansão
            launch {
                contentAlpha.animateTo(0f, tween(140))
            }
            // Fundo escuro dissolve gradualmente revelando o ecrã principal
            launch {
                delay(220)
                backdropAlpha.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            }
        }

        onAnimationComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(backdropAlpha.value)
            .background(Color(0xFF090D16))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onAnimationComplete()
            },
        contentAlignment = Alignment.Center
    ) {
        // Ondas de choque circulares (Canvas fluido de alta performance)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val baseRadius = 52.dp.toPx()

            if (shockwave2Alpha.value > 0f) {
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = shockwave2Alpha.value * 0.50f),
                    radius = baseRadius * shockwave2Scale.value,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            if (shockwave1Alpha.value > 0f) {
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = shockwave1Alpha.value * 0.70f),
                    radius = baseRadius * shockwave1Scale.value,
                    center = center,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }

        // Esfera Central ("A Bola") idêntica ao ícone exterior oficial
        Box(
            modifier = Modifier
                .size(112.dp)
                .scale(orbScale.value * burstScale.value)
                .alpha(orbAlpha.value)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E293B),
                            Color(0xFF0F172A),
                            Color(0xFF07090E)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Halo de brilho exterior em Sky Blue (idêntico ao gradiente exterior)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(glowPulse.value)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF38BDF8).copy(alpha = 0.40f)
                            )
                        )
                    )
            )

            // Logótipo oficial Finflow no centro da esfera
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Finflow Logo",
                modifier = Modifier
                    .size(76.dp)
                    .alpha(contentAlpha.value)
            )
        }

        // Tipografia da marca abaixo da esfera durante a entrada
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(contentAlpha.value)
        ) {
            Spacer(modifier = Modifier.height(176.dp))
            Text(
                text = "FINFLOW",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Finanças Pessoais",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
