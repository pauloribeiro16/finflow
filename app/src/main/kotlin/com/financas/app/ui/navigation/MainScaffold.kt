package com.financas.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.di.AppContainer
import com.financas.app.ui.components.FinflowProfileAvatar
import com.financas.app.ui.components.MonthSelector
import com.financas.app.ui.i18n.AppLanguage
import com.financas.app.ui.i18n.AppStrings
import com.financas.app.ui.i18n.EnStrings
import com.financas.app.ui.i18n.LocalAppLanguage
import com.financas.app.ui.i18n.LocalAppStrings
import com.financas.app.ui.i18n.LocalPrivacyMode
import com.financas.app.ui.i18n.PtStrings
import com.financas.app.ui.screens.account.AccountScreen
import com.financas.app.ui.screens.account.AccountViewModel
import com.financas.app.ui.screens.addsheet.AddTransactionBottomSheet
import com.financas.app.ui.screens.addsheet.AddTransactionViewModel
import com.financas.app.ui.screens.analytics.AnalyticsLens
import com.financas.app.ui.screens.analytics.AnalyticsScreen
import com.financas.app.ui.screens.analytics.AnalyticsViewModel
import com.financas.app.ui.screens.budgets.BudgetsGoalsScreen
import com.financas.app.ui.screens.budgets.BudgetsViewModel
import com.financas.app.ui.screens.budgets.WealthTab
import com.financas.app.ui.screens.home.HomeScreen
import com.financas.app.ui.screens.home.HomeViewModel
import com.financas.app.ui.screens.transactions.TransactionsScreen
import com.financas.app.ui.screens.transactions.TransactionsViewModel
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.FinflowAmber
import com.financas.app.ui.theme.FinflowBackground
import com.financas.app.ui.theme.FinflowBorderSubtle
import com.financas.app.ui.theme.FinflowPrimary
import com.financas.app.ui.theme.FinflowSurface
import com.financas.app.ui.theme.FinflowTextMuted
import com.financas.app.ui.theme.FinflowTextPrimary
import com.financas.app.ui.theme.FinflowTextSecondary
import com.financas.app.ui.theme.FinflowTheme
import java.time.YearMonth
import kotlinx.coroutines.launch

enum class NavTab(val icon: ImageVector, val visualOrder: Int) {
    HOME(Icons.Default.GridView, 0),
    TRANSACTIONS(Icons.AutoMirrored.Filled.ReceiptLong, 1),
    ANALYTICS(Icons.Default.PieChart, 2),
    BUDGETS(Icons.Default.AccountBalanceWallet, 3),
    ACCOUNT(Icons.Default.Settings, 4)
}

private val pagerTabs = listOf(NavTab.HOME, NavTab.TRANSACTIONS, NavTab.ANALYTICS, NavTab.BUDGETS)

