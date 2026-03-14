package com.claw.accountbook.data.local.dao

import androidx.room.*
import com.claw.accountbook.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据访问对象
 */
@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY type, name")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name")
    fun getCategoriesByType(type: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isDefault = 1")
    suspend fun getDefaultCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE userId = :userId OR isDefault = 1")
    fun getCategoriesForUser(userId: Long): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
