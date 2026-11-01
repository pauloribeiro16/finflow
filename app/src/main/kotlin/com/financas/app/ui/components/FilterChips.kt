package com.financas.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.DarkSurfaceHighlight
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary

data class ChipOption(
    val id: String,
    val label: String,
    val iconEmoji: String? = null
)

@Composable
fun FilterChipsRow(
    options: List<ChipOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = AccentBlue
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (option in options) {
            val isSelected = option.id == selectedId
            val bgColor = if (isSelected) activeColor.copy(alpha = 0.2f) else DarkSurfaceVariant
            val borderColor = if (isSelected) activeColor else DarkBorder
            val textColor = if (isSelected) TextPrimary else TextSecondary

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .clickable { onSelect(option.id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (option.iconEmoji != null) {
                    Text(
                        text = option.iconEmoji,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = option.label,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
