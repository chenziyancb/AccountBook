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

    // 当前选择的日期（首页月份导航用）
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    // 当前记录类型（支出/收入）
    private val _recordType = MutableStateFlow(0) // 0-支出，1-收入
    val recordType: StateFlow<Int> = _recordType.asStateFlow()

    // UI状态
    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    // 月度统计（首页用）
    private val _monthlyStats = MutableStateFlow(MonthlyStats())
    val monthlyStats: StateFlow<MonthlyStats> = _monthlyStats.asStateFlow()

    // 周统计（统计页 - 本周）
    private val _weeklyStats = MutableStateFlow(WeeklyStats())
    val weeklyStats: StateFlow<WeeklyStats> = _weeklyStats.asStateFlow()

    // 年统计
    private val _yearlyStats = MutableStateFlow(YearlyStats())
    val yearlyStats: StateFlow<YearlyStats> = _yearlyStats.asStateFlow()

    // ────── 统计页专属状态 ──────

    // 统计页：选中的年
    private val _statsYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val statsYear: StateFlow<Int> = _statsYear.asStateFlow()

    // 统计页：选中的月（1-12，-1=未选）
    private val _statsMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val statsMonth: StateFlow<Int> = _statsMonth.asStateFlow()

    // 统计页：选中的周（1-5，-1=未选）
    private val _statsWeek = MutableStateFlow(-1)
    val statsWeek: StateFlow<Int> = _statsWeek.asStateFlow()

    // 统计页：年统计数据（按选中年）
    private val _statsYearData = MutableStateFlow(YearlyStats())
    val statsYearData: StateFlow<YearlyStats> = _statsYearData.asStateFlow()

    // 统计页：月统计数据（按选中年月）
    private val _statsMonthData = MutableStateFlow(MonthlyStats())
    val statsMonthData: StateFlow<MonthlyStats> = _statsMonthData.asStateFlow()

    // 统计页：周统计数据（按选中年月周）
    private val _statsWeekData = MutableStateFlow(WeeklyStats())
    val statsWeekData: StateFlow<WeeklyStats> = _statsWeekData.asStateFlow()

    // 统计页：当前月所有周的区间列表（Pair<startMs, endMs>）
    private val _statsWeekRanges = MutableStateFlow<List<Pair<Long, Long>>>(emptyList())
    val statsWeekRanges: StateFlow<List<Pair<Long, Long>>> = _statsWeekRanges.asStateFlow()

    // 可查询到的历史年份列表
    private val _availableYears = MutableStateFlow<List<Int>>(emptyList())
    val availableYears: StateFlow<List<Int>> = _availableYears.asStateFlow()

    // ────── 首页汇总专属状态 ──────

    // 今日收支
    private val _todayStats = MutableStateFlow(SimpleSummary())
    val todayStats: StateFlow<SimpleSummary> = _todayStats.asStateFlow()

    // 本周收支
    private val _thisWeekStats = MutableStateFlow(SimpleSummary())
    val thisWeekStats: StateFlow<SimpleSummary> = _thisWeekStats.asStateFlow()

    // 本月收支（首页用，与 monthlyStats 保持一致，额外暴露一个 SimpleSummary 供汇总行使用）
    private val _thisMonthStats = MutableStateFlow(SimpleSummary())
    val thisMonthStats: StateFlow<SimpleSummary> = _thisMonthStats.asStateFlow()

    // 本月每日支出（折线图数据，day(1-31) -> 支出金额）
    private val _monthDailyExpense = MutableStateFlow<Map<Int, Double>>(emptyMap())
    val monthDailyExpense: StateFlow<Map<Int, Double>> = _monthDailyExpense.asStateFlow()

    init {
        loadMonthlyStats()
        loadWeeklyStats()
        loadYearlyStats()
        loadAvailableYears()
        loadStatsYearData()
        loadStatsMonthData()
        loadHomeSummary()
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
        loadAvailableYears()
        loadStatsYearData()
        loadStatsMonthData()
        loadHomeSummary()
        if (_statsWeek.value != -1) loadStatsWeekData()
    }

    // ── 统计页年月周选择 ──

    fun selectStatsYear(year: Int) {
        _statsYear.value = year
        _statsMonth.value = -1
        _statsWeek.value = -1
        _statsWeekRanges.value = emptyList()
        loadStatsYearData()
    }

    fun selectStatsMonth(month: Int) {
        _statsMonth.value = month
        _statsWeek.value = -1
        loadStatsMonthData()
        computeWeekRangesForMonth()
    }

    fun selectStatsWeek(weekIndex: Int) {
        _statsWeek.value = weekIndex
        loadStatsWeekData()
    }

    private fun computeWeekRangesForMonth() {
        val year = _statsYear.value
        val month = _statsMonth.value
        if (month == -1) return

        val weeks = mutableListOf<Pair<Long, Long>>()
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        var day = 1
        while (day <= lastDay) {
            val startCal = Calendar.getInstance()
            startCal.set(year, month - 1, day, 0, 0, 0)
            startCal.set(Calendar.MILLISECOND, 0)
            val weekEnd = minOf(day + 6, lastDay)
            val endCal = Calendar.getInstance()
            endCal.set(year, month - 1, weekEnd, 23, 59, 59)
            endCal.set(Calendar.MILLISECOND, 999)
            weeks.add(startCal.timeInMillis to endCal.timeInMillis)
            day += 7
        }
        _statsWeekRanges.value = weeks
    }

    private fun loadAvailableYears() {
        viewModelScope.launch {
            val records = recordRepository.getAllRecordsOnce()
            val years = records.map { r ->
                Calendar.getInstance().apply { timeInMillis = r.date }.get(Calendar.YEAR)
            }.distinct().sorted()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val allYears = (years + currentYear).distinct().sorted()
            _availableYears.value = allYears
        }
    }

    fun loadStatsYearData() {
        viewModelScope.launch {
            val bookId = _currentAccountBookId.value
            val year = _statsYear.value
            val cal = Calendar.getInstance()
            cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfYear = cal.timeInMillis
            cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
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
            _statsYearData.value = YearlyStats(
                monthlyTotals = monthlyTotals,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                year = year
            )
        }
    }

    fun loadStatsMonthData() {
        viewModelScope.launch {
            val bookId = _currentAccountBookId.value
            val year = _statsYear.value
            val month = _statsMonth.value
            if (month == -1) return@launch

            val cal = Calendar.getInstance()
            cal.set(year, month - 1, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfMonth = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            cal.add(Calendar.MILLISECOND, -1)
            val endOfMonth = cal.timeInMillis

            val income: Double
            val expense: Double
            val expenseCategorySum: List<CategorySum>
            val incomeCategorySum: List<CategorySum>
            if (bookId == -1L) {
                income = recordRepository.getTotalByTypeAndDateRange(1, startOfMonth, endOfMonth)
                expense = recordRepository.getTotalByTypeAndDateRange(0, startOfMonth, endOfMonth)
                expenseCategorySum = recordRepository.getCategorySummary(0, startOfMonth, endOfMonth)
                incomeCategorySum = recordRepository.getCategorySummary(1, startOfMonth, endOfMonth)
            } else {
                income = recordRepository.getTotalByTypeAndDateRangeForBook(1, startOfMonth, endOfMonth, bookId)
                expense = recordRepository.getTotalByTypeAndDateRangeForBook(0, startOfMonth, endOfMonth, bookId)
                expenseCategorySum = recordRepository.getCategorySummaryForBook(0, startOfMonth, endOfMonth, bookId)
                incomeCategorySum = recordRepository.getCategorySummaryForBook(1, startOfMonth, endOfMonth, bookId)
            }
            _statsMonthData.value = MonthlyStats(
                income = income,
                expense = expense,
                balance = income - expense,
                expenseByCategory = expenseCategorySum,
                incomeByCategory = incomeCategorySum
            )
        }
    }

    fun loadStatsWeekData() {
        viewModelScope.launch {
            val bookId = _currentAccountBookId.value
            val weekIndex = _statsWeek.value
            val ranges = _statsWeekRanges.value
            if (weekIndex < 0 || weekIndex >= ranges.size) return@launch

            val (startOfWeek, endOfWeek) = ranges[weekIndex]
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

            // 加载本周每天的明细记录
            val weekRecords: List<RecordEntity>
            if (bookId == -1L) {
                weekRecords = recordRepository.getRecordsByDateRangeOnce(startOfWeek, endOfWeek)
            } else {
                weekRecords = recordRepository.getRecordsByDateRangeForBookOnce(startOfWeek, endOfWeek, bookId)
            }

            _statsWeekData.value = WeeklyStats(
                dailyTotals = dailyTotals,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                startOfWeek = startOfWeek,
                endOfWeek = endOfWeek,
                weekRecords = weekRecords
            )
        }
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
                loadAvailableYears()
                loadStatsYearData()
                loadStatsMonthData()
                loadHomeSummary()
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
                loadStatsYearData()
                loadStatsMonthData()
                loadHomeSummary()
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
                loadStatsYearData()
                loadStatsMonthData()
                loadHomeSummary()
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
                income = recordRepository.getTotalByTypeAndDateRange(1, startOfMonth, endOfMonth)
                expense = recordRepository.getTotalByTypeAndDateRange(0, startOfMonth, endOfMonth)
                expenseCategorySum = recordRepository.getCategorySummary(0, startOfMonth, endOfMonth)
                incomeCategorySum = recordRepository.getCategorySummary(1, startOfMonth, endOfMonth)
            } else {
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

    fun loadWeeklyStats() {
        viewModelScope.launch {
            val bookId = _currentAccountBookId.value
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
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

    /** 一次性获取所有记录（供导出使用） */
    suspend fun allRecordsForExport(): List<RecordEntity> {
        return recordRepository.getAllRecordsOnce()
    }

    /** 计算今日/本周/本月收支汇总 + 本月每日支出（首页折线图） */
    fun loadHomeSummary() {
        viewModelScope.launch {
            val bookId = _currentAccountBookId.value

            // ── 今日区间 ──
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayEnd = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            // ── 本周区间（周一起）──
            val weekStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }.timeInMillis
            val weekEnd = Calendar.getInstance().apply {
                timeInMillis = weekStart
                add(Calendar.DAY_OF_WEEK, 6)
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            // ── 本月区间 ──
            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val monthEnd = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            // 今日
            val todayExpense = if (bookId == -1L)
                recordRepository.getTotalByTypeAndDateRange(0, todayStart, todayEnd)
            else recordRepository.getTotalByTypeAndDateRangeForBook(0, todayStart, todayEnd, bookId)
            val todayIncome = if (bookId == -1L)
                recordRepository.getTotalByTypeAndDateRange(1, todayStart, todayEnd)
            else recordRepository.getTotalByTypeAndDateRangeForBook(1, todayStart, todayEnd, bookId)

            // 本周
            val weekExpense = if (bookId == -1L)
                recordRepository.getTotalByTypeAndDateRange(0, weekStart, weekEnd)
            else recordRepository.getTotalByTypeAndDateRangeForBook(0, weekStart, weekEnd, bookId)
            val weekIncome = if (bookId == -1L)
                recordRepository.getTotalByTypeAndDateRange(1, weekStart, weekEnd)
            else recordRepository.getTotalByTypeAndDateRangeForBook(1, weekStart, weekEnd, bookId)

            // 本月
            val monthExpense = if (bookId == -1L)
                recordRepository.getTotalByTypeAndDateRange(0, monthStart, monthEnd)
            else recordRepository.getTotalByTypeAndDateRangeForBook(0, monthStart, monthEnd, bookId)
            val monthIncome = if (bookId == -1L)
                recordRepository.getTotalByTypeAndDateRange(1, monthStart, monthEnd)
            else recordRepository.getTotalByTypeAndDateRangeForBook(1, monthStart, monthEnd, bookId)

            _todayStats.value = SimpleSummary(income = todayIncome, expense = todayExpense)
            _thisWeekStats.value = SimpleSummary(income = weekIncome, expense = weekExpense)
            _thisMonthStats.value = SimpleSummary(income = monthIncome, expense = monthExpense)

            // ── 本月每日支出（折线图）──
            val dailyTotals = if (bookId == -1L)
                recordRepository.getDailyTotals(monthStart, monthEnd)
            else recordRepository.getDailyTotalsForBook(monthStart, monthEnd, bookId)

            val dailyExpenseMap = dailyTotals
                .filter { it.type == 0 }
                .associate { it.day to it.total }
            _monthDailyExpense.value = dailyExpenseMap
        }
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
    val endOfWeek: Long = 0L,
    val weekRecords: List<RecordEntity> = emptyList()
)

data class YearlyStats(
    val monthlyTotals: List<MonthlyTotal> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR)
)

data class SimpleSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0
) {
    val balance: Double get() = income - expense
}
