package com.financas.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.domain.model.MoneyCents
import com.financas.app.ui.i18n.LocalPrivacyMode
import com.financas.app.ui.theme.AccentAmber
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.AccentCoral
import com.financas.app.ui.theme.AccentEmerald
import com.financas.app.ui.theme.DarkSurfaceHighlight
import com.financas.app.ui.theme.TextMuted
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary

@Composable
fun EffortBar(
    fixedPercent: Double,
    variablePercent: Double,
    savingsPercent: Double,
    fixedCents: Long,
    variableCents: Long,
    savingsCents: Long,
    modifier: Modifier = Modifier
) {
    val fixedColor = AccentBlue
    val variableColor = AccentCoral
    val savingsColor = AccentEmerald

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Segmented Canvas Bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(DarkSurfaceHighlight)
        ) {
            val totalWidth = size.width
            val totalHeight = size.height

            val safeFixed = fixedPercent.coerceAtLeast(0.0)
            val safeVariable = variablePercent.coerceAtLeast(0.0)
            val safeSavings = savingsPercent.coerceAtLeast(0.0)
            val sum = safeFixed + safeVariable + safeSavings

            if (sum > 0.0) {
                val fixedWidth = ((safeFixed / sum) * totalWidth).toFloat()
                val variableWidth = ((safeVariable / sum) * totalWidth).toFloat()
                val savingsWidth = ((safeSavings / sum) * totalWidth).toFloat()

                // Draw Fixed segment
                if (fixedWidth > 0f) {
                    drawRect(
                        color = fixedColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(fixedWidth, totalHeight)
                    )
                }

                // Draw Variable segment
                if (variableWidth > 0f) {
                    drawRect(
                        color = variableColor,
                        topLeft = Offset(fixedWidth, 0f),
                        size = Size(variableWidth, totalHeight)
                    )
                }

                // Draw Savings segment
                if (savingsWidth > 0f) {
                    drawRect(
                        color = savingsColor,
                        topLeft = Offset(fixedWidth + variableWidth, 0f),
                        size = Size(savingsWidth, totalHeight)
                    )
                }
            }
        }

        // Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EffortLegendItem(
                label = "Fixos",
                percent = fixedPercent,
                cents = fixedCents,
                color = fixedColor
            )

            EffortLegendItem(
                label = "Variáveis",
                percent = variablePercent,
                cents = variableCents,
                color = variableColor
            )

            EffortLegendItem(
                label = "Poupado",
                percent = savingsPercent,
                cents = savingsCents,
                color = savingsColor
            )
        }
    }
}

@Composable
private fun EffortLegendItem(
    label: String,
    percent: Double,
    cents: Long,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = MoneyCents.formatPercent(percent),
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            val isPrivacy = LocalPrivacyMode.current
            Text(
                text = if (isPrivacy) MoneyCents.MASKED_STRING else MoneyCents.formatCents(cents, false),
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}
