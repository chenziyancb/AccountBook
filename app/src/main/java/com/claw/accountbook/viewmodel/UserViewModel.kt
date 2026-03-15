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
 * 支持：登录、注册、退出、Session 自动恢复、修改用户名/密码
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

    /**
     * 修改用户名
     * @param newUsername 新用户名（3-20字符）
     */
    fun updateUsername(newUsername: String) {
        val user = _currentUser.value ?: return
        if (newUsername.isBlank() || newUsername.length < 3 || newUsername.length > 20) {
            _uiState.update { it.copy(error = "用户名须为 3-20 个字符") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // 检查新用户名是否已被占用
            val existing = userRepository.getByUsername(newUsername)
            if (existing != null && existing.id != user.id) {
                _uiState.update { it.copy(isLoading = false, error = "用户名已被占用") }
                return@launch
            }
            val updatedUser = user.copy(
                username = newUsername,
                updatedAt = System.currentTimeMillis()
            )
            userRepository.updateUserInfo(updatedUser)
            _currentUser.value = updatedUser
            _uiState.update { it.copy(isLoading = false, message = "用户名已更新") }
        }
    }

    /**
     * 修改密码
     * @param oldPassword 旧密码（用于验证）
     * @param newPassword 新密码（≥6字符）
     */
    fun updatePassword(oldPassword: String, newPassword: String) {
        val user = _currentUser.value ?: return
        if (newPassword.length < 6) {
            _uiState.update { it.copy(error = "新密码不能少于 6 个字符") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // 验证旧密码
            val result = userRepository.login(user.username, oldPassword)
            if (result.isFailure) {
                _uiState.update { it.copy(isLoading = false, error = "原密码错误") }
                return@launch
            }
            val updatedUser = userRepository.updatePassword(user, newPassword)
            _currentUser.value = updatedUser
            _uiState.update { it.copy(isLoading = false, message = "密码已更新") }
        }
    }

    /**
     * 通过邮箱找回密码（重置为新密码）
     * @param email 注册时绑定的邮箱
     * @param newPassword 新密码
     */
    fun resetPasswordByEmail(email: String, newPassword: String) {
        if (newPassword.length < 6) {
            _uiState.update { it.copy(error = "新密码不能少于 6 个字符") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = userRepository.getByEmail(email)
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, error = "未找到该邮箱对应的账号") }
                return@launch
            }
            userRepository.updatePassword(user, newPassword)
            _uiState.update { it.copy(isLoading = false, message = "密码已重置，请用新密码登录") }
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

