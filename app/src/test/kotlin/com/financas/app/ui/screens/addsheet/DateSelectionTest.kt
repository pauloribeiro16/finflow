package com.financas.app.ui.screens.addsheet

import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.usecase.SmartPrefillEngine
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeContextTagsStore
import com.financas.app.ui.fakes.FakeTransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class DateSelectionTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoryDao = FakeCategoryDao(DefaultSeed.getDefaultCategories())
        transactionDao = FakeTransactionDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = AddTransactionViewModel(
        transactionDao = transactionDao,
        categoryDao = categoryDao,
        smartPrefillEngine = SmartPrefillEngine(transactionDao),
        contextTagsStore = FakeContextTagsStore()
    )

    private fun yearMonthOf(epochDay: Long): Int {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.year * 100 + date.monthValue
    }

    @Test
    fun test1_selectedDate_SetsEpochDayAndDefinesTheSavedYearMonth() = runTest {
        val viewModel = buildViewModel()
        val chosen = LocalDate.of(2026, 9, 15)

        viewModel.onDateSelected(chosen.toEpochDay())

        assertEquals(chosen.toEpochDay(), viewModel.uiState.value.epochDay)

        viewModel.onDigit('1')
        viewModel.onDigit('2')
        viewModel.saveTransaction()

        val inserted = transactionDao.getAllSync().single()
        assertEquals(chosen.toEpochDay(), inserted.epochDay)
        assertEquals(202609, inserted.yearMonth)
        assertEquals(yearMonthOf(inserted.epochDay), inserted.yearMonth)
    }

    @Test
    fun test2_changingDateToThePreviousMonth_ProducesThePreviousYearMonth() = runTest {
        val viewModel = buildViewModel()

        viewModel.onDateSelected(LocalDate.of(2026, 10, 20).toEpochDay())
        assertEquals(202610, yearMonthOf(viewModel.uiState.value.epochDay))

        viewModel.onDateSelected(LocalDate.of(2026, 9, 20).toEpochDay())

        assertEquals(LocalDate.of(2026, 9, 20).toEpochDay(), viewModel.uiState.value.epochDay)
        assertEquals(202609, yearMonthOf(viewModel.uiState.value.epochDay))

        viewModel.onDigit('5')
        viewModel.saveTransaction()

        assertEquals(202609, transactionDao.getAllSync().single().yearMonth)
    }

    @Test
    fun test3_changingDateToThePreviousYear_ProducesThePreviousYearMonth() = runTest {
        val viewModel = buildViewModel()

        viewModel.onDateSelected(LocalDate.of(2026, 1, 5).toEpochDay())
        viewModel.onDateSelected(LocalDate.of(2025, 12, 31).toEpochDay())

        assertEquals(202512, yearMonthOf(viewModel.uiState.value.epochDay))

        viewModel.onDigit('5')
        viewModel.saveTransaction()

        assertEquals(202512, transactionDao.getAllSync().single().yearMonth)
    }

    @Test
    fun test4_firstDayOfTheMonth_IsAcceptedAndMapsToTheSameMonth() = runTest {
        val viewModel = buildViewModel()
        val firstDay = LocalDate.of(2026, 10, 1)

        viewModel.onDateSelected(firstDay.toEpochDay())

        assertEquals(firstDay.toEpochDay(), viewModel.uiState.value.epochDay)
        assertEquals(202610, yearMonthOf(viewModel.uiState.value.epochDay))

        viewModel.onDigit('7')
        viewModel.saveTransaction()

        val inserted = transactionDao.getAllSync().single()
        assertEquals(firstDay.toEpochDay(), inserted.epochDay)
        assertEquals(202610, inserted.yearMonth)
    }

    @Test
    fun test5_lastDayOfTheMonth_IsAcceptedAndMapsToTheSameMonth() = runTest {
        val viewModel = buildViewModel()
        val lastDay = LocalDate.of(2026, 10, 31)

        viewModel.onDateSelected(lastDay.toEpochDay())

        assertEquals(lastDay.toEpochDay(), viewModel.uiState.value.epochDay)
        assertEquals(202610, yearMonthOf(viewModel.uiState.value.epochDay))

        viewModel.onDigit('7')
        viewModel.saveTransaction()

        val inserted = transactionDao.getAllSync().single()
        assertEquals(lastDay.toEpochDay(), inserted.epochDay)
        assertEquals(202610, inserted.yearMonth)
    }

    @Test
    fun test6_leapFebruaryBoundaries_KeepTheSameYearMonth() = runTest {
        val viewModel = buildViewModel()

        viewModel.onDateSelected(LocalDate.of(2024, 2, 29).toEpochDay())
        assertEquals(202402, yearMonthOf(viewModel.uiState.value.epochDay))

        viewModel.onDateSelected(LocalDate.of(2026, 2, 28).toEpochDay())
        assertEquals(202602, yearMonthOf(viewModel.uiState.value.epochDay))

        viewModel.onDigit('3')
        viewModel.saveTransaction()

        assertEquals(202602, transactionDao.getAllSync().single().yearMonth)
    }

    @Test
    fun test7_savedTransactionAlwaysKeepsYearMonthInSyncWithTheSelectedDate() = runTest {
        val viewModel = buildViewModel()
        val dates = listOf(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 31),
            LocalDate.of(2026, 11, 30),
            LocalDate.of(2026, 12, 31),
            LocalDate.of(2027, 1, 1)
        )

        dates.forEachIndexed { index, date ->
            viewModel.onDateSelected(date.toEpochDay())
            viewModel.setRawAmount("${index + 1},00")
            viewModel.saveTransaction()
        }

        val inserted = transactionDao.getAllSync().sortedBy { it.id }
        assertEquals(dates.size, inserted.size)
        inserted.forEachIndexed { index, tx ->
            assertEquals(dates[index].toEpochDay(), tx.epochDay)
            assertEquals(yearMonthOf(dates[index].toEpochDay()), tx.yearMonth)
        }
    }

    @Test
    fun test8_yearMonthOfSelectedDate_MatchesTheYearMonthOfEveryMonthOfACalendarYear() = runTest {
        val viewModel = buildViewModel()

        for (month in 1..12) {
            val date = YearMonth.of(2026, month).atDay(1)
            viewModel.onDateSelected(date.toEpochDay())
            assertEquals(202600 + month, yearMonthOf(viewModel.uiState.value.epochDay))
        }
    }
}
