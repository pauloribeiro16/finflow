package com.financas.app.ui.screens.account

import com.financas.app.core.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.AccentCoral
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.TextMuted
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
internal fun AddSubcategoryDialog(
    viewModel: AccountViewModel,
    uiState: AccountUiState,
    targetParentForSubcategory: CategoryEntity?,
    onDismiss: () -> Unit
) {
    var subName by remember { mutableStateOf("") }
    var isFixed by remember { mutableStateOf(false) }
    var selectedParentId by remember {
        mutableStateOf(targetParentForSubcategory?.id ?: uiState.parentCategories.firstOrNull()?.id ?: "")
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = DarkSurface,
        title = {
            Text(tr("Nova Subcategoria", "New Subcategory"), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tr("Categoria Pai:", "Parent Category:"), fontSize = 12.sp, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.parentCategories.forEach { parent ->
                        val isSelected = parent.id == selectedParentId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AccentBlue else DarkSurfaceVariant)
                                .border(1.dp, if (isSelected) AccentBlue else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedParentId = parent.id }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = parent.name,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = subName,
                    onValueChange = { subName = it },
                    label = { Text(tr("Nome da Subcategoria", "Subcategory Name")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr("Gasto/Rendimento Fixo?", "Fixed Expense/Income?"), fontSize = 13.sp, color = TextPrimary)
                    Switch(
                        checked = isFixed,
                        onCheckedChange = { isFixed = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subName.isNotBlank() && selectedParentId.isNotBlank()) {
                        viewModel.addSubcategory(
                            parentId = selectedParentId,
                            name = subName,
                            isFixed = isFixed
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(tr("Adicionar", "Add"), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(tr("Cancelar", "Cancel"), color = TextSecondary)
            }
        }
    )
}

@Composable
internal fun DeleteSubcategoryDialog(
    viewModel: AccountViewModel,
    sub: CategoryEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = DarkSurface,
        title = {
            Text(
                "Apagar subcategoria?",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                tr("Esta ação é definitiva e não pode ser anulada.", "This action is permanent and cannot be undone."),
                color = TextSecondary,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.deleteSubcategory(sub)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCoral)
            ) {
                Text(tr("Apagar", "Delete"), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(tr("Cancelar", "Cancel"), color = TextSecondary)
            }
        }
    )
}

@Composable
internal fun AddContextDialog(
    viewModel: AccountViewModel,
    onDismiss: () -> Unit
) {
    var contextTagInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = DarkSurface,
        title = {
            Text(tr("Novo Contexto «Onde»", "New «Where» Context"), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = tr("Exemplos: ESCRITORIO, FERIAS, ALGARVE, SUPERMERCADO", "Examples: ESCRITORIO, FERIAS, ALGARVE, SUPERMERCADO"),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = contextTagInput,
                    onValueChange = { contextTagInput = it },
                    label = { Text(tr("Nome do Contexto", "Context Name")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (contextTagInput.isNotBlank()) {
                        viewModel.addContextTag(contextTagInput)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(tr("Guardar", "Save"), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(tr("Cancelar", "Cancel"), color = TextSecondary)
            }
        }
    )
}

@Composable
internal fun RestoreBackupDialog(
    onPickFile: () -> Unit,
    onRestoreText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var jsonRestoreInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = DarkSurface,
        title = {
            Text(tr("Restaurar Backup JSON", "Restore JSON Backup"), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = tr("Pode escolher um ficheiro do dispositivo ou colar diretamente o conteúdo JSON:", "You can pick a file from the device or paste the JSON content directly:"),
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Button(
                    onClick = {
                        onDismiss()
                        onPickFile()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = AccentBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Escolher ficheiro (.finflow ou .json)", "Choose file (.finflow or .json)"), color = AccentBlue)
                }

                Text(tr("Ou colar o texto JSON:", "Or paste the JSON text:"), fontSize = 12.sp, color = TextSecondary)

                OutlinedTextField(
                    value = jsonRestoreInput,
                    onValueChange = { jsonRestoreInput = it },
                    placeholder = { Text("{\"version\": 2, ...}", color = TextMuted) },
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (jsonRestoreInput.isNotBlank()) {
                        val content = jsonRestoreInput
                        onDismiss()
                        jsonRestoreInput = ""
                        onRestoreText(content)
                    }
                },
                enabled = jsonRestoreInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(tr("Restaurar Texto", "Restore Text"), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(tr("Cancelar", "Cancel"), color = TextSecondary)
            }
        }
    )
}

@Composable
internal fun AddBudgetDialog(
    viewModel: AccountViewModel,
    categoriesAvailableForBudget: List<CategoryEntity>,
    onDismiss: () -> Unit
) {
    var selectedCategoryId by remember {
        mutableStateOf(categoriesAvailableForBudget.firstOrNull()?.id ?: "")
    }
    var limitInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = DarkSurface,
        title = {
            Text(tr("Novo Orçamento", "New Budget"), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (categoriesAvailableForBudget.isEmpty()) {
                    Text(
                        tr("Todas as categorias de despesa já têm um orçamento ativo.", "All expense categories already have an active budget."),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                } else {
                    Text(tr("Categoria:", "Category:"), fontSize = 12.sp, color = TextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categoriesAvailableForBudget.forEach { category ->
                            val isSelected = category.id == selectedCategoryId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) AccentBlue else DarkSurfaceVariant)
                                    .border(1.dp, if (isSelected) AccentBlue else DarkBorder, RoundedCornerShape(6.dp))
                                    .clickable { selectedCategoryId = category.id }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = category.name,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text(tr("Limite Mensal (€)", "Monthly Limit (€)")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limitCents = parseEurosToCents(limitInput)
                    if (selectedCategoryId.isNotBlank() && limitCents != null) {
                        viewModel.addBudget(selectedCategoryId, limitCents)
                        onDismiss()
                    }
                },
                enabled = categoriesAvailableForBudget.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(tr("Definir", "Set"), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(tr("Cancelar", "Cancel"), color = TextSecondary)
            }
        }
    )
}

@Composable
internal fun EditBudgetDialog(
    viewModel: AccountViewModel,
    budget: BudgetEntity,
    categoriesById: Map<String, CategoryEntity>,
    onDismiss: () -> Unit
) {
    var limitInput by remember(budget.id) {
        mutableStateOf(
            DecimalFormat("0.00", DecimalFormatSymbols(Locale("pt", "PT")))
                .format(budget.limitCents / 100.0)
        )
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = DarkSurface,
        title = {
            Text(
                tr("Editar Orçamento", "Edit Budget"),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = tr("Categoria: ${categoriesById[budget.categoryId]?.name ?: budget.categoryId}", "Category: ${categoriesById[budget.categoryId]?.name ?: budget.categoryId}"),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text(tr("Limite Mensal (€)", "Monthly Limit (€)")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newLimitCents = parseEurosToCents(limitInput)
                    if (newLimitCents != null) {
                        viewModel.updateBudgetLimit(budget, newLimitCents)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(tr("Guardar", "Save"), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(tr("Cancelar", "Cancel"), color = TextSecondary)
            }
        }
    )
}

@Composable
internal fun DeleteBudgetDialog(
    viewModel: AccountViewModel,
    budget: BudgetEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = DarkSurface,
        title = {
            Text(
                tr("Apagar orçamento?", "Delete budget?"),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                tr("Esta ação é definitiva e não pode ser anulada.", "This action is permanent and cannot be undone."),
                color = TextSecondary,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.deleteBudget(budget)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCoral)
            ) {
                Text(tr("Apagar", "Delete"), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(tr("Cancelar", "Cancel"), color = TextSecondary)
            }
        }
    )
}

@Composable
internal fun AddGoalDialog(
    viewModel: AccountViewModel,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var targetInput by remember { mutableStateOf("") }
    var hasDeadline by remember { mutableStateOf(false) }
    var yearInput by remember { mutableStateOf("") }
    var monthInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = DarkSurface,
        title = {
            Text(tr("Nova Meta", "New Goal"), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(tr("Nome da Meta", "Goal Name")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text(tr("Alvo (€)", "Target (€)")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr("Definir prazo?", "Set a deadline?"), fontSize = 13.sp, color = TextPrimary)
                    Switch(
                        checked = hasDeadline,
                        onCheckedChange = { hasDeadline = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                    )
                }

                if (hasDeadline) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = yearInput,
                            onValueChange = { yearInput = it },
                            label = { Text(tr("Ano (AAAA)", "Year (YYYY)")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = monthInput,
                            onValueChange = { monthInput = it },
                            label = { Text(tr("Mês (1-12)", "Month (1-12)")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetCents = parseEurosToCents(targetInput)
                    val deadline = if (hasDeadline) {
                        parseDeadlineYearMonth(yearInput, monthInput)
                    } else {
                        null
                    }
                    if (nameInput.isNotBlank() && targetCents != null && (!hasDeadline || deadline != null)) {
                        viewModel.createGoal(nameInput, targetCents, deadline)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(tr("Criar", "Create"), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(tr("Cancelar", "Cancel"), color = TextSecondary)
            }
        }
    )
}

@Composable
internal fun DeleteGoalDialog(
    viewModel: AccountViewModel,
    goal: GoalEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = DarkSurface,
        title = {
            Text(
                "Apagar meta?",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                tr("Esta ação é definitiva e não pode ser anulada.", "This action is permanent and cannot be undone."),
                color = TextSecondary,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.deleteGoal(goal)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCoral)
            ) {
                Text(tr("Apagar", "Delete"), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(tr("Cancelar", "Cancel"), color = TextSecondary)
            }
        }
    )
}
