package com.claw.accountbook.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.claw.accountbook.data.local.entity.AccountBookEntity
import com.claw.accountbook.data.local.entity.RecordEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 导入/导出工具类
 *
 * 支持导出为：
 *  - CSV（可直接用 Excel 打开）
 *  - SQL（INSERT 语句格式）
 *
 * 写入应用外部文件目录，通过 FileProvider 分享到系统，
 * 用户可选择保存位置（下载目录 / 云盘等）。
 */
object ExportUtils {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileNameFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * 将记录列表导出为 CSV 文件，并通过系统分享弹出保存选择器
     *
     * @param context      Context
     * @param records      要导出的记录列表
     * @param bookMap      账本ID → AccountBookEntity 映射（用于获取账本名称）
     */
    fun exportCsv(
        context: Context,
        records: List<RecordEntity>,
        bookMap: Map<Long, AccountBookEntity>
    ) {
        val fileName = "accounts_${fileNameFmt.format(Date())}.csv"
        val file = getExportFile(context, fileName)

        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            // BOM 头，让 Excel 正确识别 UTF-8
            writer.write("\uFEFF")
            // 表头
            writer.write("账本名称,日期,消费类型,消费金额,收支类型,备注\n")
            records.forEach { r ->
                val bookName = bookMap[r.accountBookId]?.name ?: "默认账本"
                val date = dateFmt.format(Date(r.date))
                val type = if (r.type == 0) "支出" else "收入"
                val note = (r.note ?: "").replace(",", "，") // 避免逗号破坏 CSV
                writer.write("$bookName,$date,${r.categoryName},${String.format("%.2f", r.amount)},$type,$note\n")
            }
        }

        shareFile(context, file, "text/csv", "导出 CSV 账单")
    }

    /**
     * 将记录列表导出为 SQL 文件（INSERT 语句）
     */
    fun exportSql(
        context: Context,
        records: List<RecordEntity>,
        bookMap: Map<Long, AccountBookEntity>
    ) {
        val fileName = "accounts_${fileNameFmt.format(Date())}.sql"
        val file = getExportFile(context, fileName)

        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("-- Claw 记账本导出 SQL\n")
            writer.write("-- 导出时间：${dateFmt.format(Date())}\n\n")
            writer.write(
                "CREATE TABLE IF NOT EXISTS records_import (\n" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "  account_book_name TEXT,\n" +
                "  date TEXT,\n" +
                "  category_name TEXT,\n" +
                "  amount REAL,\n" +
                "  type TEXT,\n" +
                "  note TEXT\n" +
                ");\n\n"
            )
            records.forEach { r ->
                val bookName = (bookMap[r.accountBookId]?.name ?: "默认账本").sqlEscape()
                val date = dateFmt.format(Date(r.date)).sqlEscape()
                val category = r.categoryName.sqlEscape()
                val type = if (r.type == 0) "支出" else "收入"
                val note = (r.note ?: "").sqlEscape()
                writer.write(
                    "INSERT INTO records_import (account_book_name, date, category_name, amount, type, note) " +
                    "VALUES ('$bookName', '$date', '$category', ${String.format("%.2f", r.amount)}, '$type', '$note');\n"
                )
            }
        }

        shareFile(context, file, "application/sql", "导出 SQL 账单")
    }

    // ── 私有辅助 ──────────────────────────────────────

    private fun getExportFile(context: Context, fileName: String): File {
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        return File(exportDir, fileName)
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun String.sqlEscape(): String = this.replace("'", "''")
}