private fun shiftYearMonth(ym: Int, months: Long): Int {
    val shifted = YearMonth.of(ym / 100, ym % 100).plusMonths(months)
    return shifted.year * 100 + shifted.monthValue
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    val isAppDark = FinflowTheme.colors.isDark
    var pendingIdToClearOnSave by remember { mutableStateOf<Long?>(null) }

    val pagerState = rememberPagerState(pageCount = { pagerTabs.size })
    val coroutineScope = rememberCoroutineScope()
    val selectedTab = pagerTabs[pagerState.currentPage]
    var showAccount by remember { mutableStateOf(false) }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(
            transactionDao = container.transactionDao,
            categoryDao = container.categoryDao,
            budgetDao = container.budgetDao,
            goalDao = container.goalDao,
            monthlyAnalyticsCalculator = container.monthlyAnalyticsCalculator,
            pendingTransactionDao = container.pendingTransactionDao,
            investmentAssetDao = container.investmentAssetDao,
            investmentPortfolioCalculator = container.investmentPortfolioCalculator,
            userProfileStore = container.userProfileStore
        )
    )

    val transactionsViewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.provideFactory(
            transactionDao = container.transactionDao,
            categoryDao = container.categoryDao,
            investmentAssetDao = container.investmentAssetDao
        )
    )

    val addTransactionViewModel: AddTransactionViewModel = viewModel(
        factory = AddTransactionViewModel.provideFactory(
            transactionDao = container.transactionDao,
            categoryDao = container.categoryDao,
            smartPrefillEngine = container.smartPrefillEngine,
            contextTagsStore = container.contextTagsStore,
            investmentAssetDao = container.investmentAssetDao
        )
    )

    val analyticsViewModel: AnalyticsViewModel = viewModel(
        factory = AnalyticsViewModel.provideFactory(
            transactionDao = container.transactionDao,
            categoryDao = container.categoryDao,
            budgetDao = container.budgetDao,
            goalDao = container.goalDao,
            monthlyAnalyticsCalculator = container.monthlyAnalyticsCalculator,
            userProfileStore = container.userProfileStore
        )
    )

    val budgetsViewModel: BudgetsViewModel = viewModel(
        factory = BudgetsViewModel.provideFactory(
            budgetDao = container.budgetDao,
            goalDao = container.goalDao,
            categoryDao = container.categoryDao,
            transactionDao = container.transactionDao,
            investmentAssetDao = container.investmentAssetDao,
            investmentPortfolioCalculator = container.investmentPortfolioCalculator
        )
    )

    val accountViewModel: AccountViewModel = viewModel(
        factory = AccountViewModel.provideFactory(
            categoryDao = container.categoryDao,
            transactionDao = container.transactionDao,
            budgetDao = container.budgetDao,
            goalDao = container.goalDao,
            biometricLockGate = container.biometricLockGate,
            backupSerializer = container.backupSerializer,
            contextTagsStore = container.contextTagsStore,
            pendingTransactionDao = container.pendingTransactionDao,
            investmentAssetDao = container.investmentAssetDao,
            userProfileStore = container.userProfileStore,
            appUpdateManager = container.appUpdateManager,
            settingsBackupManager = container.settingsBackupManager
        )
    )

    LaunchedEffect(Unit) {
        if (!container.userProfileStore.isAutoUpdateCheckEnabled()) return@LaunchedEffect
        container.appUpdateManager.checkForUpdates(
            currentVersion = com.financas.app.BuildConfig.VERSION_NAME,
            notifyIfAvailable = true
        )
    }

    val userProfileStore = container.userProfileStore
    val profilePhotoUri by userProfileStore.profilePhotoFlow.collectAsState()
    val userName by userProfileStore.userNameFlow.collectAsState()
    val appLanguageCode by userProfileStore.appLanguageFlow.collectAsState()
    val isPrivacyMode by userProfileStore.privacyModeFlow.collectAsState()
    val appLanguage = AppLanguage.fromCode(appLanguageCode)
    val appStrings = if (appLanguage == AppLanguage.PT) PtStrings else EnStrings

    val startNewEntry = {
        addTransactionViewModel.startNewEntry()
        editingTransaction = null
        pendingIdToClearOnSave = null
        showAddSheet = true
    }

    val startEdit: (TransactionEntity) -> Unit = { tx ->
        addTransactionViewModel.startEdit(tx)
        editingTransaction = tx
        pendingIdToClearOnSave = null
        showAddSheet = true
    }

    val startPendingEdit: (PendingTransactionEntity) -> Unit = { pending ->
        addTransactionViewModel.startFromPending(pending)
        editingTransaction = null
        pendingIdToClearOnSave = pending.id
        showAddSheet = true
    }

    val propagateMonth: (Int) -> Unit = { ym ->
        homeViewModel.setYearMonth(ym)
        transactionsViewModel.setYearMonth(ym)
        analyticsViewModel.setYearMonth(ym)
        budgetsViewModel.setYearMonth(ym)
    }

    CompositionLocalProvider(
        LocalAppStrings provides appStrings,
        LocalAppLanguage provides appLanguage,
        LocalPrivacyMode provides isPrivacyMode
    ) {
        BackHandler(enabled = showAccount) {
            showAccount = false
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = FinflowBackground,
            bottomBar = {
                FinflowBottomNavBar(
                    selectedTab = if (showAccount) NavTab.ACCOUNT else selectedTab,
                    onSelectTab = { tab ->
                        showAccount = false
                        coroutineScope.launch { pagerState.animateScrollToPage(tab.visualOrder) }
                    },
                    onAddClick = startNewEntry
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                FinflowFixedHeader(
                    selectedTab = if (showAccount) NavTab.ACCOUNT else selectedTab,
                    homeViewModel = homeViewModel,
                    transactionsViewModel = transactionsViewModel,
                    analyticsViewModel = analyticsViewModel,
                    budgetsViewModel = budgetsViewModel,
                    onMonthChanged = propagateMonth,
                    isAppDark = isAppDark,
                    onToggleTheme = {
                        container.userProfileStore.setThemeMode(if (isAppDark) "LIGHT" else "DARK")
                    },
                    isPrivacyMode = isPrivacyMode,
                    onTogglePrivacy = {
                        container.userProfileStore.setPrivacyModeEnabled(!isPrivacyMode)
                    },
                    onOpenAccount = { showAccount = true },
                    photoUri = profilePhotoUri,
                    userName = userName
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (pagerTabs[page]) {
                            NavTab.HOME -> {
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onNavigateToTransactions = { ym ->
                                        transactionsViewModel.setYearMonth(ym)
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(NavTab.TRANSACTIONS.visualOrder)
                                        }
                                    },
                                    onNavigateToInvestments = {
                                        budgetsViewModel.selectTab(WealthTab.INVESTMENTS)
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(NavTab.BUDGETS.visualOrder)
                                        }
                                    },
                                    onEdit = startEdit,
                                    onEditPending = startPendingEdit
                                )
                            }
                            NavTab.TRANSACTIONS -> {
                                TransactionsScreen(
                                    viewModel = transactionsViewModel,
                                    onEdit = startEdit
                                )
                            }
                            NavTab.ANALYTICS -> {
                                AnalyticsScreen(
                                    viewModel = analyticsViewModel,
                                    onGlobalMonthChanged = propagateMonth
                                )
                            }
                            NavTab.BUDGETS -> {
                                BudgetsGoalsScreen(
                                    viewModel = budgetsViewModel
                                )
                            }
                            NavTab.ACCOUNT -> Unit
                        }
                    }

                    if (showAccount) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(FinflowBackground)
                        ) {
                            AccountScreen(
                                viewModel = accountViewModel
                            )
                        }
                    }
                }
            }
        }

        if (showAddSheet) {
            AddTransactionBottomSheet(
                viewModel = addTransactionViewModel,
                onDismissRequest = {
                    showAddSheet = false
                    editingTransaction = null
                    pendingIdToClearOnSave = null
                },
                onSaveSuccess = {
                    showAddSheet = false
                    editingTransaction = null
                    pendingIdToClearOnSave?.let { id ->
                        homeViewModel.discardPending(id)
                        pendingIdToClearOnSave = null
                    }
                }
            )
        }
    }
}

