package com.claw.accountbook.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.claw.accountbook.data.local.dao.AccountBookDao
import com.claw.accountbook.data.local.dao.CategoryDao
import com.claw.accountbook.data.local.dao.RecordDao
import com.claw.accountbook.data.local.dao.UserDao
import com.claw.accountbook.data.local.entity.AccountBookEntity
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
        UserEntity::class,
        AccountBookEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AccountBookDatabase : RoomDatabase() {

    abstract fun recordDao(): RecordDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userDao(): UserDao
    abstract fun accountBookDao(): AccountBookDao

    companion object {
        @Volatile
        private var INSTANCE: AccountBookDatabase? = null

        /**
         * 数据库迁移：v1 -> v2（添加 account_books 表 + records.accountBookId 列）
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建账本表
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS account_books (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        userId INTEGER NOT NULL DEFAULT 0,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                // 插入默认账本
                val now = System.currentTimeMillis()
                database.execSQL(
                    "INSERT INTO account_books (name, description, userId, isDefault, createdAt, updatedAt) " +
                    "VALUES ('我的账本', '默认账本', 0, 1, $now, $now)"
                )
                // 给 records 表添加 accountBookId 列（默认值1对应上面插入的默认账本）
                database.execSQL(
                    "ALTER TABLE records ADD COLUMN accountBookId INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        /**
         * 数据库迁移：v2 -> v3（给 users.username 加唯一索引）
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // SQLite 不支持 ALTER TABLE ADD UNIQUE，需要重建索引
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_users_username ON users(username)"
                )
            }
        }

        fun getDatabase(context: Context): AccountBookDatabase {
            return INSTANCE ?: synchronized(this) {
                // 先创建 Callback 实例，build() 后再把 db 引用传进去
                val callback = DatabaseCallback()
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AccountBookDatabase::class.java,
                    "accountbook_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(callback)
                    .build()
                INSTANCE = instance
                // build() 完成后，把真实实例注入 callback，避免 onCreate 时 INSTANCE 为 null
                callback.database = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            // 由外部在 build() 之后赋值
            var database: AccountBookDatabase? = null

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val db = database ?: return
                CoroutineScope(Dispatchers.IO).launch {
                    populateDefaultData(db)
                }
            }
        }

        private suspend fun populateDefaultData(database: AccountBookDatabase) {
            // 插入默认账本
            database.accountBookDao().insert(
                AccountBookEntity(
                    name = "我的账本",
                    description = "默认账本",
                    isDefault = true
                )
            )
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
            database.categoryDao().insertAll(expenseCategories + incomeCategories)
        }
    }
}
