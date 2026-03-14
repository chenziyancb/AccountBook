package com.claw.accountbook.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claw.accountbook.data.local.entity.CategoryEntity
import com.claw.accountbook.data.local.entity.RecordEntity
import com.claw.accountbook.ui.theme.ExpenseColor
import com.claw.accountbook.ui.theme.IncomeColor
import com.claw.accountbook.viewmodel.RecordViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 首页 - 显示记账记录列表，支持月份筛选和记录编辑/删除
 */
@Composable
fun HomeScreen(
    viewModel: RecordViewModel = hiltViewModel()
) {
    val records by viewModel.allRecords.collectAsStateWithLifecycle()
    val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 用于显示编辑对话框的记录
    var editingRecord by remember { mutableStateOf<RecordEntity?>(null) }
    // 用于显示删除确认的记录
    var deletingRecord by remember { mutableStateOf<RecordEntity?>(null) }

    // 根据 selectedDate 过滤本月记录
    val filteredRecords = remember(records, selectedDate) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        records.filter { record ->
            val recCal = Calendar.getInstance().apply { timeInMillis = record.date }
            recCal.get(Calendar.YEAR) == year && recCal.get(Calendar.MONTH) == month
        }.sortedByDescending { it.date }
    }

    // 月份格式化
    val monthLabel = remember(selectedDate) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
    }

    // 消息提示
    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 月份选择导航条
        MonthNavigator(
            monthLabel = monthLabel,
            onPrevMonth = {
                val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                cal.add(Calendar.MONTH, -1)
                viewModel.setSelectedDate(cal.timeInMillis)
            },
            onNextMonth = {
                val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                cal.add(Calendar.MONTH, 1)
                viewModel.setSelectedDate(cal.timeInMillis)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 月度概览卡片
        MonthlyOverviewCard(
            income = monthlyStats.income,
            expense = monthlyStats.expense,
            balance = monthlyStats.balance
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 记录列表标题
        Text(
            text = "收支明细（${filteredRecords.size}笔）",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "本月暂无记录，点击 + 添加第一笔",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRecords, key = { it.id }) { record ->
                    RecordItem(
                        record = record,
                        onEdit = { editingRecord = it },
                        onDelete = { deletingRecord = it }
                    )
                }
            }
        }
    }

    // 编辑记录对话框
    editingRecord?.let { record ->
        EditRecordDialog(
            record = record,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            onDismiss = { editingRecord = null },
            onConfirm = { updatedRecord ->
                viewModel.updateRecord(updatedRecord)
                editingRecord = null
            }
        )
    }

    // 删除确认对话框
    deletingRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { deletingRecord = null },
            title = { Text("删除记录") },
            text = {
                Text("确定要删除这笔${if (record.type == 1) "收入" else "支出"}记录吗？\n金额：¥${String.format("%.2f", record.amount)}")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(record)
                        deletingRecord = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingRecord = null }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 月份导航条
 */
@Composable
fun MonthNavigator(
    monthLabel: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevMonth) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "上月",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "下月",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 月度概览卡片
 */
@Composable
fun MonthlyOverviewCard(
    income: Double,
    expense: Double,
    balance: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "本月收支",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 收入
                Column {
                    Text(
                        text = "收入",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "¥${String.format("%.2f", income)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = IncomeColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 支出
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "支出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "¥${String.format("%.2f", expense)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = ExpenseColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 余额
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "结余: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "¥${String.format("%.2f", balance)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (balance >= 0) IncomeColor else ExpenseColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 记录项 - 支持点击编辑/删除
 */
@Composable
fun RecordItem(
    record: RecordEntity,
    onEdit: (RecordEntity) -> Unit,
    onDelete: (RecordEntity) -> Unit
) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMenu = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (!record.note.isNullOrBlank()) {
                    Text(
                        text = record.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = dateFormat.format(Date(record.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${if (record.type == 1) "+" else "-"}¥${String.format("%.2f", record.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = if (record.type == 1) IncomeColor else ExpenseColor,
                fontWeight = FontWeight.Bold
            )
        }

        // 操作菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("编辑") },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                },
                onClick = {
                    showMenu = false
                    onEdit(record)
                }
            )
            DropdownMenuItem(
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    showMenu = false
                    onDelete(record)
                }
            )
        }
    }
}

/**
 * 编辑记录对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordDialog(
    record: RecordEntity,
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (RecordEntity) -> Unit
) {
    var amountText by remember { mutableStateOf(record.amount.toString()) }
    var note by remember { mutableStateOf(record.note ?: "") }
    var selectedType by remember { mutableIntStateOf(record.type) }
    var selectedCategoryId by remember { mutableStateOf(record.categoryId) }
    var selectedCategoryName by remember { mutableStateOf(record.categoryName) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val currentCategories = if (selectedType == 0) expenseCategories else incomeCategories

    // 切换类型时重置分类
    LaunchedEffect(selectedType) {
        val cats = if (selectedType == 0) expenseCategories else incomeCategories
        if (cats.isNotEmpty() && cats.none { it.id == selectedCategoryId }) {
            selectedCategoryId = cats.first().id
            selectedCategoryName = cats.first().name
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑记录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 收入/支出切换
                TabRow(selectedTabIndex = selectedType) {
                    Tab(
                        selected = selectedType == 0,
                        onClick = { selectedType = 0 },
                        text = { Text("支出") }
                    )
                    Tab(
                        selected = selectedType == 1,
                        onClick = { selectedType = 1 },
                        text = { Text("收入") }
                    )
                }

                // 金额输入
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = { Text("金额") },
                    prefix = { Text("¥") },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 分类选择
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        currentCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    selectedCategoryName = category.name
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // 备注输入
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        amountError = "请输入有效金额"
                        return@TextButton
                    }
                    onConfirm(
                        record.copy(
                            amount = amount,
                            type = selectedType,
                            categoryId = selectedCategoryId,
                            categoryName = selectedCategoryName,
                            note = note.ifBlank { null },
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
