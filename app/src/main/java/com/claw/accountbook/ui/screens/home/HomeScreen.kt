package com.claw.accountbook.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claw.accountbook.data.local.entity.AccountBookEntity
import com.claw.accountbook.data.local.entity.CategoryEntity
import com.claw.accountbook.data.local.entity.RecordEntity
import com.claw.accountbook.ui.theme.ExpenseColor
import com.claw.accountbook.ui.theme.IncomeColor
import com.claw.accountbook.viewmodel.AccountBookViewModel
import com.claw.accountbook.viewmodel.RecordViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 首页 - 显示记账记录列表，支持月份筛选、账本切换、记录编辑/删除
 */
@Composable
fun HomeScreen(
    viewModel: RecordViewModel = hiltViewModel(),
    accountBookViewModel: AccountBookViewModel = hiltViewModel()
) {
    val records by viewModel.allRecords.collectAsStateWithLifecycle()
    val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val accountBooks by accountBookViewModel.accountBooks.collectAsStateWithLifecycle()
    val selectedBookId by accountBookViewModel.selectedAccountBookId.collectAsStateWithLifecycle()
    val selectedBookName by accountBookViewModel.selectedAccountBookName.collectAsStateWithLifecycle()

    // 当账本切换时，同步到 RecordViewModel
    LaunchedEffect(selectedBookId) {
        viewModel.setCurrentAccountBook(selectedBookId)
    }

    // 用于显示编辑对话框的记录
    var editingRecord by remember { mutableStateOf<RecordEntity?>(null) }
    // 用于显示删除确认的记录
    var deletingRecord by remember { mutableStateOf<RecordEntity?>(null) }
    // 账本切换下拉菜单
    var showAccountBookMenu by remember { mutableStateOf(false) }
    // 新建账本对话框
    var showCreateBookDialog by remember { mutableStateOf(false) }

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
        // 账本切换条
        AccountBookSelector(
            selectedBookName = selectedBookName,
            accountBooks = accountBooks,
            selectedBookId = selectedBookId,
            menuExpanded = showAccountBookMenu,
            onMenuToggle = { showAccountBookMenu = it },
            onSelectAll = {
                accountBookViewModel.selectAllAccountBooks()
                showAccountBookMenu = false
            },
            onSelectBook = { book ->
                accountBookViewModel.selectAccountBook(book.id)
                showAccountBookMenu = false
            },
            onCreateNew = {
                showAccountBookMenu = false
                showCreateBookDialog = true
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

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

    // 新建账本对话框
    if (showCreateBookDialog) {
        CreateAccountBookDialog(
            onDismiss = { showCreateBookDialog = false },
            onConfirm = { name, desc ->
                accountBookViewModel.createAccountBook(name, desc)
                showCreateBookDialog = false
            }
        )
    }
}

/**
 * 账本选择器 — 顶部账本切换条
 */
@Composable
fun AccountBookSelector(
    selectedBookName: String,
    accountBooks: List<AccountBookEntity>,
    selectedBookId: Long,
    menuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onSelectBook: (AccountBookEntity) -> Unit,
    onCreateNew: () -> Unit
) {
    Box {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMenuToggle(!menuExpanded) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "账本",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedBookName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "切换账本",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { onMenuToggle(false) }
        ) {
            // "全部账本"选项
            DropdownMenuItem(
                text = {
                    Text(
                        text = "全部账本",
                        fontWeight = if (selectedBookId == -1L) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedBookId == -1L) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = onSelectAll
            )

            HorizontalDivider()

            // 各个账本
            accountBooks.forEach { book ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = book.name + if (book.isDefault) " ★" else "",
                            fontWeight = if (selectedBookId == book.id) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBookId == book.id) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = { onSelectBook(book) }
                )
            }

            HorizontalDivider()

            // 新建账本
            DropdownMenuItem(
                text = { Text("+ 新建账本", color = MaterialTheme.colorScheme.primary) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "新建账本",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = onCreateNew
            )
        }
    }
}

/**
 * 新建账本对话框
 */
@Composable
fun CreateAccountBookDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建账本") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("账本名称") },
                    placeholder = { Text("例如：家庭账本") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("账本名称不能为空") }
                    } else null
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@TextButton
                    }
                    onConfirm(name.trim(), description.takeIf { it.isNotBlank() })
                }
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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
 * 月度概览卡片 — 三列横排：支出 | 收入 | 结余，与预览 UI 保持一致
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
            // 三列：收入 | 支出（分隔线分隔）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 收入
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "收入",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "¥${String.format("%.2f", income)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = IncomeColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 分隔线
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )

                // 支出
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "支出",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "¥${String.format("%.2f", expense)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ExpenseColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 结余行
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "结余",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "¥${String.format("%.2f", balance)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 记录项 — 预览 UI 风格：左侧彩色圆角图标 + 分类名/备注/日期 + 右侧金额
 */
