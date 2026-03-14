package com.claw.accountbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.claw.accountbook.data.local.dao.CategorySum
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

    // 所有记录
    val allRecords: StateFlow<List<RecordEntity>> = recordRepository.getAllRecords()
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

    // 日期范围统计
    private val _monthlyStats = MutableStateFlow(MonthlyStats())
    val monthlyStats: StateFlow<MonthlyStats> = _monthlyStats.asStateFlow()

    init {
        loadMonthlyStats()
    }

    fun setRecordType(type: Int) {
        _recordType.value = type
    }

    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
        loadMonthlyStats()
    }

    fun addRecord(
        amount: Double,
        categoryId: Long,
        categoryName: String,
        note: String?
    ) {
        viewModelScope.launch {
            try {
                val record = RecordEntity(
                    amount = amount,
                    type = _recordType.value,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    note = note,
                    date = _selectedDate.value
                )
                recordRepository.insert(record)
                loadMonthlyStats()
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
                _uiState.update { it.copy(message = "记录删除成功") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun loadMonthlyStats() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = _selectedDate.value
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.timeInMillis

            val income = recordRepository.getTotalByTypeAndDateRange(1, startOfMonth, endOfMonth)
            val expense = recordRepository.getTotalByTypeAndDateRange(0, startOfMonth, endOfMonth)
            val expenseCategorySum = recordRepository.getCategorySummary(0, startOfMonth, endOfMonth)
            val incomeCategorySum = recordRepository.getCategorySummary(1, startOfMonth, endOfMonth)

            _monthlyStats.value = MonthlyStats(
                income = income,
                expense = expense,
                balance = income - expense,
                expenseByCategory = expenseCategorySum,
                incomeByCategory = incomeCategorySum
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
