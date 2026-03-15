package com.claw.accountbook.data.repository

import com.claw.accountbook.data.local.dao.AccountBookDao
import com.claw.accountbook.data.local.entity.AccountBookEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 账本数据仓库，封装账本的增删改查操作
 */
@Singleton
class AccountBookRepository @Inject constructor(
    private val accountBookDao: AccountBookDao
) {
    /**
     * 获取所有账本（Flow，实时更新）
     */
    fun getAllAccountBooks(): Flow<List<AccountBookEntity>> =
        accountBookDao.getAllAccountBooks()

    /**
     * 获取指定用户的账本
     */
    fun getAccountBooksByUser(userId: Long): Flow<List<AccountBookEntity>> =
        accountBookDao.getAccountBooksByUser(userId)

    /**
     * 获取默认账本
     */
    suspend fun getDefaultAccountBook(): AccountBookEntity? =
        accountBookDao.getDefaultAccountBook()

    /**
     * 创建账本
     */
    suspend fun createAccountBook(name: String, description: String? = null, userId: Long = 0): Long {
        val accountBook = AccountBookEntity(
            name = name,
            description = description,
            userId = userId,
            isDefault = false
        )
        return accountBookDao.insert(accountBook)
    }

    /**
     * 更新账本
     */
    suspend fun updateAccountBook(accountBook: AccountBookEntity) {
        accountBookDao.update(accountBook.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * 删除账本
     */
    suspend fun deleteAccountBook(accountBook: AccountBookEntity) {
        accountBookDao.delete(accountBook)
    }

    /**
     * 删除账本（按ID）
     */
    suspend fun deleteAccountBookById(id: Long) {
        accountBookDao.deleteById(id)
    }

    /**
     * 设置默认账本
     */
    suspend fun setDefaultAccountBook(id: Long) {
        accountBookDao.clearDefaultFlag()
        accountBookDao.setDefault(id)
    }

    /**
     * 获取账本数量
     */
    suspend fun getCount(): Int = accountBookDao.getCount()

    /**
     * 确保至少有一个默认账本，若没有则创建
     */
    suspend fun ensureDefaultAccountBook() {
        val default = accountBookDao.getDefaultAccountBook()
        if (default == null) {
            val id = accountBookDao.insert(
                AccountBookEntity(
                    name = "我的账本",
                    description = "默认账本",
                    isDefault = true
                )
            )
            accountBookDao.setDefault(id)
        }
    }
}
