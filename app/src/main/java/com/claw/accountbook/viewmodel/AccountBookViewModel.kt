package com.claw.accountbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.claw.accountbook.data.local.entity.AccountBookEntity
import com.claw.accountbook.data.repository.AccountBookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 账本 ViewModel — 管理账本列表、当前选中账本、创建/删除账本等操作
 */
@HiltViewModel
class AccountBookViewModel @Inject constructor(
    private val accountBookRepository: AccountBookRepository
) : ViewModel() {

    // 所有账本列表
    val accountBooks: StateFlow<List<AccountBookEntity>> =
        accountBookRepository.getAllAccountBooks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当前选中的账本ID（-1 表示"全部账本"）
    private val _selectedAccountBookId = MutableStateFlow<Long>(-1L)
    val selectedAccountBookId: StateFlow<Long> = _selectedAccountBookId.asStateFlow()

    // 当前选中账本的名称（用于UI显示）
    val selectedAccountBookName: StateFlow<String> = combine(
        accountBooks, _selectedAccountBookId
    ) { books, selectedId ->
        when {
            selectedId == -1L -> "全部账本"
            else -> books.find { it.id == selectedId }?.name ?: "我的账本"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "全部账本")

    // UI状态（消息/错误）
    private val _uiState = MutableStateFlow(AccountBookUiState())
    val uiState: StateFlow<AccountBookUiState> = _uiState.asStateFlow()

    init {
        // 初始化时设置默认账本
        viewModelScope.launch {
            accountBookRepository.ensureDefaultAccountBook()
            val default = accountBookRepository.getDefaultAccountBook()
            if (default != null) {
                _selectedAccountBookId.value = default.id
            }
        }
    }

    /**
     * 切换当前账本
     */
    fun selectAccountBook(id: Long) {
        _selectedAccountBookId.value = id
    }

    /**
     * 选择"全部账本"
     */
    fun selectAllAccountBooks() {
        _selectedAccountBookId.value = -1L
    }

    /**
     * 创建新账本
     */
    fun createAccountBook(name: String, description: String? = null) {
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "账本名称不能为空") }
            return
        }
        viewModelScope.launch {
            try {
                val id = accountBookRepository.createAccountBook(name.trim(), description)
                _uiState.update { it.copy(message = "账本「${name.trim()}」创建成功") }
                // 自动切换到新建的账本
                _selectedAccountBookId.value = id
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "创建失败：${e.message}") }
            }
        }
    }

    /**
     * 删除账本
     */
    fun deleteAccountBook(accountBook: AccountBookEntity) {
        viewModelScope.launch {
            try {
                accountBookRepository.deleteAccountBook(accountBook)
                // 若删除的是当前选中账本，切换到默认账本
                if (_selectedAccountBookId.value == accountBook.id) {
                    val default = accountBookRepository.getDefaultAccountBook()
                    _selectedAccountBookId.value = default?.id ?: -1L
                }
                _uiState.update { it.copy(message = "账本已删除") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除失败：${e.message}") }
            }
        }
    }

    /**
     * 重命名账本
     */
    fun renameAccountBook(accountBook: AccountBookEntity, newName: String) {
        if (newName.isBlank()) {
            _uiState.update { it.copy(error = "账本名称不能为空") }
            return
        }
        viewModelScope.launch {
            try {
                accountBookRepository.updateAccountBook(accountBook.copy(name = newName.trim()))
                _uiState.update { it.copy(message = "账本已重命名") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "重命名失败：${e.message}") }
            }
        }
    }

    /**
     * 设置默认账本
     */
    fun setDefaultAccountBook(id: Long) {
        viewModelScope.launch {
            try {
                accountBookRepository.setDefaultAccountBook(id)
                _uiState.update { it.copy(message = "已设为默认账本") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "操作失败：${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}

data class AccountBookUiState(
    val message: String? = null,
    val error: String? = null
)
