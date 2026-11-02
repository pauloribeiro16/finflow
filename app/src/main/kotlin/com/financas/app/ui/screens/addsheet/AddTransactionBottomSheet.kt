package com.financas.app.ui.screens.addsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.domain.model.MoneyCents
import com.financas.app.ui.components.DateDropdownDialog
import com.financas.app.ui.components.FinflowNumpad
import com.financas.app.ui.theme.FinflowAmber
import com.financas.app.ui.theme.FinflowAmberLight
import com.financas.app.ui.theme.FinflowBorder
import com.financas.app.ui.theme.FinflowBorderSubtle
import com.financas.app.ui.theme.FinflowCoral
import com.financas.app.ui.theme.FinflowCoralLight
import com.financas.app.ui.theme.FinflowPrimary
import com.financas.app.ui.theme.FinflowPrimaryLight
import com.financas.app.ui.theme.FinflowPurple
import com.financas.app.ui.theme.FinflowPurpleLight
import com.financas.app.ui.theme.FinflowSecondary
import com.financas.app.ui.theme.FinflowSecondaryLight
import com.financas.app.ui.theme.FinflowSky
import com.financas.app.ui.theme.FinflowSkyLight
import com.financas.app.ui.theme.FinflowSurface
import com.financas.app.ui.theme.FinflowSurfaceContainerLow
import com.financas.app.ui.theme.FinflowTeal
import com.financas.app.ui.theme.FinflowTealLight
import com.financas.app.ui.theme.FinflowTextMuted
import com.financas.app.ui.theme.FinflowTextPrimary
import com.financas.app.ui.theme.FinflowTextSecondary
import com.financas.app.ui.i18n.AppLanguage
import com.financas.app.ui.i18n.CategoryTranslator
import com.financas.app.ui.i18n.LocalAppLanguage
import com.financas.app.ui.i18n.LocalAppStrings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class StitchCategoryOption(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val bgColor: Color,
    val tintColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    viewModel: AddTransactionViewModel,
    onDismissRequest: () -> Unit,
    onSaveSuccess: () -> Unit = onDismissRequest,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val isEnglish = language == AppLanguage.EN
    var showDateDropdownDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAllCategoriesSheet by remember { mutableStateOf(false) }
    var showPaymentMethodDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = FinflowSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Brand & Title + Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Finflow",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowPrimary
                    )
                    Text(
                        text = if (uiState.editingTransactionId != null) strings.editTransaction else strings.newTransaction,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(FinflowSurfaceContainerLow)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.close,
                        tint = FinflowTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Segmented Type Selector [ ● Expense | ● Income | ● Transfer ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FinflowSurfaceContainerLow)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isExpense = uiState.type == "EXPENSE"
                val isIncome = uiState.type == "INCOME"
                val isTransfer = uiState.type == "TRANSFER"

                TypeSegmentChip(
                    label = strings.expense,
                    dotColor = FinflowCoral,
                    isSelected = isExpense,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onTypeChanged("EXPENSE") }
                )

                TypeSegmentChip(
                    label = strings.income,
                    dotColor = FinflowSecondary,
                    isSelected = isIncome,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onTypeChanged("INCOME") }
                )

                TypeSegmentChip(
                    label = if (isEnglish) "Transfer" else "Transferência",
                    dotColor = FinflowTextMuted,
                    isSelected = isTransfer,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onTypeChanged("TRANSFER") }
                )
            }

            // Card: QUANTIA A REGISTAR (Stitch Design)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FinflowSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, FinflowBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUANTIA A REGISTAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextMuted,
                            letterSpacing = 0.5.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FinflowSurfaceContainerLow)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "EUR (€) ⇅",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FinflowTextSecondary
                            )
                        }
                    }

                    // Large Amount Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "€ ",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextMuted
                        )
                        Text(
                            text = MoneyCents.formatCents(uiState.amountCents).replace("€", "").trim(),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextPrimary
                        )
                        Text(
                            text = " |",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Light,
                            color = FinflowPrimary
                        )
                    }

                    // Quick-Add Pills: [+5 €] [+10 €] [+20 €] [+50 €]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(500L to "+5 €", 1000L to "+10 €", 2000L to "+20 €", 5000L to "+50 €").forEach { (delta, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(FinflowSurfaceContainerLow)
                                    .clickable { viewModel.addQuickAmountCents(delta) }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FinflowPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Dynamic Category & Subcategory Selector
            val selectedCategory = uiState.allCategories.find { it.id == uiState.selectedCategoryId }
            val selectedParent = uiState.parentCategories.find { it.id == uiState.selectedParentCategoryId }
                ?: uiState.parentCategories.find { it.id == selectedCategory?.parentId }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Categoria",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextPrimary
                        )
                        val categoryLabel = when {
                            selectedCategory != null && selectedParent != null && selectedCategory.id != selectedParent.id ->
                                "${selectedParent.name} › ${selectedCategory.name}"
                            selectedCategory != null -> selectedCategory.name
                            selectedParent != null -> selectedParent.name
                            else -> "Nenhuma selecionada"
                        }
                        Text(
                            text = categoryLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FinflowPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(
                            onClick = { showAddCategoryDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = FinflowPrimary)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Nova", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FinflowPrimary)
                        }

                        TextButton(
                            onClick = { showAllCategoriesSheet = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Ver Todas ▾", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = FinflowTextSecondary)
                        }
                    }
                }

                // Horizontal scroll of Parent Categories for active type
                if (uiState.parentCategories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.parentCategories.forEach { parent ->
                            val isSelected = uiState.selectedParentCategoryId == parent.id
                            val icon = getCategoryIcon(parent.name, parent.icon)
                            val parentColor = Color(parent.colorHex)

                            Column(
                                modifier = Modifier
                                    .width(76.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) FinflowPrimaryLight.copy(alpha = 0.5f) else FinflowSurfaceContainerLow)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) FinflowPrimary else FinflowBorderSubtle,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { viewModel.onParentCategorySelected(parent.id) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(parentColor.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = parent.name,
                                        tint = parentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = parent.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) FinflowPrimary else FinflowTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Horizontal Subcategory Chips under the selected parent
                if (uiState.subcategories.isNotEmpty()) {
                    Text(
                        text = "Subcategoria:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinflowTextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        uiState.subcategories.forEach { sub ->
                            val isSubSelected = uiState.selectedCategoryId == sub.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSubSelected) FinflowPrimary else FinflowSurfaceContainerLow)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSubSelected) FinflowPrimary else FinflowBorderSubtle,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.onSubcategorySelected(sub.id) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sub.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSubSelected) Color.White else FinflowTextPrimary
                                )
                            }
                        }

                        // Add Subcategory quick pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(FinflowPrimaryLight.copy(alpha = 0.3f))
                                .border(1.dp, FinflowPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { showAddCategoryDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = FinflowPrimary, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Nova Subcategoria",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FinflowPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Input: Descrição ou Estabelecimento
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Descrição ou Estabelecimento",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = FinflowTextSecondary
                )
                OutlinedTextField(
                    value = uiState.merchant,
                    onValueChange = { viewModel.onMerchantChanged(it) },
                    placeholder = { Text("Ex: Continente Colombo") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = FinflowPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FinflowSurfaceContainerLow,
                        unfocusedContainerColor = FinflowSurfaceContainerLow,
                        focusedBorderColor = FinflowPrimary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            // Row: Data do Movimento
            val dateFormatted = remember(uiState.epochDay) {
                LocalDate.ofEpochDay(uiState.epochDay).format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "PT"))).replaceFirstChar { it.uppercase() }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FinflowSurfaceContainerLow)
                    .clickable { showDateDropdownDialog = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = FinflowPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Data do Movimento",
                            fontSize = 11.sp,
                            color = FinflowTextMuted
                        )
                        Text(
                            text = dateFormatted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FinflowTextPrimary
                        )
                    }
                }
                Text(
                    text = ">",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinflowTextMuted
                )
            }

            // Row: Conta / Método
            val (methodLabel, methodIcon, methodTint) = when (uiState.selectedPaymentMethod.uppercase()) {
                "CASH", "DINHEIRO" -> Triple("Dinheiro", Icons.Default.AccountBalanceWallet, FinflowSecondary)
                "CARD", "DEBITO" -> Triple("Cartão Débito", Icons.Default.CreditCard, FinflowPrimary)
                "MBWAY" -> Triple("MB WAY", Icons.Default.CreditCard, FinflowCoral)
                "TRANSFER", "TRANSFERENCIA" -> Triple("Transferência", Icons.Default.Repeat, FinflowPurple)
                "DIRECT_DEBIT", "DEBITO_DIRETO" -> Triple("Débito Direto", Icons.Default.AccountBalanceWallet, FinflowAmber)
                else -> Triple(uiState.selectedPaymentMethod, Icons.Default.CreditCard, FinflowPrimary)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FinflowSurfaceContainerLow)
                    .clickable { showPaymentMethodDialog = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(methodTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = methodIcon,
                            contentDescription = null,
                            tint = methodTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Conta / Método",
                            fontSize = 11.sp,
                            color = FinflowTextMuted
                        )
                        Text(
                            text = methodLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FinflowTextPrimary
                        )
                    }
                }
                Text(
                    text = "▾",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinflowTextMuted
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FinflowSurfaceContainerLow)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = if (uiState.isFixed) FinflowPrimary else FinflowTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Despesa Fixa",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FinflowTextPrimary
                        )
                        Text(
                            text = "Marca o movimento como fixo para a análise de fixos vs. variáveis",
                            fontSize = 11.sp,
                            color = FinflowTextMuted
                        )
                    }
                }

                Switch(
                    checked = uiState.isFixed,
                    onCheckedChange = { viewModel.onFixedToggled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = FinflowPrimary
                    )
                )
            }

            // Integrated Finflow Numpad
            FinflowNumpad(
                modifier = Modifier.padding(horizontal = 16.dp),
                onDigit = { viewModel.onDigit(it) },
                onComma = { viewModel.onComma() },
                onBackspace = { viewModel.onBackspace() }
            )

            // Primary Button: (✓) Guardar Despesa + Eliminar (só em edição)
            val isEditing = uiState.editingTransactionId != null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing) {
                    TextButton(
                        onClick = { showDeleteConfirmation = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Eliminar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowCoral
                        )
                    }
                }
                Button(
                    onClick = { viewModel.saveTransaction(onSaveSuccess) },
                    modifier = Modifier
                        .then(if (isEditing) Modifier.weight(1f) else Modifier.fillMaxWidth())
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isEditing -> if (isEnglish) "Update Transaction" else "Atualizar Despesa"
                            uiState.type == "INCOME" -> if (isEnglish) "Save Income" else "Guardar Receita"
                            else -> if (isEnglish) "Save Expense" else "Guardar Despesa"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showDateDropdownDialog) {
        DateDropdownDialog(
            initialEpochDay = uiState.epochDay,
            onDismiss = { showDateDropdownDialog = false },
            onDateSelected = { epoch ->
                viewModel.onDateSelected(epoch)
                showDateDropdownDialog = false
            }
        )
    }

    if (showAddCategoryDialog) {
        CreateCategoryDialog(
            parentCategories = uiState.parentCategories,
            initialParentId = uiState.selectedParentCategoryId,
            currentType = uiState.type,
            onDismiss = { showAddCategoryDialog = false },
            onSave = { name, parentId ->
                viewModel.createCategory(name = name, parentId = parentId, type = uiState.type)
            }
        )
    }

    if (showAllCategoriesSheet) {
        AllCategoriesDialog(
            parentCategories = uiState.parentCategories,
            allCategories = uiState.allCategories,
            selectedCategoryId = uiState.selectedCategoryId,
            onDismiss = { showAllCategoriesSheet = false },
            onSelectCategory = { parent, sub ->
                viewModel.onParentCategorySelected(parent.id)
                if (sub != null) {
                    viewModel.onSubcategorySelected(sub.id)
                }
            },
            onOpenCreateCategory = { showAddCategoryDialog = true }
        )
    }

    if (showPaymentMethodDialog) {
        PaymentMethodDialog(
            selectedMethod = uiState.selectedPaymentMethod,
            onDismiss = { showPaymentMethodDialog = false },
            onMethodSelected = { method ->
                viewModel.onPaymentMethodSelected(method)
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = FinflowSurface,
            title = {
                Text(
                    text = "Eliminar movimento?",
                    fontWeight = FontWeight.Bold,
                    color = FinflowTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Esta ação não pode ser anulada.",
                        fontSize = 14.sp,
                        color = FinflowTextSecondary
                    )
                    if (uiState.hasLinkedRefunds) {
                        Text(
                            text = "As devoluções ligadas passam a receitas normais.",
                            fontSize = 14.sp,
                            color = FinflowTextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteTransaction(onSaveSuccess)
                    }
                ) {
                    Text(
                        text = "Eliminar",
                        fontWeight = FontWeight.Bold,
                        color = FinflowCoral
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar", color = FinflowTextSecondary)
                }
            }
        )
    }
}

private data class PaymentOption(
    val code: String,
    val label: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: androidx.compose.ui.graphics.Color
)

@Composable
private fun PaymentMethodDialog(
    selectedMethod: String,
    onDismiss: () -> Unit,
    onMethodSelected: (String) -> Unit
) {
    val methods = listOf(
        PaymentOption("CASH", "Dinheiro", "Pagamento em numerário", Icons.Default.AccountBalanceWallet, FinflowSecondary),
        PaymentOption("CARD", "Cartão Débito", "Cartão bancário de débito", Icons.Default.CreditCard, FinflowPrimary),
        PaymentOption("MBWAY", "MB WAY", "Pagamento via app móvel", Icons.Default.CreditCard, FinflowCoral),
        PaymentOption("TRANSFER", "Transferência", "Transferência bancária", Icons.Default.Repeat, FinflowPurple),
        PaymentOption("DIRECT_DEBIT", "Débito Direto", "Débito direto em conta", Icons.Default.AccountBalanceWallet, FinflowAmber)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FinflowSurface,
        title = {
            Text(
                text = "Método de Pagamento",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                methods.forEach { opt ->
                    val isSelected = selectedMethod.equals(opt.code, ignoreCase = true) ||
                            (opt.code == "CARD" && selectedMethod.equals("DEBITO", ignoreCase = true)) ||
                            (opt.code == "CASH" && selectedMethod.equals("DINHEIRO", ignoreCase = true)) ||
                            (opt.code == "TRANSFER" && selectedMethod.equals("TRANSFERENCIA", ignoreCase = true))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) opt.tint.copy(alpha = 0.15f) else FinflowSurfaceContainerLow)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) opt.tint else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onMethodSelected(opt.code)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(opt.tint.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = null,
                                    tint = opt.tint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = opt.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) opt.tint else FinflowTextPrimary
                                )
                                Text(
                                    text = opt.description,
                                    fontSize = 11.sp,
                                    color = FinflowTextMuted
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selecionado",
                                tint = opt.tint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = FinflowPrimary)
            }
        }
    )
}

