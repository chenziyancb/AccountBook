package com.claw.accountbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.claw.accountbook.data.local.entity.UserEntity
import com.claw.accountbook.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户 ViewModel
 * 支持：登录、注册、退出、Session 自动恢复
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    init {
        restoreSession()
    }

    /**
     * 从 SharedPreferences 恢复登录状态
     * 若存在有效 Session，则自动跳过登录页
     */
    private fun restoreSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = userRepository.restoreSession()
            if (user != null) {
                _currentUser.value = user
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true
                    )
                }
            } else {
                // 无有效 Session，检查是否首次启动
                val userCount = userRepository.getUserCount()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFirstLaunch = userCount == 0
                    )
                }
            }
        }
    }

    fun register(username: String, password: String, email: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            userRepository.register(username, password, email)
                .onSuccess { user ->
                    _currentUser.value = user
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            message = "注册成功"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            userRepository.login(username, password)
                .onSuccess { user ->
                    _currentUser.value = user
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            message = "登录成功"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
        }
    }

    fun logout() {
        userRepository.logout()
        _currentUser.value = null
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                message = "已退出登录"
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}

data class UserUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isFirstLaunch: Boolean = false,
    val message: String? = null,
    val error: String? = null
)
