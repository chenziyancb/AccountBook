package com.claw.accountbook.data.local.dao

import androidx.room.*
import com.claw.accountbook.data.local.entity.RecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 记账记录数据访问对象
 */
@Dao
interface RecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RecordEntity): Long

    @Update
    suspend fun update(record: RecordEntity)

    @Delete
    suspend fun delete(record: RecordEntity)

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getById(id: Long): RecordEntity?

    @Query("SELECT * FROM records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getRecordsByDateRange(startDate: Long, endDate: Long): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE type = :type ORDER BY date DESC")
    fun getRecordsByType(type: Int): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getRecordsByCategory(categoryId: Long): Flow<List<RecordEntity>>

    @Query("SELECT SUM(amount) FROM records WHERE type = :type AND date >= :startDate AND date <= :endDate")
    suspend fun getTotalByTypeAndDateRange(type: Int, startDate: Long, endDate: Long): Double?

    @Query("SELECT categoryId, categoryName, SUM(amount) as total FROM records WHERE type = :type AND date >= :startDate AND date <= :endDate GROUP BY categoryId ORDER BY total DESC")
    suspend fun getCategorySummary(type: Int, startDate: Long, endDate: Long): List<CategorySum>

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM records")
    suspend fun deleteAll()
}

data class CategorySum(
    val categoryId: Long,
    val categoryName: String = "",
    val total: Double
)
