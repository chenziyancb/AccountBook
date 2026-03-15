package com.claw.accountbook.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs

/**
 * 首页 — 今日/本周/本月汇总卡片 + 每日消费折线图 + 收支明细列表
 */
@Composable
fun HomeScreen(
    viewModel: RecordViewModel = hiltViewModel(),
    accountBookViewModel: AccountBookViewModel = hiltViewModel()
) {
    val records by viewModel.allRecords.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 首页汇总数据
    val todayStats by viewModel.todayStats.collectAsStateWithLifecycle()
    val thisWeekStats by viewModel.thisWeekStats.collectAsStateWithLifecycle()
    val thisMonthStats by viewModel.thisMonthStats.collectAsStateWithLifecycle()
    val monthDailyExpense by viewModel.monthDailyExpense.collectAsStateWithLifecycle()

    val accountBooks by accountBookViewModel.accountBooks.collectAsStateWithLifecycle()
    val selectedBookId by accountBookViewModel.selectedAccountBookId.collectAsStateWithLifecycle()
    val selectedBookName by accountBookViewModel.selectedAccountBookName.collectAsStateWithLifecycle()

    // 当账本切换时，同步到 RecordViewModel
    LaunchedEffect(selectedBookId) {
        viewModel.setCurrentAccountBook(selectedBookId)
    }

    var editingRecord by remember { mutableStateOf<RecordEntity?>(null) }
    var deletingRecord by remember { mutableStateOf<RecordEntity?>(null) }
    var showAccountBookMenu by remember { mutableStateOf(false) }
    var showCreateBookDialog by remember { mutableStateOf(false) }
    // 是否展开记录列表
    var showRecordList by remember { mutableStateOf(false) }

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

    val monthLabel = remember(selectedDate) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
    }

    LaunchedEffect(uiState.message) {
        if (uiState.message != null) viewModel.clearMessage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 账本选择器
        AccountBookSelector(
            selectedBookName = selectedBookName,
            accountBooks = accountBooks,
            selectedBookId = selectedBookId,
            menuExpanded = showAccountBookMenu,
            onMenuToggle = { showAccountBookMenu = it },
            onSelectAll = { accountBookViewModel.selectAllAccountBooks(); showAccountBookMenu = false },
            onSelectBook = { accountBookViewModel.selectAccountBook(it.id); showAccountBookMenu = false },
            onCreateNew = { showAccountBookMenu = false; showCreateBookDialog = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── 三行汇总卡片 ──
        Text(
            text = "收支汇总",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        SummaryRow(
            label = "今天",
            expense = todayStats.expense,
            income = todayStats.income,
            onClick = {
                // 定位到今日所在月份并展开列表
                viewModel.setSelectedDate(System.currentTimeMillis())
                showRecordList = true
            }
        )
        Spacer(modifier = Modifier.height(6.dp))
        SummaryRow(
            label = "本周",
            expense = thisWeekStats.expense,
            income = thisWeekStats.income,
            onClick = {
                viewModel.setSelectedDate(System.currentTimeMillis())
                showRecordList = true
            }
        )
        Spacer(modifier = Modifier.height(6.dp))
        SummaryRow(
            label = "本月",
            expense = thisMonthStats.expense,
            income = thisMonthStats.income,
            onClick = {
                viewModel.setSelectedDate(System.currentTimeMillis())
                showRecordList = true
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── 每日消费折线图 ──
        DailyExpenseLineChart(
            title = "本月每日消费",
            dailyExpense = monthDailyExpense,
            selectedDate = selectedDate
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── 明细记录（可折叠）──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showRecordList = !showRecordList },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 月份导航
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
        }

        if (showRecordList) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "收支明细（${filteredRecords.size}笔）",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (filteredRecords.isEmpty()) {
                Text(
                    text = "本月暂无记录，点击 + 添加第一笔",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                // 使用 Column 代替 LazyColumn（因外层已有滚动）
                filteredRecords.forEach { record ->
                    RecordItem(
                        record = record,
                        onEdit = { editingRecord = it },
                        onDelete = { deletingRecord = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
            onConfirm = { viewModel.updateRecord(it); editingRecord = null }
        )
    }

    // 删除确认对话框
    deletingRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { deletingRecord = null },
            title = { Text("删除记录") },
            text = { Text("确定要删除这笔${if (record.type == 1) "收入" else "支出"}记录吗？\n金额：¥${String.format("%.2f", record.amount)}") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRecord(record); deletingRecord = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deletingRecord = null }) { Text("取消") } }
        )
    }

    if (showCreateBookDialog) {
        CreateAccountBookDialog(
            onDismiss = { showCreateBookDialog = false },
            onConfirm = { name, desc -> accountBookViewModel.createAccountBook(name, desc); showCreateBookDialog = false }
        )
    }
}

// ═══════════════════════════════════════════════════
// 汇总行
// ═══════════════════════════════════════════════════

@Composable
private fun SummaryRow(
    label: String,
    expense: Double,
    income: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 标签
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(48.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.weight(1f))

            // 支出
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
                Text("支出", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "¥${String.format("%.2f", expense)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseColor
                )
            }

            // 分隔
            Box(modifier = Modifier.width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))

            // 收入
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
                Text("收入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "¥${String.format("%.2f", income)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = IncomeColor
                )
            }

            // 箭头提示
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "查看详情",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════
// 折线图
// ═══════════════════════════════════════════════════

@Composable
private fun DailyExpenseLineChart(
    title: String,
    dailyExpense: Map<Int, Double>,   // day(1-31) -> amount
    selectedDate: Long
) {
    // 当月总天数
    val daysInMonth = remember(selectedDate) {
        Calendar.getInstance().apply { timeInMillis = selectedDate }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // 点击时显示的 tooltip
    var tooltipDay by remember { mutableStateOf<Int?>(null) }

    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            if (dailyExpense.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("暂无消费数据", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            val maxAmount = (dailyExpense.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                // 点击检测
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dailyExpense) {
                            detectTapGestures { tapOffset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val padLeft = 0f
                                val padRight = 0f
                                val padTop = 12f
                                val padBottom = 20f
                                val chartW = w - padLeft - padRight
                                val chartH = h - padTop - padBottom
                                val colW = chartW / daysInMonth.toFloat()

                                // 找最近点
                                var closestDay: Int? = null
                                var minDist = Float.MAX_VALUE
                                for (day in 1..daysInMonth) {
                                    val x = padLeft + (day - 0.5f) * colW
                                    val amount = dailyExpense[day] ?: 0.0
                                    val y = padTop + chartH - (amount / maxAmount).toFloat() * chartH
                                    val dist = abs(tapOffset.x - x) + abs(tapOffset.y - y)
                                    if (dist < minDist) {
                                        minDist = dist
                                        closestDay = day
                                    }
                                }
                                tooltipDay = if (minDist < 60f) closestDay else null
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val padTop = 12f
                    val padBottom = 20f
                    val chartH = h - padTop - padBottom
                    val colW = w / daysInMonth.toFloat()

                    // 背景网格线（3条）
                    for (i in 1..3) {
                        val y = padTop + chartH * (1f - i / 3f)
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.8f)
                    }

                    // 计算所有点坐标
                    val points = (1..daysInMonth).map { day ->
                        val x = (day - 0.5f) * colW
                        val amount = dailyExpense[day] ?: 0.0
                        val y = padTop + chartH - (amount / maxAmount).toFloat() * chartH
                        Offset(x, y)
                    }

                    // 绘制折线路径
                    val path = Path()
                    points.forEachIndexed { idx, pt ->
                        if (idx == 0) path.moveTo(pt.x, pt.y)
                        else {
                            // 贝塞尔曲线平滑
                            val prev = points[idx - 1]
                            val cx = (prev.x + pt.x) / 2f
                            path.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                        }
                    }
                    drawPath(path, lineColor, style = Stroke(width = 2.5f))

                    // 绘制数据点
                    points.forEachIndexed { idx, pt ->
                        val day = idx + 1
                        val hasData = (dailyExpense[day] ?: 0.0) > 0
                        if (hasData) {
                            drawCircle(Color.White, radius = 5f, center = pt)
                            drawCircle(dotColor, radius = 4f, center = pt, style = Stroke(width = 2f))
                        }
                    }

                    // 高亮选中点
                    val td = tooltipDay
                    if (td != null && td in 1..daysInMonth) {
                        val pt = points[td - 1]
                        drawCircle(dotColor.copy(alpha = 0.3f), radius = 10f, center = pt)
                        drawCircle(dotColor, radius = 5f, center = pt)
                    }
                }

                // Tooltip
                val td = tooltipDay
                if (td != null) {
                    val amount = dailyExpense[td] ?: 0.0
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    val month = cal.get(Calendar.MONTH) + 1
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 0.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shape = RoundedCornerShape(6.dp),
                        tonalElevation = 4.dp
                    ) {
                        Text(
                            text = "${month}月${td}日  ¥${String.format("%.2f", amount)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // X轴月份标签（首/中/末）
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                Text("1日", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.weight(1f))
                Text("${daysInMonth / 2}日", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.weight(1f))
                Text("${daysInMonth}日", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// 以下组件保持不变
// ═══════════════════════════════════════════════════

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
                    Icon(Icons.Default.Book, contentDescription = "账本", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedBookName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = "切换账本", tint = MaterialTheme.colorScheme.primary)
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuToggle(false) }) {
            DropdownMenuItem(
                text = { Text("全部账本", fontWeight = if (selectedBookId == -1L) FontWeight.Bold else FontWeight.Normal, color = if (selectedBookId == -1L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                onClick = onSelectAll
            )
            HorizontalDivider()
            accountBooks.forEach { book ->
                DropdownMenuItem(
                    text = { Text(book.name + if (book.isDefault) " ★" else "", fontWeight = if (selectedBookId == book.id) FontWeight.Bold else FontWeight.Normal, color = if (selectedBookId == book.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                    onClick = { onSelectBook(book) }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("+ 新建账本", color = MaterialTheme.colorScheme.primary) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = "新建账本", tint = MaterialTheme.colorScheme.primary) },
                onClick = onCreateNew
            )
        }
    }
}

/**
 * 新建账本对话框
 */
@Composable
fun CreateAccountBookDialog(onDismiss: () -> Unit, onConfirm: (String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建账本") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; nameError = false },
                    label = { Text("账本名称") }, placeholder = { Text("例如：家庭账本") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    isError = nameError, supportingText = if (nameError) { { Text("账本名称不能为空") } } else null
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth(), maxLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isBlank()) { nameError = true; return@TextButton }; onConfirm(name.trim(), description.takeIf { it.isNotBlank() }) }) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/**
 * 月份导航条
 */
@Composable
fun MonthNavigator(monthLabel: String, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "上月", tint = MaterialTheme.colorScheme.primary)
        }
        Text(monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = "下月", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * 月度概览卡片（保留兼容）
 */
@Composable
fun MonthlyOverviewCard(income: Double, expense: Double, balance: Double) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("收入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("¥${String.format("%.2f", income)}", style = MaterialTheme.typography.titleMedium, color = IncomeColor, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("支出", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("¥${String.format("%.2f", expense)}", style = MaterialTheme.typography.titleMedium, color = ExpenseColor, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("结余", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(2.dp))
                Text("¥${String.format("%.2f", balance)}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 记录项
 */
@Composable
fun RecordItem(record: RecordEntity, onEdit: (RecordEntity) -> Unit, onDelete: (RecordEntity) -> Unit) {
    val dateFormat = SimpleDateFormat("M月d日", Locale.getDefault())
    var showMenu by remember { mutableStateOf(false) }
    val (emoji, iconBg) = remember(record.categoryName, record.type) {
        if (record.type == 1) {
            when {
                record.categoryName.contains("工资") || record.categoryName.contains("薪") -> "💰" to Color(0xFFE8F5E9)
                record.categoryName.contains("兼职") || record.categoryName.contains("副业") -> "💼" to Color(0xFFF3E5F5)
                record.categoryName.contains("理财") || record.categoryName.contains("投资") -> "📈" to Color(0xFFE3F2FD)
                record.categoryName.contains("红包") || record.categoryName.contains("奖励") -> "🎁" to Color(0xFFFCE4EC)
                else -> "💵" to Color(0xFFE8F5E9)
            }
        } else {
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
        modifier = Modifier.fillMaxWidth().clickable { showMenu = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Text(text = emoji, style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(record.categoryName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                val meta = buildString {
                    if (!record.note.isNullOrBlank()) append("${record.note} · ")
                    append(dateFormat.format(Date(record.date)))
                }
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "${if (record.type == 1) "+" else "-"}¥${String.format("%.2f", record.amount)}",
                style = MaterialTheme.typography.titleSmall,
                color = if (record.type == 1) IncomeColor else ExpenseColor,
                fontWeight = FontWeight.Bold
            )
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("编辑") }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "编辑") }, onClick = { showMenu = false; onEdit(record) })
            DropdownMenuItem(
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDelete(record) }
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
                TabRow(selectedTabIndex = selectedType) {
                    Tab(selected = selectedType == 0, onClick = { selectedType = 0 }, text = { Text("支出") })
                    Tab(selected = selectedType == 1, onClick = { selectedType = 1 }, text = { Text("收入") })
                }
                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = it; amountError = null },
                    label = { Text("金额") }, prefix = { Text("¥") },
                    isError = amountError != null, supportingText = amountError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                    OutlinedTextField(
                        value = selectedCategoryName, onValueChange = {}, readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        currentCategories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name) }, onClick = { selectedCategoryId = cat.id; selectedCategoryName = cat.name; categoryExpanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth(), maxLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount <= 0) { amountError = "请输入有效金额"; return@TextButton }
                onConfirm(record.copy(amount = amount, type = selectedType, categoryId = selectedCategoryId, categoryName = selectedCategoryName, note = note.ifBlank { null }, updatedAt = System.currentTimeMillis()))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
