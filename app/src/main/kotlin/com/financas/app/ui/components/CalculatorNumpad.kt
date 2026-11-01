package com.financas.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.AccentEmerald
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurfaceHighlight
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.FinflowSurfaceContainerLow
import com.financas.app.ui.theme.FinflowTextPrimary
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary

@Composable
fun CalculatorNumpad(
    modifier: Modifier = Modifier,
    onDigit: (Char) -> Unit,
    onDoubleZero: () -> Unit,
    onComma: () -> Unit,
    onPlus: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: 1, 2, 3, ⌫
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumpadKey("1", Modifier.weight(1f)) { onDigit('1') }
            NumpadKey("2", Modifier.weight(1f)) { onDigit('2') }
            NumpadKey("3", Modifier.weight(1f)) { onDigit('3') }
            NumpadIconKey(
                modifier = Modifier.weight(1f),
                iconColor = TextSecondary,
                backgroundColor = DarkSurfaceVariant,
                onClick = onBackspace
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Apagar",
                    tint = TextSecondary
                )
            }
        }

        // Row 2: 4, 5, 6, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumpadKey("4", Modifier.weight(1f)) { onDigit('4') }
            NumpadKey("5", Modifier.weight(1f)) { onDigit('5') }
            NumpadKey("6", Modifier.weight(1f)) { onDigit('6') }
            NumpadKey(
                text = "+",
                modifier = Modifier.weight(1f),
                textColor = AccentBlue,
                backgroundColor = DarkSurfaceVariant,
                fontSize = 26.sp,
                onClick = onPlus
            )
        }

        // Row 3: 7, 8, 9, C
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumpadKey("7", Modifier.weight(1f)) { onDigit('7') }
            NumpadKey("8", Modifier.weight(1f)) { onDigit('8') }
            NumpadKey("9", Modifier.weight(1f)) { onDigit('9') }
            NumpadKey(
                text = "C",
                modifier = Modifier.weight(1f),
                textColor = TextSecondary,
                backgroundColor = DarkSurfaceVariant,
                fontSize = 18.sp,
                onClick = onClear
            )
        }

        // Row 4: ,, 0, 00, ✓
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumpadKey(
                text = ",",
                modifier = Modifier.weight(1f),
                fontSize = 24.sp,
                onClick = onComma
            )
            NumpadKey("0", Modifier.weight(1f)) { onDigit('0') }
            NumpadKey(
                text = "00",
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                onClick = onDoubleZero
            )
            NumpadIconKey(
                modifier = Modifier.weight(1f),
                iconColor = DarkBorder,
                backgroundColor = AccentEmerald,
                onClick = onConfirm
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Concluir",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
private fun NumpadKey(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimary,
    backgroundColor: Color = DarkSurfaceHighlight,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun NumpadIconKey(
    modifier: Modifier = Modifier,
    iconColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun FinflowNumpad(
    modifier: Modifier = Modifier,
    onDigit: (Char) -> Unit,
    onComma: () -> Unit,
    onBackspace: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: 1, 2, 3
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FinflowNumpadKey("1", Modifier.weight(1f)) { onDigit('1') }
            FinflowNumpadKey("2", Modifier.weight(1f)) { onDigit('2') }
            FinflowNumpadKey("3", Modifier.weight(1f)) { onDigit('3') }
        }
        // Row 2: 4, 5, 6
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FinflowNumpadKey("4", Modifier.weight(1f)) { onDigit('4') }
            FinflowNumpadKey("5", Modifier.weight(1f)) { onDigit('5') }
            FinflowNumpadKey("6", Modifier.weight(1f)) { onDigit('6') }
        }
        // Row 3: 7, 8, 9
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FinflowNumpadKey("7", Modifier.weight(1f)) { onDigit('7') }
            FinflowNumpadKey("8", Modifier.weight(1f)) { onDigit('8') }
            FinflowNumpadKey("9", Modifier.weight(1f)) { onDigit('9') }
        }
        // Row 4: ,, 0, ⌫
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FinflowNumpadKey(",", Modifier.weight(1f)) { onComma() }
            FinflowNumpadKey("0", Modifier.weight(1f)) { onDigit('0') }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FinflowSurfaceContainerLow)
                    .clickable(onClick = onBackspace),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Apagar",
                    tint = FinflowTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FinflowNumpadKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FinflowSurfaceContainerLow)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = FinflowTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
