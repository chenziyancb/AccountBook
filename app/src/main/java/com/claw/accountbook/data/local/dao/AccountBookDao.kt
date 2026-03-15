package com.claw.accountbook.data.local.dao

import androidx.room.*
import com.claw.accountbook.data.local.entity.AccountBookEntity
import kotlinx.coroutines.flow.Flow

/**
 * 账本数据访问对象
 */
@Dao
interface AccountBookDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(accountBook: AccountBookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accountBooks: List<AccountBookEntity>)

    @Update
    suspend fun update(accountBook: AccountBookEntity)

    @Delete
    suspend fun delete(accountBook: AccountBookEntity)

    @Query("SELECT * FROM account_books ORDER BY createdAt ASC")
    fun getAllAccountBooks(): Flow<List<AccountBookEntity>>

    @Query("SELECT * FROM account_books WHERE userId = :userId OR userId = 0 ORDER BY createdAt ASC")
    fun getAccountBooksByUser(userId: Long): Flow<List<AccountBookEntity>>

    @Query("SELECT * FROM account_books WHERE id = :id")
    suspend fun getById(id: Long): AccountBookEntity?

    @Query("SELECT * FROM account_books WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccountBook(): AccountBookEntity?

    @Query("UPDATE account_books SET isDefault = 0")
    suspend fun clearDefaultFlag()

    @Query("UPDATE account_books SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)

    @Query("DELETE FROM account_books WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM account_books")
    suspend fun getCount(): Int
}
