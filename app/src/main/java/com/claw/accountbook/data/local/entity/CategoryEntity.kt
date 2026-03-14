package com.claw.accountbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类实体类
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,             // 分类名称
    val icon: String,              // 图标名称
    val type: Int,                // 类型：0-支出，1-收入
    val isDefault: Boolean = false, // 是否为默认分类
    val userId: Long? = null,     // 用户ID（如果是自定义分类）
    val createdAt: Long = System.currentTimeMillis()
)
