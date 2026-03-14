package com.claw.accountbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户实体类
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,          // 用户名
    val passwordHash: String,     // 密码哈希
    val email: String?,            // 邮箱
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
