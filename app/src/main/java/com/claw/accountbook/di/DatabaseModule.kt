package com.claw.accountbook.di

import android.content.Context
import com.claw.accountbook.data.local.AccountBookDatabase
import com.claw.accountbook.data.local.SessionManager
import com.claw.accountbook.data.local.dao.AccountBookDao
import com.claw.accountbook.data.local.dao.CategoryDao
import com.claw.accountbook.data.local.dao.RecordDao
import com.claw.accountbook.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AccountBookDatabase {
        return AccountBookDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideRecordDao(database: AccountBookDatabase): RecordDao {
        return database.recordDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: AccountBookDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AccountBookDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideAccountBookDao(database: AccountBookDatabase): AccountBookDao {
        return database.accountBookDao()
    }

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }
}
