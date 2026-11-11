package com.financas.app.ui.screens.account

import com.financas.app.core.i18n.tr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.domain.model.MoneyCents
import com.financas.app.ui.i18n.LocalPrivacyMode
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.AccentCoral
import com.financas.app.ui.theme.AccentEmerald
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.DarkSurfaceHighlight
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.TextMuted
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun BudgetRowItem(
    budget: BudgetEntity,
    categoryName: String,
    colorHex: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, DarkBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(colorHex))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = categoryName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                val isPrivacy = LocalPrivacyMode.current
                Text(
                    text = tr("Limite: ${if (isPrivacy) MoneyCents.MASKED_STRING else MoneyCents.formatCents(budget.limitCents, false)}", "Limit: ${if (isPrivacy) MoneyCents.MASKED_STRING else MoneyCents.formatCents(budget.limitCents, false)}"),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = tr("Editar Orçamento", "Edit Budget"),
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = tr("Eliminar Orçamento", "Delete Budget"),
                    tint = AccentCoral.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
internal fun GoalRowItem(
    item: GoalWithProgress,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val goal = item.goal
    val deadlineText = goal.deadlineYearMonth?.let { "Prazo: ${formatMonthYear(it)}" } ?: "Sem prazo"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, DarkBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = goal.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (goal.archived) TextMuted else TextPrimary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (goal.archived) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Arquivada", "Archived"),
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (item.progress.percentComplete / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (goal.archived) TextMuted else AccentEmerald,
                trackColor = DarkSurfaceHighlight
            )

            val isPrivacy = LocalPrivacyMode.current
            Text(
                text = tr("${MoneyCents.formatPercent(item.progress.percentComplete)} · Alvo: ${if (isPrivacy) MoneyCents.MASKED_STRING else MoneyCents.formatCents(goal.targetCents, false)} · $deadlineText", "${MoneyCents.formatPercent(item.progress.percentComplete)} · Target: ${if (isPrivacy) MoneyCents.MASKED_STRING else MoneyCents.formatCents(goal.targetCents, false)} · $deadlineText"),
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!goal.archived) {
                IconButton(
                    onClick = onArchive,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = tr("Arquivar Meta", "Archive Goal"),
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = tr("Eliminar Meta", "Delete Goal"),
                    tint = AccentCoral.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

internal fun parseEurosToCents(raw: String): Long? {
    val cleaned = raw.trim().replace("€", "").trim()
    if (cleaned.isEmpty()) return null
    val value = cleaned.replace(',', '.').toDoubleOrNull() ?: return null
    if (value <= 0.0) return null
    return Math.round(value * 100.0)
}

internal fun parseDeadlineYearMonth(yearInput: String, monthInput: String): Int? {
    val year = yearInput.trim().toIntOrNull() ?: return null
    val month = monthInput.trim().toIntOrNull() ?: return null
    if (month !in 1..12) return null
    if (year !in 1900..2999) return null
    return year * 100 + month
}

internal fun formatMonthYear(yearMonth: Int): String {
    val ym = YearMonth.of(yearMonth / 100, yearMonth % 100)
    return ym.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale("pt", "PT")))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString() }
}

@Composable
internal fun ParentCategoryGroupItem(
    parent: CategoryEntity,
    subcategories: List<CategoryEntity>,
    onAddSubcategory: () -> Unit,
    onDeleteSubcategory: (CategoryEntity) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, DarkBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(parent.colorHex))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = parent.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${subcategories.size} subcategorias • ${if (parent.type == "INCOME") "Receita" else "Despesa"}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onAddSubcategory,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = tr("Adicionar Subcategoria", "Add Subcategory"),
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (subcategories.isEmpty()) {
                    Text(
                        text = tr("Sem subcategorias criadas.", "No subcategories yet."),
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(start = 18.dp)
                    )
                } else {
                    subcategories.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurface)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(sub.colorHex).copy(alpha = 0.7f))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sub.name,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                if (sub.isDefaultFixed) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tr("Fixo", "Fixed"),
                                        fontSize = 10.sp,
                                        color = AccentBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteSubcategory(sub) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = tr("Eliminar", "Delete"),
                                    tint = AccentCoral.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ActionButtonItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