@Composable
private fun FinflowFixedHeader(
    selectedTab: NavTab,
    homeViewModel: HomeViewModel,
    transactionsViewModel: TransactionsViewModel,
    analyticsViewModel: AnalyticsViewModel,
    budgetsViewModel: BudgetsViewModel,
    onMonthChanged: (Int) -> Unit,
    isAppDark: Boolean,
    onToggleTheme: () -> Unit,
    isPrivacyMode: Boolean,
    onTogglePrivacy: () -> Unit,
    onOpenAccount: () -> Unit,
    photoUri: String?,
    userName: String?
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val transactionsUiState by transactionsViewModel.uiState.collectAsState()
    val analyticsUiState by analyticsViewModel.uiState.collectAsState()
    val budgetsUiState by budgetsViewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Finflow",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AccentBlue
            )
            Text(
                text = when (selectedTab) {
                    NavTab.HOME -> strings.navHome
                    NavTab.TRANSACTIONS -> strings.navTransactions
                    NavTab.ANALYTICS -> strings.navAnalytics
                    NavTab.BUDGETS -> when (budgetsUiState.selectedTab) {
                        WealthTab.BUDGETS -> strings.tabBudgets
                        WealthTab.GOALS -> strings.tabGoals
                        WealthTab.INVESTMENTS -> strings.tabInvestments
                    }
                    NavTab.ACCOUNT -> strings.navAccount
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (selectedTab) {
                NavTab.HOME -> {
                    MonthSelector(
                        monthTitle = homeUiState.monthTitle,
                        selectedYearMonth = homeUiState.selectedYearMonth,
                        onPreviousMonth = { onMonthChanged(shiftYearMonth(homeUiState.selectedYearMonth, -1L)) },
                        onNextMonth = { onMonthChanged(shiftYearMonth(homeUiState.selectedYearMonth, 1L)) },
                        onSelectYearMonth = { onMonthChanged(it) },
                        compact = true
                    )
                }
                NavTab.TRANSACTIONS -> {
                    MonthSelector(
                        monthTitle = transactionsUiState.monthTitle,
                        selectedYearMonth = transactionsUiState.currentYearMonth,
                        onPreviousMonth = { onMonthChanged(shiftYearMonth(transactionsUiState.currentYearMonth, -1L)) },
                        onNextMonth = { onMonthChanged(shiftYearMonth(transactionsUiState.currentYearMonth, 1L)) },
                        onSelectYearMonth = { onMonthChanged(it) },
                        compact = true
                    )
                }
                NavTab.ANALYTICS -> {
                    if (analyticsUiState.activeLens == AnalyticsLens.ANUAL) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { analyticsViewModel.selectAnnualPreviousYear() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Ano Anterior",
                                    tint = FinflowTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = "Ano ${analyticsUiState.annualAnalysis.selectedYear}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinflowTextPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            IconButton(
                                onClick = { analyticsViewModel.selectAnnualNextYear() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Ano Seguinte",
                                    tint = FinflowTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        MonthSelector(
                            monthTitle = analyticsUiState.monthTitle,
                            selectedYearMonth = analyticsUiState.selectedYearMonth,
                            onPreviousMonth = { onMonthChanged(shiftYearMonth(analyticsUiState.selectedYearMonth, -1L)) },
                            onNextMonth = { onMonthChanged(shiftYearMonth(analyticsUiState.selectedYearMonth, 1L)) },
                            onSelectYearMonth = { onMonthChanged(it) },
                            compact = true
                        )
                    }
                }
                NavTab.BUDGETS -> {
                    if (budgetsUiState.selectedTab != WealthTab.INVESTMENTS) {
                        MonthSelector(
                            monthTitle = budgetsUiState.monthTitle,
                            selectedYearMonth = budgetsUiState.selectedYearMonth,
                            onPreviousMonth = { onMonthChanged(shiftYearMonth(budgetsUiState.selectedYearMonth, -1L)) },
                            onNextMonth = { onMonthChanged(shiftYearMonth(budgetsUiState.selectedYearMonth, 1L)) },
                            onSelectYearMonth = { onMonthChanged(it) },
                            compact = true
                        )
                    }
                }
                NavTab.ACCOUNT -> Unit
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isPrivacyMode) AccentBlue.copy(alpha = 0.2f) else FinflowSurface)
                    .border(
                        1.dp,
                        if (isPrivacyMode) AccentBlue else FinflowBorderSubtle,
                        CircleShape
                    )
                    .clickable(onClick = onTogglePrivacy),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isPrivacyMode) "Modo Privacidade Ativo (Mostrar Valores)" else "Ativar Modo Privacidade (Ocultar Valores)",
                    tint = if (isPrivacyMode) AccentBlue else FinflowTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(FinflowSurface)
                    .border(1.dp, FinflowBorderSubtle, CircleShape)
                    .clickable(onClick = onToggleTheme),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAppDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = if (isAppDark) "Alternar para Modo Diurno" else "Alternar para Modo Noturno",
                    tint = if (isAppDark) FinflowAmber else FinflowTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            FinflowProfileAvatar(
                photoUri = photoUri,
                userName = userName ?: "",
                onClick = onOpenAccount,
                size = 36.dp
            )
        }
    }
}

