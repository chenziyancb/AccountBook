package com.claw.accountbook.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 记账记录实体类
 */
@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,           // 金额
    val type: Int,                // 类型：0-支出，1-收入
    val categoryId: Long,         // 分类ID
    val categoryName: String,     // 分类名称
    val note: String?,            // 备注
    val date: Long,               // 日期时间戳
    val accountBookId: Long = 1,  // 所属账本ID
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
