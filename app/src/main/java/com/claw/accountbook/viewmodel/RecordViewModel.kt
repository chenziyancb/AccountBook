package com.claw.accountbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.claw.accountbook.data.local.dao.CategorySum
import com.claw.accountbook.data.local.dao.DailyTotal
import com.claw.accountbook.data.local.dao.MonthlyTotal
import com.claw.accountbook.data.local.entity.CategoryEntity
import com.claw.accountbook.data.local.entity.RecordEntity
import com.claw.accountbook.data.repository.CategoryRepository
import com.claw.accountbook.data.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * 记账记录 ViewModel
 */
@HiltViewModel
class RecordViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // 当前账本ID（-1=全部）
    private val _currentAccountBookId = MutableStateFlow(-1L)
    val currentAccountBookId: StateFlow<Long> = _currentAccountBookId.asStateFlow()

    // 所有记录（根据账本过滤）
    val allRecords: StateFlow<List<RecordEntity>> = _currentAccountBookId
        .flatMapLatest { bookId ->
            recordRepository.getRecordsByAccountBook(bookId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 支出分类
    val expenseCategories: StateFlow<List<CategoryEntity>> = categoryRepository.getCategoriesByType(0)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 收入分类
    val incomeCategories: StateFlow<List<CategoryEntity>> = categoryRepository.getCategoriesByType(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当前选择的日期
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    // 当前记录类型（支出/收入）
    private val _recordType = MutableStateFlow(0) // 0-支出，1-收入
    val recordType: StateFlow<Int> = _recordType.asStateFlow()

    // UI状态
    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    // 月度统计
    private val _monthlyStats = MutableStateFlow(MonthlyStats())
    val monthlyStats: StateFlow<MonthlyStats> = _monthlyStats.asStateFlow()

    // 周统计
    private val _weeklyStats = MutableStateFlow(WeeklyStats())
    val weeklyStats: StateFlow<WeeklyStats> = _weeklyStats.asStateFlow()

    // 年统计
    private val _yearlyStats = MutableStateFlow(YearlyStats())
    val yearlyStats: StateFlow<YearlyStats> = _yearlyStats.asStateFlow()

    init {
        loadMonthlyStats()
        loadWeeklyStats()
        loadYearlyStats()
    }

    fun setRecordType(type: Int) {
        _recordType.value = type
    }

    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
        loadMonthlyStats()
    }

    fun setCurrentAccountBook(bookId: Long) {
        _currentAccountBookId.value = bookId
        loadMonthlyStats()
        loadWeeklyStats()
        loadYearlyStats()
    }

    fun addRecord(
        amount: Double,
        categoryId: Long,
        categoryName: String,
        note: String?,
        accountBookId: Long = _currentAccountBookId.value.takeIf { it != -1L } ?: 1L,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            try {
                val record = RecordEntity(
                    amount = amount,
                    type = _recordType.value,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    note = note,
                    date = date,
                    accountBookId = accountBookId
                )
                recordRepository.insert(record)
                loadMonthlyStats()
                loadWeeklyStats()
                loadYearlyStats()
                _uiState.update { it.copy(message = "记录添加成功") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateRecord(record: RecordEntity) {
        viewModelScope.launch {
            try {
                recordRepository.update(record.copy(updatedAt = System.currentTimeMillis()))
                loadMonthlyStats()
                loadWeeklyStats()
                loadYearlyStats()
                _uiState.update { it.copy(message = "记录更新成功") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteRecord(record: RecordEntity) {
        viewModelScope.launch {
            try {
                recordRepository.delete(record)
                loadMonthlyStats()
                loadWeeklyStats()
                loadYearlyStats()
                _uiState.update { it.copy(message = "记录删除成功") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun loadMonthlyStats() {
        viewModelScope.launch {
            val bookId = _currentAccountBookId.value
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = _selectedDate.value
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.timeInMillis

            val income: Double
            val expense: Double
            val expenseCategorySum: List<CategorySum>
            val incomeCategorySum: List<CategorySum>

            if (bookId == -1L) {
                // 全部账本
                income = recordRepository.getTotalByTypeAndDateRange(1, startOfMonth, endOfMonth)
                expense = recordRepository.getTotalByTypeAndDateRange(0, startOfMonth, endOfMonth)
                expenseCategorySum = recordRepository.getCategorySummary(0, startOfMonth, endOfMonth)
                incomeCategorySum = recordRepository.getCategorySummary(1, startOfMonth, endOfMonth)
            } else {
                // 指定账本
                income = recordRepository.getTotalByTypeAndDateRangeForBook(1, startOfMonth, endOfMonth, bookId)
                expense = recordRepository.getTotalByTypeAndDateRangeForBook(0, startOfMonth, endOfMonth, bookId)
                expenseCategorySum = recordRepository.getCategorySummaryForBook(0, startOfMonth, endOfMonth, bookId)
                incomeCategorySum = recordRepository.getCategorySummaryForBook(1, startOfMonth, endOfMonth, bookId)
            }

            _monthlyStats.value = MonthlyStats(
                income = income,
                expense = expense,
                balance = income - expense,
                expenseByCategory = expenseCategorySum,
                incomeByCategory = incomeCategorySum
            )
        }
    }

    /**
     * 加载本周统计：周一到周日
     */
    fun loadWeeklyStats() {
        viewModelScope.launch {
            val bookId = _currentAccountBookId.value
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            // 将周一设为一周的起始
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val startOfWeek = cal.timeInMillis

            cal.add(Calendar.DAY_OF_WEEK, 6)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfWeek = cal.timeInMillis

            val dailyTotals: List<DailyTotal>
            val totalIncome: Double
            val totalExpense: Double

            if (bookId == -1L) {
                dailyTotals = recordRepository.getDailyTotals(startOfWeek, endOfWeek)
                totalIncome = recordRepository.getTotalByTypeAndDateRange(1, startOfWeek, endOfWeek)
                totalExpense = recordRepository.getTotalByTypeAndDateRange(0, startOfWeek, endOfWeek)
            } else {
                dailyTotals = recordRepository.getDailyTotalsForBook(startOfWeek, endOfWeek, bookId)
                totalIncome = recordRepository.getTotalByTypeAndDateRangeForBook(1, startOfWeek, endOfWeek, bookId)
                totalExpense = recordRepository.getTotalByTypeAndDateRangeForBook(0, startOfWeek, endOfWeek, bookId)
            }

            _weeklyStats.value = WeeklyStats(
                dailyTotals = dailyTotals,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                startOfWeek = startOfWeek,
                endOfWeek = endOfWeek
            )
        }
    }

    /**
     * 加载本年统计：1月到12月每月汇总
     */
    fun loadYearlyStats() {
        viewModelScope.launch {
            val bookId = _currentAccountBookId.value
            val cal = Calendar.getInstance()
            cal.set(Calendar.MONTH, Calendar.JANUARY)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfYear = cal.timeInMillis

            cal.set(Calendar.MONTH, Calendar.DECEMBER)
            cal.set(Calendar.DAY_OF_MONTH, 31)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfYear = cal.timeInMillis

            val monthlyTotals: List<MonthlyTotal>
            val totalIncome: Double
            val totalExpense: Double

            if (bookId == -1L) {
                monthlyTotals = recordRepository.getMonthlyTotals(startOfYear, endOfYear)
                totalIncome = recordRepository.getTotalByTypeAndDateRange(1, startOfYear, endOfYear)
                totalExpense = recordRepository.getTotalByTypeAndDateRange(0, startOfYear, endOfYear)
            } else {
                monthlyTotals = recordRepository.getMonthlyTotalsForBook(startOfYear, endOfYear, bookId)
                totalIncome = recordRepository.getTotalByTypeAndDateRangeForBook(1, startOfYear, endOfYear, bookId)
                totalExpense = recordRepository.getTotalByTypeAndDateRangeForBook(0, startOfYear, endOfYear, bookId)
            }

            _yearlyStats.value = YearlyStats(
                monthlyTotals = monthlyTotals,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                year = Calendar.getInstance().get(Calendar.YEAR)
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}

data class RecordUiState(
    val message: String? = null,
    val error: String? = null
)

data class MonthlyStats(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0,
    val expenseByCategory: List<CategorySum> = emptyList(),
    val incomeByCategory: List<CategorySum> = emptyList()
)

data class WeeklyStats(
    val dailyTotals: List<DailyTotal> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val startOfWeek: Long = 0L,
    val endOfWeek: Long = 0L
)

data class YearlyStats(
    val monthlyTotals: List<MonthlyTotal> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR)
)
