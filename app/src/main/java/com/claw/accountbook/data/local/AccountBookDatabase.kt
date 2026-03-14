package com.claw.accountbook.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.claw.accountbook.data.local.dao.CategoryDao
import com.claw.accountbook.data.local.dao.RecordDao
import com.claw.accountbook.data.local.dao.UserDao
import com.claw.accountbook.data.local.entity.CategoryEntity
import com.claw.accountbook.data.local.entity.RecordEntity
import com.claw.accountbook.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 记账本数据库
 */
@Database(
    entities = [
        RecordEntity::class,
        CategoryEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AccountBookDatabase : RoomDatabase() {

    abstract fun recordDao(): RecordDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AccountBookDatabase? = null

        fun getDatabase(context: Context): AccountBookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AccountBookDatabase::class.java,
                    "accountbook_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultCategories(database.categoryDao())
                    }
                }
            }
        }

        private suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
            // 默认支出分类
            val expenseCategories = listOf(
                CategoryEntity(name = "餐饮", icon = "restaurant", type = 0, isDefault = true),
                CategoryEntity(name = "交通", icon = "directions_car", type = 0, isDefault = true),
                CategoryEntity(name = "购物", icon = "shopping_bag", type = 0, isDefault = true),
                CategoryEntity(name = "娱乐", icon = "movie", type = 0, isDefault = true),
                CategoryEntity(name = "居住", icon = "home", type = 0, isDefault = true),
                CategoryEntity(name = "医疗", icon = "local_hospital", type = 0, isDefault = true),
                CategoryEntity(name = "教育", icon = "school", type = 0, isDefault = true),
                CategoryEntity(name = "其他", icon = "more_horiz", type = 0, isDefault = true)
            )

            // 默认收入分类
            val incomeCategories = listOf(
                CategoryEntity(name = "工资", icon = "work", type = 1, isDefault = true),
                CategoryEntity(name = "奖金", icon = "card_giftcard", type = 1, isDefault = true),
                CategoryEntity(name = "投资", icon = "trending_up", type = 1, isDefault = true),
                CategoryEntity(name = "其他", icon = "more_horiz", type = 1, isDefault = true)
            )

            categoryDao.insertAll(expenseCategories + incomeCategories)
        }
    }
}
