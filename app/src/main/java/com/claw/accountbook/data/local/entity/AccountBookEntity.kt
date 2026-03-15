package com.claw.accountbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 账本实体类
 */
@Entity(tableName = "account_books")
data class AccountBookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,               // 账本名称
    val description: String? = null, // 账本描述
    val userId: Long = 0,           // 所属用户ID（0表示全局）
    val isDefault: Boolean = false, // 是否为默认账本
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