@Composable
private fun CreateCategoryDialog(
    parentCategories: List<CategoryEntity>,
    initialParentId: String?,
    currentType: String,
    onDismiss: () -> Unit,
    onSave: (name: String, parentId: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf(initialParentId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (selectedParentId != null) "Nova Subcategoria" else "Nova Categoria",
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Categoria") },
                    placeholder = { Text("Ex: Animais, Farmácia, Cinema") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (parentCategories.isNotEmpty()) {
                    Text(
                        text = "Pertence a:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = FinflowTextSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isRoot = selectedParentId == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isRoot) FinflowPrimary else FinflowSurfaceContainerLow)
                                .clickable { selectedParentId = null }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Principal",
                                fontSize = 12.sp,
                                fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Normal,
                                color = if (isRoot) Color.White else FinflowTextPrimary
                            )
                        }

                        parentCategories.forEach { parent ->
                            val isSelected = selectedParentId == parent.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) FinflowPrimary else FinflowSurfaceContainerLow)
                                    .clickable { selectedParentId = parent.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = parent.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else FinflowTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotBlank()) {
                        onSave(name.trim(), selectedParentId)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
            ) {
                Text("Criar e Selecionar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = FinflowTextSecondary)
            }
        }
    )
}

@Composable
private fun AllCategoriesDialog(
    parentCategories: List<CategoryEntity>,
    allCategories: List<CategoryEntity>,
    selectedCategoryId: String,
    onDismiss: () -> Unit,
    onSelectCategory: (parent: CategoryEntity, sub: CategoryEntity?) -> Unit,
    onOpenCreateCategory: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val subsByParent = remember(allCategories) {
        allCategories.filter { it.parentId != null }.groupBy { it.parentId!! }
    }

    val filteredParents = remember(parentCategories, subsByParent, searchQuery) {
        if (searchQuery.isBlank()) parentCategories
        else {
            parentCategories.filter { parent ->
                parent.name.contains(searchQuery, ignoreCase = true) ||
                    (subsByParent[parent.id]?.any { it.name.contains(searchQuery, ignoreCase = true) } == true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selecionar Categoria",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = FinflowTextPrimary
                )
                TextButton(onClick = {
                    onDismiss()
                    onOpenCreateCategory()
                }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = FinflowPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nova", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FinflowPrimary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Pesquisar categoria...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = FinflowTextMuted, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filteredParents.forEach { parent ->
                        item(key = parent.id) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(FinflowSurfaceContainerLow)
                                        .clickable {
                                            onSelectCategory(parent, null)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(parent.colorHex))
                                        )
                                        Text(
                                            text = parent.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = FinflowTextPrimary
                                        )
                                    }
                                    if (selectedCategoryId == parent.id) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = FinflowPrimary, modifier = Modifier.size(16.dp))
                                    }
                                }

                                val subs = subsByParent[parent.id]?.filter {
                                    searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                                } ?: emptyList()

                                if (subs.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp)
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        subs.forEach { sub ->
                                            val isSelected = selectedCategoryId == sub.id
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) FinflowPrimary else Color.Transparent)
                                                    .border(1.dp, if (isSelected) FinflowPrimary else FinflowBorderSubtle, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        onSelectCategory(parent, sub)
                                                        onDismiss()
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = sub.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.White else FinflowTextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = FinflowPrimary)
            }
        }
    )
}

