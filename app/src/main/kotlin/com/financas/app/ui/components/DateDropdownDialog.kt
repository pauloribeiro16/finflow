package com.financas.app.ui.components

import com.financas.app.core.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDropdownDialog(
    initialEpochDay: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onSwitchToCalendar: (() -> Unit)? = null
) {
    val initialParts = remember(initialEpochDay) { DateDropdownUtils.fromEpochDay(initialEpochDay) }
    var selectedYear by remember { mutableIntStateOf(initialParts.year) }
    var selectedMonth by remember { mutableIntStateOf(initialParts.month) }
    var selectedDay by remember { mutableIntStateOf(initialParts.day) }

    val today = remember { LocalDate.now() }
    val todayEpoch = remember { today.toEpochDay() }
    val yesterdayEpoch = remember { today.minusDays(1).toEpochDay() }
    val anteontemEpoch = remember { today.minusDays(2).toEpochDay() }

    val currentEpoch = DateDropdownUtils.toEpochDay(selectedDay, selectedMonth, selectedYear)
    val maxDays = DateDropdownUtils.daysInMonth(selectedMonth, selectedYear)

    var dayExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("Escolher Data", "Choose Date"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (onSwitchToCalendar != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSwitchToCalendar() }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = tr("Ver calendário", "View calendar"),
                            tint = AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = tr("Calendário", "Calendar"),
                            fontSize = 12.sp,
                            color = AccentBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick Shortcut Chips (Hoje, Ontem, Anteontem)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    Triple(tr("Hoje", "Today"), todayEpoch, today),
                    Triple(tr("Ontem", "Yesterday"), yesterdayEpoch, today.minusDays(1)),
                    Triple("Anteontem", anteontemEpoch, today.minusDays(2))
                )

                chips.forEach { (label, epoch, date) ->
                    val isSelected = currentEpoch == epoch
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AccentBlue.copy(alpha = 0.25f) else DarkSurfaceVariant)
                            .border(
                                1.dp,
                                if (isSelected) AccentBlue else DarkBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedDay = date.dayOfMonth
                                selectedMonth = date.monthValue
                                selectedYear = date.year
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AccentBlue else TextSecondary
                        )
                    }
                }
            }

            // Formatted Date Label
            Text(
                text = DateDropdownUtils.formatFullPt(currentEpoch),
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            // 3 Dropdown Pickers: Dia | Mês | Ano
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // DIA Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceHighlight)
                            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                            .clickable { dayExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(text = tr("Dia", "Day"), fontSize = 10.sp, color = TextMuted)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedDay.toString(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false },
                        modifier = Modifier
                            .heightIn(max = 280.dp)
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        for (d in 1..maxDays) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = d.toString(),
                                        color = if (d == selectedDay) AccentBlue else TextPrimary,
                                        fontWeight = if (d == selectedDay) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedDay = d
                                    dayExpanded = false
                                }
                            )
                        }
                    }
                }

                // MÊS Dropdown
                Box(modifier = Modifier.weight(1.5f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceHighlight)
                            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                            .clickable { monthExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(text = tr("Mês", "Month"), fontSize = 10.sp, color = TextMuted)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateDropdownUtils.MONTH_NAMES_PT[selectedMonth - 1],
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = monthExpanded,
                        onDismissRequest = { monthExpanded = false },
                        modifier = Modifier
                            .heightIn(max = 280.dp)
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        DateDropdownUtils.MONTH_NAMES_PT.forEachIndexed { index, name ->
                            val monthNum = index + 1
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = name,
                                        color = if (monthNum == selectedMonth) AccentBlue else TextPrimary,
                                        fontWeight = if (monthNum == selectedMonth) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedMonth = monthNum
                                    selectedDay = DateDropdownUtils.clampDay(selectedDay, selectedMonth, selectedYear)
                                    monthExpanded = false
                                }
                            )
                        }
                    }
                }

                // ANO Dropdown
                Box(modifier = Modifier.weight(1.1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceHighlight)
                            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                            .clickable { yearExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(text = tr("Ano", "Year"), fontSize = 10.sp, color = TextMuted)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedYear.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = yearExpanded,
                        onDismissRequest = { yearExpanded = false },
                        modifier = Modifier
                            .heightIn(max = 280.dp)
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        DateDropdownUtils.DEFAULT_YEARS.forEach { y ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = y.toString(),
                                        color = if (y == selectedYear) AccentBlue else TextPrimary,
                                        fontWeight = if (y == selectedYear) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedYear = y
                                    selectedDay = DateDropdownUtils.clampDay(selectedDay, selectedMonth, selectedYear)
                                    yearExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Dialog Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(tr("Cancelar", "Cancel"), color = TextSecondary)
                }

                Button(
                    onClick = {
                        onDateSelected(currentEpoch)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(tr("Confirmar", "Confirm"), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
