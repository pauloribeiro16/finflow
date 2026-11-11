package com.financas.app.ui.components

import com.financas.app.core.i18n.tr

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.ui.i18n.AppLanguage
import com.financas.app.ui.i18n.LocalAppLanguage
import com.financas.app.ui.i18n.LocalAppStrings
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.DarkSurfaceHighlight
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.TextMuted
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary
import com.financas.app.ui.util.DateDropdownUtils
import java.time.LocalDate

@Composable
fun MonthSelector(
    monthTitle: String,
    selectedYearMonth: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectYearMonth: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val language = LocalAppLanguage.current
    val localizedTitle = remember(selectedYearMonth, monthTitle, language) {
        if (selectedYearMonth > 0) {
            val year = selectedYearMonth / 100
            val month = selectedYearMonth % 100
            val ym = java.time.YearMonth.of(year, month)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", language.locale)
            ym.format(formatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(language.locale) else it.toString() }
        } else {
            monthTitle
        }
    }
    var showDialog by remember { mutableStateOf(false) }

    if (compact) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDialog = true }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AnimatedContent(
                    targetState = localizedTitle,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        ) { height -> height / 2 } + fadeIn(tween(180)))
                            .togetherWith(
                                slideOutVertically(
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                                ) { height -> -height / 2 } + fadeOut(tween(140))
                            )
                    },
                    label = "MonthFlipCompact"
                ) { title ->
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = tr("Selecionar Mês", "Select Month"),
                    tint = AccentBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = tr("Próximo Mês", "Next Month"),
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = tr("Mês Anterior", "Previous Month"),
                    tint = TextPrimary
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showDialog = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimatedContent(
                    targetState = localizedTitle,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        ) { height -> height / 2 } + fadeIn(tween(180)))
                            .togetherWith(
                                slideOutVertically(
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                                ) { height -> -height / 2 } + fadeOut(tween(140))
                            )
                    },
                    label = "MonthFlipRegular"
                ) { title ->
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = tr("Selecionar Mês", "Select Month"),
                    tint = AccentBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = tr("Mês Seguinte", "Next Month"),
                    tint = TextPrimary
                )
            }
        }
    }

    if (showDialog) {
        MonthPickerDialog(
            currentYearMonth = selectedYearMonth,
            onDismiss = { showDialog = false },
            onMonthSelected = { ym ->
                onSelectYearMonth(ym)
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerDialog(
    currentYearMonth: Int,
    onDismiss: () -> Unit,
    onMonthSelected: (Int) -> Unit
) {
    val initialYear = currentYearMonth / 100
    val initialMonth = currentYearMonth % 100
    var activeYear by remember { mutableIntStateOf(initialYear) }
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val isEnglish = language == AppLanguage.EN

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isEnglish) "Select Month and Year" else "Selecionar Mês e Ano",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Year selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateDropdownUtils.DEFAULT_YEARS.forEach { year ->
                    val isSelected = year == activeYear
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AccentBlue else DarkSurfaceVariant)
                            .clickable { activeYear = year }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = year.toString(),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }

            // 12 Months in 4x3 Grid
            val months = DateDropdownUtils.getMonthShortNames(isEnglish)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0 until 4) {
                            val monthIdx = row * 4 + col + 1
                            val monthLabel = months[monthIdx - 1]
                            val isCurrentSelection = (activeYear == initialYear && monthIdx == initialMonth)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isCurrentSelection -> AccentBlue
                                            else -> DarkSurfaceHighlight
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isCurrentSelection) AccentBlue else DarkBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        onMonthSelected(activeYear * 100 + monthIdx)
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = monthLabel,
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrentSelection) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrentSelection) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Footer actions: "Current Month" and "Cancel"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        val now = LocalDate.now()
                        onMonthSelected(now.year * 100 + now.monthValue)
                    }
                ) {
                    Text(if (isEnglish) "Current Month" else "Mês Atual", color = AccentBlue, fontSize = 13.sp)
                }

                TextButton(onClick = onDismiss) {
                    Text(strings.cancel, color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}