@Composable
fun RecordItem(
    record: RecordEntity,
    onEdit: (RecordEntity) -> Unit,
    onDelete: (RecordEntity) -> Unit
) {
    val dateFormat = SimpleDateFormat("M月d日", Locale.getDefault())
    var showMenu by remember { mutableStateOf(false) }

    // 根据分类名映射 emoji，与预览风格一致
    val (emoji, iconBg) = remember(record.categoryName, record.type) {
        if (record.type == 1) {
            // 收入
            when {
                record.categoryName.contains("工资") || record.categoryName.contains("薪") -> "💰" to Color(0xFFE8F5E9)
                record.categoryName.contains("兼职") || record.categoryName.contains("副业") -> "💼" to Color(0xFFF3E5F5)
                record.categoryName.contains("理财") || record.categoryName.contains("投资") -> "📈" to Color(0xFFE3F2FD)
                record.categoryName.contains("红包") || record.categoryName.contains("奖励") -> "🎁" to Color(0xFFFCE4EC)
                else -> "💵" to Color(0xFFE8F5E9)
            }
        } else {
            // 支出
            when {
                record.categoryName.contains("餐") || record.categoryName.contains("饮") || record.categoryName.contains("食") -> "🍜" to Color(0xFFFFF3E0)
                record.categoryName.contains("交通") || record.categoryName.contains("出行") -> "🚇" to Color(0xFFE3F2FD)
                record.categoryName.contains("购物") || record.categoryName.contains("衣") -> "🛍️" to Color(0xFFFCE4EC)
                record.categoryName.contains("娱乐") || record.categoryName.contains("游") -> "🎮" to Color(0xFFF3E5F5)
                record.categoryName.contains("住") || record.categoryName.contains("房") -> "🏠" to Color(0xFFE8F5E9)
                record.categoryName.contains("医") || record.categoryName.contains("药") -> "💊" to Color(0xFFFFEBEE)
                record.categoryName.contains("教") || record.categoryName.contains("书") -> "📚" to Color(0xFFE3F2FD)
                else -> "💳" to Color(0xFFF5F5F5)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMenu = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 彩色圆角图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, style = MaterialTheme.typography.titleMedium)
            }

            // 分类 + 备注 + 日期
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                val meta = buildString {
                    if (!record.note.isNullOrBlank()) append("${record.note} · ")
                    append(dateFormat.format(Date(record.date)))
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 金额
            Text(
                text = "${if (record.type == 1) "+" else "-"}¥${String.format("%.2f", record.amount)}",
                style = MaterialTheme.typography.titleSmall,
                color = if (record.type == 1) IncomeColor else ExpenseColor,
                fontWeight = FontWeight.Bold
            )
        }

        // 长按操作菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("编辑") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "编辑") },
                onClick = {
                    showMenu = false
                    onEdit(record)
                }
            )
            DropdownMenuItem(
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
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
