package com.claw.accountbook.data.repository

import com.claw.accountbook.data.local.dao.CategorySum
import com.claw.accountbook.data.local.dao.DailyTotal
import com.claw.accountbook.data.local.dao.MonthlyTotal
import com.claw.accountbook.data.local.dao.RecordDao
import com.claw.accountbook.data.local.entity.RecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 记账记录仓库
 */
@Singleton
class RecordRepository @Inject constructor(
    private val recordDao: RecordDao
) {

    fun getAllRecords(): Flow<List<RecordEntity>> {
        return recordDao.getAllRecords()
    }

    fun getRecordsByAccountBook(accountBookId: Long): Flow<List<RecordEntity>> {
        return if (accountBookId == -1L) recordDao.getAllRecords()
        else recordDao.getRecordsByAccountBook(accountBookId)
    }

    fun getRecordsByDateRange(startDate: Long, endDate: Long): Flow<List<RecordEntity>> {
        return recordDao.getRecordsByDateRange(startDate, endDate)
    }

    fun getRecordsByType(type: Int): Flow<List<RecordEntity>> {
        return recordDao.getRecordsByType(type)
    }

    fun getRecordsByCategory(categoryId: Long): Flow<List<RecordEntity>> {
        return recordDao.getRecordsByCategory(categoryId)
    }

    suspend fun getById(id: Long): RecordEntity? {
        return recordDao.getById(id)
    }

    suspend fun insert(record: RecordEntity): Long {
        return recordDao.insert(record)
    }

    suspend fun update(record: RecordEntity) {
        recordDao.update(record)
    }

    suspend fun delete(record: RecordEntity) {
        recordDao.delete(record)
    }

    suspend fun deleteById(id: Long) {
        recordDao.deleteById(id)
    }

    suspend fun getTotalByTypeAndDateRange(type: Int, startDate: Long, endDate: Long): Double {
        return recordDao.getTotalByTypeAndDateRange(type, startDate, endDate) ?: 0.0
    }

    suspend fun getTotalByTypeAndDateRangeForBook(type: Int, startDate: Long, endDate: Long, accountBookId: Long): Double {
        return recordDao.getTotalByTypeAndDateRangeForBook(type, startDate, endDate, accountBookId) ?: 0.0
    }

    suspend fun getCategorySummary(type: Int, startDate: Long, endDate: Long): List<CategorySum> {
        return recordDao.getCategorySummary(type, startDate, endDate)
    }

    suspend fun getCategorySummaryForBook(type: Int, startDate: Long, endDate: Long, accountBookId: Long): List<CategorySum> {
        return recordDao.getCategorySummaryForBook(type, startDate, endDate, accountBookId)
    }

    /**
     * 获取年度每月汇总数据（用于年统计图表）
     */
    suspend fun getMonthlyTotals(startDate: Long, endDate: Long): List<MonthlyTotal> {
        return recordDao.getMonthlyTotals(startDate, endDate)
    }

    suspend fun getMonthlyTotalsForBook(startDate: Long, endDate: Long, accountBookId: Long): List<MonthlyTotal> {
        return recordDao.getMonthlyTotalsForBook(startDate, endDate, accountBookId)
    }

    /**
     * 获取周内每日汇总数据（用于周统计图表）
     */
    suspend fun getDailyTotals(startDate: Long, endDate: Long): List<DailyTotal> {
        return recordDao.getDailyTotals(startDate, endDate)
    }

    suspend fun getDailyTotalsForBook(startDate: Long, endDate: Long, accountBookId: Long): List<DailyTotal> {
        return recordDao.getDailyTotalsForBook(startDate, endDate, accountBookId)
    }

    suspend fun getAllRecordsOnce(): List<RecordEntity> {
        return recordDao.getAllRecordsOnce()
    }

    suspend fun getRecordsByDateRangeOnce(startDate: Long, endDate: Long): List<RecordEntity> {
        return recordDao.getRecordsByDateRangeOnce(startDate, endDate)
    }

    suspend fun getRecordsByDateRangeForBookOnce(startDate: Long, endDate: Long, accountBookId: Long): List<RecordEntity> {
        return recordDao.getRecordsByDateRangeForBookOnce(startDate, endDate, accountBookId)
    }
}
