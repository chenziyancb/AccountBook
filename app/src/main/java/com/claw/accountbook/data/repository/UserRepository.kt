package com.claw.accountbook.data.repository

import com.claw.accountbook.data.local.SessionManager
import com.claw.accountbook.data.local.dao.UserDao
import com.claw.accountbook.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户仓库
 * 支持：用户注册、登录验证、密码 SHA-256 加密、Session 持久化
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {

    fun getAllUsers(): Flow<List<UserEntity>> {
        return userDao.getAllUsers()
    }

    suspend fun getById(id: Long): UserEntity? {
        return userDao.getById(id)
    }

    suspend fun getByUsername(username: String): UserEntity? {
        return userDao.getByUsername(username)
    }

    suspend fun getByEmail(email: String): UserEntity? {
        return userDao.getByEmail(email)
    }

    suspend fun register(username: String, password: String, email: String? = null): Result<UserEntity> {
        // 检查用户名是否已存在
        if (userDao.getByUsername(username) != null) {
            return Result.failure(Exception("用户名已存在"))
        }

        // 检查邮箱是否已存在
        if (email != null && userDao.getByEmail(email) != null) {
            return Result.failure(Exception("邮箱已被注册"))
        }

        // 加密密码（SHA-256）
        val passwordHash = hashPassword(password)

        // 创建用户
        val user = UserEntity(
            username = username,
            passwordHash = passwordHash,
            email = email
        )

        val id = userDao.insert(user)
        val savedUser = user.copy(id = id)

        // 注册成功后自动保存 Session
        sessionManager.saveLoginSession(savedUser.id, savedUser.username)

        return Result.success(savedUser)
    }

    suspend fun login(username: String, password: String): Result<UserEntity> {
        val passwordHash = hashPassword(password)
        val user = userDao.login(username, passwordHash)

        return if (user != null) {
            // 登录成功，持久化 Session
            sessionManager.saveLoginSession(user.id, user.username)
            Result.success(user)
        } else {
            Result.failure(Exception("用户名或密码错误"))
        }
    }

    /**
     * 退出登录，清除 Session
     */
    fun logout() {
        sessionManager.clearSession()
    }

    /**
     * 从 SharedPreferences 恢复登录状态（应用重启后调用）
     * 返回已登录用户，若无有效 Session 则返回 null
     */
    suspend fun restoreSession(): UserEntity? {
        if (!sessionManager.isLoggedIn()) return null
        val savedId = sessionManager.getSavedUserId()
        if (savedId == -1L) return null
        return userDao.getById(savedId)
    }

    /**
     * 修改用户信息
     */
    suspend fun updateUserInfo(user: UserEntity) {
        userDao.update(user)
        // 更新 Session 中的用户名
        sessionManager.saveLoginSession(user.id, user.username)
    }

    /**
     * 更新密码（传入明文新密码，内部完成哈希）
     */
    suspend fun updatePassword(user: UserEntity, newPassword: String): UserEntity {
        val newHash = hashPassword(newPassword)
        val updatedUser = user.copy(
            passwordHash = newHash,
            updatedAt = System.currentTimeMillis()
        )
        userDao.update(updatedUser)
        return updatedUser
    }

    suspend fun update(user: UserEntity) {
        userDao.update(user)
    }

    suspend fun delete(user: UserEntity) {
        userDao.delete(user)
    }

    suspend fun getUserCount(): Int {
        return userDao.getUserCount()
    }

    /**
     * 密码 SHA-256 哈希加密（无盐，适合本地存储）
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