private fun getCategoryIcon(name: String, iconKey: String?): ImageVector {
    val lower = name.lowercase()
    return when {
        lower.contains("aliment") || lower.contains("comida") || lower.contains("restaur") -> Icons.Default.Restaurant
        lower.contains("mercado") || lower.contains("supermercado") || lower.contains("compras") -> Icons.Default.ShoppingCart
        lower.contains("habit") || lower.contains("casa") || lower.contains("renda") -> Icons.Default.Home
        lower.contains("transp") || lower.contains("carro") || lower.contains("combust") -> Icons.Default.DirectionsCar
        lower.contains("saúde") || lower.contains("saude") || lower.contains("farm") || lower.contains("médic") -> Icons.Default.LocalHospital
        lower.contains("lazer") || lower.contains("social") || lower.contains("festa") -> Icons.Default.LocalActivity
        lower.contains("salário") || lower.contains("rendimento") || lower.contains("ordenado") -> Icons.Default.AccountBalanceWallet
        lower.contains("poupança") || lower.contains("invest") -> Icons.Default.Savings
        lower.contains("educa") || lower.contains("curso") -> Icons.Default.School
        else -> Icons.Default.Category
    }
}

@Composable
private fun TypeSegmentChip(
    label: String,
    dotColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) FinflowSurface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) FinflowTextPrimary else FinflowTextSecondary
            )
        }
    }
}
