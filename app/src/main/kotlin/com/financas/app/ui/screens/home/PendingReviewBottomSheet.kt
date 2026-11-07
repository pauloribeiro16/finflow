package com.financas.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.domain.usecase.InvestmentTransferRules
import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.model.MoneyCents
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.AccentCoral
import com.financas.app.ui.theme.AccentEmerald
import com.financas.app.ui.theme.DarkBackground
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.DarkSurfaceHighlight
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.TextMuted
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PendingReviewBottomSheet(
    sheetState: SheetState,
    pendingList: List<PendingTransactionEntity>,
    categories: Map<String, CategoryEntity>,
    onDismiss: () -> Unit,
    investmentAssets: List<InvestmentAssetEntity> = emptyList(),
    onConfirmInvestment: (PendingTransactionEntity, String, String) -> Unit = { _, _, _ -> },
    onConfirm: (PendingTransactionEntity, String, String) -> Unit,
    onEditFull: (PendingTransactionEntity) -> Unit,
    onDiscard: (Long) -> Unit,
    onConfirmAll: () -> Unit,
    onUpdatePending: (PendingTransactionEntity) -> Unit = {}
) {
    var itemPickingCategory by remember { mutableStateOf<PendingTransactionEntity?>(null) }
    var itemPickingContext by remember { mutableStateOf<PendingTransactionEntity?>(null) }
    var selectedCategories by remember { mutableStateOf(mapOf<Long, String>()) }
    var selectedContexts by remember { mutableStateOf(mapOf<Long, String>()) }
    var selectedAssets by remember { mutableStateOf(mapOf<Long, String>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Fila de Pendentes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${pendingList.size} movimento(s) por validar",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (pendingList.size > 1) {
                    Button(
                        onClick = onConfirmAll,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirmar Todos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pendingList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Não tens movimentos pendentes para rever.",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingList, key = { it.id }) { item ->
                        val currentCategoryId = selectedCategories[item.id] ?: item.suggestedCategoryId
                        val currentContextTag = selectedContexts[item.id] ?: item.suggestedContextTag

                        val currentCategory = categories[currentCategoryId]
                        val parentCategory = currentCategory?.parentId?.let { categories[it] }
                        val categoryName = when {
                            parentCategory != null -> "${parentCategory.name} › ${currentCategory.name}"
                            currentCategory != null -> currentCategory.name
                            else -> currentCategoryId
                        }
                        val contextLabel = DefaultSeed.CONTEXTS.firstOrNull { it.tag == currentContextTag }?.label
                            ?: currentContextTag

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Top row: Source badge + Amount
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AccentBlue.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = item.sourceApp,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentBlue
                                        )
                                    }

                                    val isIncomePending = item.suggestedType.equals("INCOME", ignoreCase = true)
                                    Text(
                                        text = "${if (isIncomePending) "+" else "-"} ${MoneyCents.formatCents(item.amountCents)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIncomePending) AccentEmerald else AccentCoral
                                    )
                                }

                                // Merchant Name
                                Text(
                                    text = item.rawMerchant,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )

                                // Interactive Category & Context Selectors
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Category Pill (clickable to change)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurfaceHighlight)
                                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                            .clickable { itemPickingCategory = item }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Category,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Categoria",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                                Text(
                                                    text = categoryName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextPrimary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    // Context Pill (clickable to change)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurfaceHighlight)
                                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                            .clickable { itemPickingContext = item }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Label,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Contexto",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                                Text(
                                                    text = contextLabel,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextPrimary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                val isInvestment = InvestmentTransferRules.isInvestmentTransfer(
                                    item.rawMerchant, currentCategoryId, currentCategory?.name
                                )
                                val monthlyAssetId = if (isInvestment) {
                                    InvestmentTransferRules.matchMonthlyAsset(
                                        item.amountCents,
                                        InvestmentTransferRules.detectPlatform(item.rawMerchant),
                                        investmentAssets
                                    )
                                } else {
                                    null
                                }
                                val chosenAssetId = if (isInvestment) selectedAssets[item.id] ?: monthlyAssetId else null

                                if (isInvestment) {
                                    Text(
                                        text = if (monthlyAssetId != null && selectedAssets[item.id] == null) {
                                            "Investimento — sugerido pelo reforço mensal definido (podes mudar)"
                                        } else {
                                            "Investimento — escolhe para onde foi (ou 'Decidir depois' para alocar na Carteira)"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                    val options = buildList {
                                        add(InvestmentTransferRules.XTB_HOLDING_ASSET_ID to InvestmentTransferRules.XTB_HOLDING_CHOICE_LABEL)
                                        investmentAssets
                                            .filter { !InvestmentTransferRules.isHoldingAsset(it) }
                                            .forEach { add(it.id to it.name) }
                                    }
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        options.forEach { (assetId, assetName) ->
                                            val isSelected = chosenAssetId == assetId
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) AccentEmerald else Color.Transparent)
                                                    .border(1.dp, if (isSelected) AccentEmerald else DarkBorder, RoundedCornerShape(10.dp))
                                                    .clickable { selectedAssets = selectedAssets + (item.id to assetId) }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = assetName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isSelected) Color.White else TextPrimary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Confirm button (1-touch)
                                    Button(
                                        onClick = {
                                            if (isInvestment && chosenAssetId != null) {
                                                onConfirmInvestment(item, chosenAssetId, currentCategoryId)
                                            } else {
                                                onConfirm(item, currentCategoryId, currentContextTag)
                                            }
                                        },
                                        enabled = !isInvestment || chosenAssetId != null,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (isInvestment && chosenAssetId == null) "Escolhe o ativo" else "Confirmar",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Edit Full button
                                    Button(
                                        onClick = {
                                            onEditFull(
                                                item.copy(
                                                    suggestedCategoryId = currentCategoryId,
                                                    suggestedContextTag = currentContextTag
                                                )
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Editar", fontSize = 12.sp, color = TextPrimary)
                                    }

                                    // Discard button
                                    IconButton(
                                        onClick = { onDiscard(item.id) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(AccentCoral.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Descartar",
                                            tint = AccentCoral,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Category Picker Dialog
    itemPickingCategory?.let { activeItem ->
        val currentCatId = selectedCategories[activeItem.id] ?: activeItem.suggestedCategoryId

        CategoryPickerDialog(
            currentCategoryId = currentCatId,
            categories = categories,
            onSelectCategory = { chosenId ->
                selectedCategories = selectedCategories + (activeItem.id to chosenId)
                onUpdatePending(activeItem.copy(suggestedCategoryId = chosenId))
                itemPickingCategory = null
            },
            onDismiss = { itemPickingCategory = null }
        )
    }

    // Context Picker Dialog
    itemPickingContext?.let { activeItem ->
        val currentCtxTag = selectedContexts[activeItem.id] ?: activeItem.suggestedContextTag

        AlertDialog(
            onDismissRequest = { itemPickingContext = null },
            title = {
                Text("Escolher Contexto", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaultSeed.CONTEXTS.forEach { ctx ->
                        val isSelected = ctx.tag == currentCtxTag

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentBlue.copy(alpha = 0.2f) else DarkSurfaceHighlight)
                                .border(1.dp, if (isSelected) AccentBlue else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedContexts = selectedContexts + (activeItem.id to ctx.tag)
                                    onUpdatePending(activeItem.copy(suggestedContextTag = ctx.tag))
                                    itemPickingContext = null
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ctx.label,
                                    color = if (isSelected) AccentBlue else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { itemPickingContext = null }) {
                    Text("Cancelar", color = AccentBlue)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPickerDialog(
    currentCategoryId: String,
    categories: Map<String, CategoryEntity>,
    onSelectCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedParentFilter by remember { mutableStateOf<String?>(null) }

    val targetType = remember(currentCategoryId, categories) {
        categories[currentCategoryId]?.type ?: "EXPENSE"
    }

    val relevantCategories = remember(categories, targetType) {
        categories.values.filter { it.type == targetType }
    }

    val parentCategories = remember(relevantCategories) {
        relevantCategories.filter { it.parentId == null }.sortedBy { it.sortOrder }
    }

    val subsByParent = remember(relevantCategories) {
        relevantCategories.filter { it.parentId != null }.groupBy { it.parentId!! }
    }

    val displayedParents = remember(parentCategories, subsByParent, selectedParentFilter, searchQuery) {
        val base = if (selectedParentFilter != null && searchQuery.isBlank()) {
            parentCategories.filter { it.id == selectedParentFilter }
        } else {
            parentCategories
        }

        if (searchQuery.isBlank()) {
            base
        } else {
            val query = searchQuery.trim().lowercase(Locale.getDefault())
            parentCategories.filter { parent ->
                parent.name.lowercase(Locale.getDefault()).contains(query) ||
                    (subsByParent[parent.id]?.any { it.name.lowercase(Locale.getDefault()).contains(query) } == true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Escolher Categoria", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Pesquisar categoria...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceHighlight,
                        unfocusedContainerColor = DarkSurfaceHighlight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentBlue
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Filter Tabs (Horizontal Scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAllSelected = selectedParentFilter == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAllSelected) AccentBlue else DarkSurfaceHighlight)
                            .border(1.dp, if (isAllSelected) AccentBlue else DarkBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedParentFilter = null }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Todas",
                            fontSize = 12.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllSelected) Color.White else TextSecondary
                        )
                    }

                    parentCategories.forEach { parent ->
                        val isSelected = selectedParentFilter == parent.id
                        val parentColor = runCatching { Color(parent.colorHex) }.getOrDefault(AccentBlue)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentBlue.copy(alpha = 0.2f) else DarkSurfaceHighlight)
                                .border(1.dp, if (isSelected) AccentBlue else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedParentFilter = if (selectedParentFilter == parent.id) null else parent.id
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(parentColor)
                                )
                                Text(
                                    text = parent.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) AccentBlue else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Hierarchical Category List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (displayedParents.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhuma categoria encontrada",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(displayedParents, key = { it.id }) { parent ->
                            val isParentSelected = currentCategoryId == parent.id
                            val parentColor = runCatching { Color(parent.colorHex) }.getOrDefault(AccentBlue)
                            val allSubs = subsByParent[parent.id] ?: emptyList()
                            val filteredSubs = if (searchQuery.isBlank()) {
                                allSubs
                            } else {
                                val query = searchQuery.trim().lowercase(Locale.getDefault())
                                if (parent.name.lowercase(Locale.getDefault()).contains(query)) {
                                    allSubs
                                } else {
                                    allSubs.filter { it.name.lowercase(Locale.getDefault()).contains(query) }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, if (isParentSelected) AccentBlue else DarkBorder, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Parent Row (Clickable to select parent)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onSelectCategory(parent.id) }
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
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
                                                .background(parentColor)
                                        )
                                        Text(
                                            text = parent.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isParentSelected) AccentBlue else TextPrimary
                                        )
                                    }

                                    if (isParentSelected) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Geral",
                                                fontSize = 11.sp,
                                                color = AccentBlue,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = AccentBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Usar geral",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                // Subcategories Chips in FlowRow
                                if (filteredSubs.isNotEmpty()) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        filteredSubs.forEach { sub ->
                                            val isSubSelected = currentCategoryId == sub.id
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSubSelected) AccentBlue.copy(alpha = 0.2f)
                                                        else DarkSurfaceHighlight
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSubSelected) AccentBlue else DarkBorder,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { onSelectCategory(sub.id) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (isSubSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = AccentBlue,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = sub.name,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSubSelected) AccentBlue else TextPrimary
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = AccentBlue)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