@Composable
fun FinflowBottomNavBar(
    selectedTab: NavTab,
    onSelectTab: (NavTab) -> Unit,
    onAddClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FinflowSurface)
            .border(1.dp, FinflowBorderSubtle)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Home
            FinflowNavItem(
                label = strings.navHome,
                icon = Icons.Default.GridView,
                isSelected = selectedTab == NavTab.HOME,
                onClick = { onSelectTab(NavTab.HOME) }
            )

            // Tab 2: Transactions
            FinflowNavItem(
                label = strings.navTransactions,
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                isSelected = selectedTab == NavTab.TRANSACTIONS,
                onClick = { onSelectTab(NavTab.TRANSACTIONS) }
            )

            // Tab 3: Quick Add
            FinflowCenterActionItem(
                onClick = onAddClick
            )

            // Tab 4: Analytics
            FinflowNavItem(
                label = strings.navAnalytics,
                icon = Icons.Default.PieChart,
                isSelected = selectedTab == NavTab.ANALYTICS,
                onClick = { onSelectTab(NavTab.ANALYTICS) }
            )

            // Tab 5: Wealth
            FinflowNavItem(
                label = strings.navWealth,
                icon = Icons.Default.AccountBalanceWallet,
                isSelected = selectedTab == NavTab.BUDGETS,
                onClick = { onSelectTab(NavTab.BUDGETS) }
            )
        }
    }
}

@Composable
private fun FinflowNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "navIconScale"
    )

    val pillAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.12f else 0.0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "navPillAlpha"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) FinflowPrimary else FinflowTextMuted,
        animationSpec = tween(200),
        label = "navContentColor"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FinflowPrimary.copy(alpha = pillAlpha))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun FinflowCenterActionItem(
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "centerActionScale"
    )

    Column(
        modifier = Modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                }
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(FinflowPrimary, AccentBlue)
                    )
                )
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.35f),
                    shape = CircleShape
                )
                .clickable {
                    isPressed = true
                    onClick()
                    isPressed = false
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Nova Transação",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Nova",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = FinflowPrimary
        )
    }
}
