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

    @Query("SELECT * FROM records WHERE accountBookId = :accountBookId ORDER BY date DESC")
    fun getRecordsByAccountBook(accountBookId: Long): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getRecordsByDateRange(startDate: Long, endDate: Long): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE type = :type ORDER BY date DESC")
    fun getRecordsByType(type: Int): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getRecordsByCategory(categoryId: Long): Flow<List<RecordEntity>>

    @Query("SELECT SUM(amount) FROM records WHERE type = :type AND date >= :startDate AND date <= :endDate")
    suspend fun getTotalByTypeAndDateRange(type: Int, startDate: Long, endDate: Long): Double?

    @Query("SELECT SUM(amount) FROM records WHERE type = :type AND date >= :startDate AND date <= :endDate AND accountBookId = :accountBookId")
    suspend fun getTotalByTypeAndDateRangeForBook(type: Int, startDate: Long, endDate: Long, accountBookId: Long): Double?

    @Query("SELECT categoryId, categoryName, SUM(amount) as total FROM records WHERE type = :type AND date >= :startDate AND date <= :endDate GROUP BY categoryId ORDER BY total DESC")
    suspend fun getCategorySummary(type: Int, startDate: Long, endDate: Long): List<CategorySum>

    @Query("SELECT categoryId, categoryName, SUM(amount) as total FROM records WHERE type = :type AND date >= :startDate AND date <= :endDate AND accountBookId = :accountBookId GROUP BY categoryId ORDER BY total DESC")
    suspend fun getCategorySummaryForBook(type: Int, startDate: Long, endDate: Long, accountBookId: Long): List<CategorySum>

    /**
     * 按月份获取汇总统计（用于年统计）
     * 返回每月的收支总额，strftime('%m', date/1000, 'unixepoch') 提取月份
     */
    @Query("""
        SELECT 
            CAST(strftime('%m', date/1000, 'unixepoch') AS INTEGER) as month,
            type,
            SUM(amount) as total
        FROM records 
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY month, type
        ORDER BY month ASC
    """)
    suspend fun getMonthlyTotals(startDate: Long, endDate: Long): List<MonthlyTotal>

    @Query("""
        SELECT 
            CAST(strftime('%m', date/1000, 'unixepoch') AS INTEGER) as month,
            type,
            SUM(amount) as total
        FROM records 
        WHERE date >= :startDate AND date <= :endDate AND accountBookId = :accountBookId
        GROUP BY month, type
        ORDER BY month ASC
    """)
    suspend fun getMonthlyTotalsForBook(startDate: Long, endDate: Long, accountBookId: Long): List<MonthlyTotal>

    /**
     * 按日期获取汇总统计（用于周统计）
     */
    @Query("""
        SELECT 
            CAST(strftime('%d', date/1000, 'unixepoch') AS INTEGER) as day,
            CAST(strftime('%m', date/1000, 'unixepoch') AS INTEGER) as month,
            type,
            SUM(amount) as total
        FROM records 
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY day, month, type
        ORDER BY date ASC
    """)
    suspend fun getDailyTotals(startDate: Long, endDate: Long): List<DailyTotal>

    @Query("""
        SELECT 
            CAST(strftime('%d', date/1000, 'unixepoch') AS INTEGER) as day,
            CAST(strftime('%m', date/1000, 'unixepoch') AS INTEGER) as month,
            type,
            SUM(amount) as total
        FROM records 
        WHERE date >= :startDate AND date <= :endDate AND accountBookId = :accountBookId
        GROUP BY day, month, type
        ORDER BY date ASC
    """)
    suspend fun getDailyTotalsForBook(startDate: Long, endDate: Long, accountBookId: Long): List<DailyTotal>

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM records")
    suspend fun deleteAll()

    @Query("SELECT * FROM records ORDER BY date DESC")
    suspend fun getAllRecordsOnce(): List<RecordEntity>

    @Query("SELECT * FROM records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getRecordsByDateRangeOnce(startDate: Long, endDate: Long): List<RecordEntity>

    @Query("SELECT * FROM records WHERE date >= :startDate AND date <= :endDate AND accountBookId = :accountBookId ORDER BY date ASC")
    suspend fun getRecordsByDateRangeForBookOnce(startDate: Long, endDate: Long, accountBookId: Long): List<RecordEntity>
}

data class CategorySum(
    val categoryId: Long,
    val categoryName: String = "",
    val total: Double
)

data class MonthlyTotal(
    val month: Int,
    val type: Int,
    val total: Double
)

data class DailyTotal(
    val day: Int,
    val month: Int,
    val type: Int,
    val total: Double
)
