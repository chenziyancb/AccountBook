package com.claw.accountbook.data.repository

import com.claw.accountbook.data.local.dao.CategorySum
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

    suspend fun getCategorySummary(type: Int, startDate: Long, endDate: Long): List<CategorySum> {
        return recordDao.getCategorySummary(type, startDate, endDate)
    }
}
