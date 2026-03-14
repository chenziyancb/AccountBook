package com.claw.accountbook.data.repository

import com.claw.accountbook.data.local.dao.CategoryDao
import com.claw.accountbook.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分类仓库
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    fun getCategoriesByType(type: Int): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesByType(type)
    }

    fun getCategoriesForUser(userId: Long): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesForUser(userId)
    }

    suspend fun getById(id: Long): CategoryEntity? {
        return categoryDao.getById(id)
    }

    suspend fun insert(category: CategoryEntity): Long {
        return categoryDao.insert(category)
    }

    suspend fun update(category: CategoryEntity) {
        categoryDao.update(category)
    }

    suspend fun delete(category: CategoryEntity) {
        categoryDao.delete(category)
    }

    suspend fun getDefaultCategories(): List<CategoryEntity> {
        return categoryDao.getDefaultCategories()
    }
}
