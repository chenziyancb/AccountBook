package com.claw.accountbook.ui.screens.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claw.accountbook.data.local.dao.CategorySum
import com.claw.accountbook.data.local.dao.DailyTotal
import com.claw.accountbook.data.local.dao.MonthlyTotal
import com.claw.accountbook.data.local.entity.RecordEntity
import com.claw.accountbook.ui.theme.ExpenseColor
import com.claw.accountbook.ui.theme.IncomeColor
import com.claw.accountbook.viewmodel.RecordViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 统计屏幕 — 三级下钻：年 → 月 → 周
 */
@Composable
fun StatisticsScreen(
    viewModel: RecordViewModel = hiltViewModel()
) {
    val statsYear by viewModel.statsYear.collectAsStateWithLifecycle()
    val statsMonth by viewModel.statsMonth.collectAsStateWithLifecycle()
    val statsWeek by viewModel.statsWeek.collectAsStateWithLifecycle()
    val statsYearData by viewModel.statsYearData.collectAsStateWithLifecycle()
    val statsMonthData by viewModel.statsMonthData.collectAsStateWithLifecycle()
    val statsWeekData by viewModel.statsWeekData.collectAsStateWithLifecycle()
    val statsWeekRanges by viewModel.statsWeekRanges.collectAsStateWithLifecycle()
    val availableYears by viewModel.availableYears.collectAsStateWithLifecycle()

    // 当前视图层级：0=年视图，1=月视图，2=周视图
    val level = when {
        statsWeek >= 0 -> 2
        statsMonth > 0 -> 1
        else -> 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 标题行（含返回按钮）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (level > 0) {
                IconButton(onClick = {
                    when (level) {
                        2 -> viewModel.selectStatsWeek(-1)
                        1 -> viewModel.selectStatsYear(statsYear) // 重置月，回年视图
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
            val titleText = when (level) {
                0 -> "年度统计"
                1 -> "${statsYear}年 月度统计"
                2 -> {
                    val r = statsWeekRanges.getOrNull(statsWeek)
                    if (r != null) "${statsYear}年${statsMonth}月 第${statsWeek + 1}周"
                    else "${statsYear}年${statsMonth}月 第${statsWeek + 1}周"
                }
                else -> "数据统计"
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (level) {
            // ═══════════ 年视图 ═══════════
            0 -> {
                YearSelector(
                    year = statsYear,
                    onPrev = {
                        val idx = availableYears.indexOf(statsYear)
                        viewModel.selectStatsYear(if (idx > 0) availableYears[idx - 1] else statsYear - 1)
                    },
                    onNext = {
                        val idx = availableYears.indexOf(statsYear)
                        viewModel.selectStatsYear(
                            if (idx >= 0 && idx < availableYears.size - 1) availableYears[idx + 1]
                            else statsYear + 1
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OverviewCard(
                    label = "${statsYear}年",
                    income = statsYearData.totalIncome,
                    expense = statsYearData.totalExpense,
                    balance = statsYearData.totalIncome - statsYearData.totalExpense
                )
                Spacer(modifier = Modifier.height(16.dp))
                YearlyBarChart(
                    title = "${statsYear}年 月度支出",
                    monthlyTotals = statsYearData.monthlyTotals,
                    type = 0, barColor = ExpenseColor,
                    onMonthClick = { viewModel.selectStatsMonth(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                YearlyBarChart(
                    title = "${statsYear}年 月度收入",
                    monthlyTotals = statsYearData.monthlyTotals,
                    type = 1, barColor = IncomeColor,
                    onMonthClick = { viewModel.selectStatsMonth(it) }
                )
            }

            // ═══════════ 月视图 ═══════════
            1 -> {
                YearSelector(
                    year = statsYear,
                    onPrev = {
                        viewModel.selectStatsYear(statsYear - 1)
                        viewModel.selectStatsMonth(statsMonth)
                    },
                    onNext = {
                        viewModel.selectStatsYear(statsYear + 1)
                        viewModel.selectStatsMonth(statsMonth)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                MonthGrid(
                    selectedMonth = statsMonth,
                    onMonthClick = { viewModel.selectStatsMonth(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OverviewCard(
                    label = "${statsYear}年${statsMonth}月",
                    income = statsMonthData.income,
                    expense = statsMonthData.expense,
                    balance = statsMonthData.balance
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (statsWeekRanges.isNotEmpty()) {
                    WeekSummaryList(
                        weekRanges = statsWeekRanges,
                        selectedWeek = statsWeek,
                        onWeekClick = { viewModel.selectStatsWeek(it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                CategoryBarChart(
                    title = "${statsMonth}月 支出分类",
                    items = statsMonthData.expenseByCategory,
                    barColor = ExpenseColor,
                    emptyText = "暂无支出数据"
                )
                Spacer(modifier = Modifier.height(16.dp))
                CategoryBarChart(
                    title = "${statsMonth}月 收入分类",
                    items = statsMonthData.incomeByCategory,
                    barColor = IncomeColor,
                    emptyText = "暂无收入数据"
                )
            }

            // ═══════════ 周视图 ═══════════
            2 -> {
                val range = statsWeekRanges.getOrNull(statsWeek)
                if (statsWeekRanges.size > 1) {
                    WeekSelector(
                        weekIndex = statsWeek,
                        weekRanges = statsWeekRanges,
                        onPrev = { if (statsWeek > 0) viewModel.selectStatsWeek(statsWeek - 1) },
                        onNext = { if (statsWeek < statsWeekRanges.size - 1) viewModel.selectStatsWeek(statsWeek + 1) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OverviewCard(
                    label = "第${statsWeek + 1}周",
                    income = statsWeekData.totalIncome,
                    expense = statsWeekData.totalExpense,
                    balance = statsWeekData.totalIncome - statsWeekData.totalExpense
                )
                Spacer(modifier = Modifier.height(16.dp))
                DailyBarChart(
                    title = "每日支出",
                    dailyTotals = statsWeekData.dailyTotals,
                    type = 0,
                    barColor = ExpenseColor,
                    weekStart = range?.first ?: 0L,
                    weekEnd = range?.second ?: 0L
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (statsWeekData.weekRecords.isNotEmpty()) {
                    WeekRecordList(records = statsWeekData.weekRecords)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════
// 子组件
// ═══════════════════════════════════════════════════

/** 年份选择器 ← 2024 → */
@Composable
private fun YearSelector(year: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "上一年", tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = "${year}年",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "下一年", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** 12个月按钮网格（4列×3行） */
@Composable
private fun MonthGrid(selectedMonth: Int, onMonthClick: (Int) -> Unit) {
    val monthLabels = listOf("1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("选择月份", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            for (row in 0..2) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0..3) {
                        val month = row * 4 + col + 1
                        FilterChip(
                            selected = month == selectedMonth,
                            onClick = { onMonthClick(month) },
                            label = {
                                Text(
                                    text = monthLabels[month - 1],
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (row < 2) Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/** 周汇总列表（月内所有周按钮） */
@Composable
private fun WeekSummaryList(
    weekRanges: List<Pair<Long, Long>>,
    selectedWeek: Int,
    onWeekClick: (Int) -> Unit
) {
    val fmt = SimpleDateFormat("M/d", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("按周查看", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weekRanges.forEachIndexed { idx, (start, end) ->
                    FilterChip(
                        selected = idx == selectedWeek,
                        onClick = { onWeekClick(idx) },
                        label = {
                            Text(
                                text = "第${idx + 1}周\n${fmt.format(Date(start))}~${fmt.format(Date(end))}",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** 周选择器（← 第2周 →） */
@Composable
private fun WeekSelector(
    weekIndex: Int,
    weekRanges: List<Pair<Long, Long>>,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val fmt = SimpleDateFormat("M月d日", Locale.getDefault())
    val range = weekRanges.getOrNull(weekIndex)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, enabled = weekIndex > 0) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "上一周", tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = if (range != null)
                "第${weekIndex + 1}周 (${fmt.format(Date(range.first))}~${fmt.format(Date(range.second))})"
            else "第${weekIndex + 1}周",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(onClick = onNext, enabled = weekIndex < weekRanges.size - 1) {
            Icon(Icons.Default.ChevronRight, contentDescription = "下一周", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** 收支概览卡片 */
@Composable
private fun OverviewCard(label: String, income: Double, expense: Double, balance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OverviewItem("$label 支出", expense, ExpenseColor)
            OverviewItem("$label 收入", income, IncomeColor)
            OverviewItem("结余", balance, if (balance >= 0) IncomeColor else ExpenseColor)
        }
    }
}

@Composable
private fun OverviewItem(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "¥${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/** 分类进度条图表 */
@Composable
private fun CategoryBarChart(title: String, items: List<CategorySum>, barColor: Color, emptyText: String) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(items) { visible = true }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            if (items.isEmpty()) {
                Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val maxTotal = items.maxOf { it.total }.coerceAtLeast(0.01)
                items.forEach { cs ->
                    val fraction = (cs.total / maxTotal).toFloat().coerceIn(0f, 1f)
                    val anim by animateFloatAsState(
                        if (visible) fraction else 0f, tween(600),
                        label = "cat_bar_${cs.categoryId}"
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(cs.categoryName.ifBlank { "分类${cs.categoryId}" }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text("¥${String.format("%.2f", cs.total)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = barColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(barColor.copy(alpha = 0.15f))) {
                            Box(modifier = Modifier.fillMaxWidth(anim).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(barColor))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("合计", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("¥${String.format("%.2f", items.sumOf { it.total })}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = barColor)
                }
            }
        }
    }
}

/** 年统计月度柱状图（可点击进入月视图） */
@Composable
private fun YearlyBarChart(
    title: String,
    monthlyTotals: List<MonthlyTotal>,
    type: Int,
    barColor: Color,
    onMonthClick: ((Int) -> Unit)? = null
) {
    val monthMap = monthlyTotals.filter { it.type == type }.associateBy { it.month }
    val monthLabels = listOf("1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (onMonthClick != null) {
                Text("点击月份查看详情", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (monthMap.isEmpty()) {
                Text("暂无数据", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val maxTotal = monthMap.values.maxOfOrNull { it.total }?.coerceAtLeast(0.01) ?: 0.01
                (1..12).forEachIndexed { idx, month ->
                    val amount = monthMap[month]?.total ?: 0.0
                    val fraction = (amount / maxTotal).toFloat().coerceIn(0f, 1f)
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(monthlyTotals) { visible = true }
                    val anim by animateFloatAsState(
                        if (visible) fraction else 0f, tween(500 + idx * 40),
                        label = "year_bar_$month"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .then(if (onMonthClick != null && amount > 0) Modifier.clickable { onMonthClick(month) } else Modifier),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(monthLabels[idx], style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(36.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(barColor.copy(alpha = 0.15f))) {
                            Box(modifier = Modifier.fillMaxWidth(anim).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(barColor))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (amount > 0) "¥${String.format("%.0f", amount)}" else "-",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(56.dp),
                            color = if (amount > 0) barColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (amount > 0) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("全年合计", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("¥${String.format("%.2f", monthMap.values.sumOf { it.total })}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = barColor)
                }
            }
        }
    }
}

/** 每日柱状图（周视图） */
@Composable
private fun DailyBarChart(
    title: String,
    dailyTotals: List<DailyTotal>,
    type: Int,
    barColor: Color,
    weekStart: Long,
    weekEnd: Long
) {
    if (weekStart == 0L) return
    val days = remember(weekStart, weekEnd) {
        val cal = Calendar.getInstance().apply { timeInMillis = weekStart }
        val list = mutableListOf<Triple<Int, Int, Long>>()
        while (cal.timeInMillis <= weekEnd) {
            list.add(Triple(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.timeInMillis))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }
    val dayMap = dailyTotals.filter { it.type == type }.associateBy { it.day to it.month }
    val maxTotal = dayMap.values.maxOfOrNull { it.total }?.coerceAtLeast(0.01) ?: 0.01
    val dateFmt = SimpleDateFormat("M/d(E)", Locale.CHINESE)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            if (dayMap.isEmpty()) {
                Text("暂无数据", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                days.forEachIndexed { idx, (dayOfMonth, month, timeMs) ->
                    val amount = dayMap[dayOfMonth to month]?.total ?: 0.0
                    val fraction = (amount / maxTotal).toFloat().coerceIn(0f, 1f)
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(dailyTotals) { visible = true }
                    val anim by animateFloatAsState(
                        if (visible) fraction else 0f, tween(500 + idx * 60),
                        label = "daily_bar_$idx"
                    )
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(dateFmt.format(Date(timeMs)), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(barColor.copy(alpha = 0.15f))) {
                            Box(modifier = Modifier.fillMaxWidth(anim).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(barColor))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (amount > 0) "¥${String.format("%.0f", amount)}" else "-",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(56.dp),
                            color = if (amount > 0) barColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (amount > 0) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("合计", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("¥${String.format("%.2f", dayMap.values.sumOf { it.total })}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = barColor)
                }
            }
        }
    }
}

/** 本周每日明细列表 */
@Composable
private fun WeekRecordList(records: List<RecordEntity>) {
    val dateFmt = SimpleDateFormat("M月d日", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("本周明细（${records.size}笔）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            records.forEach { record ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(record.categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        val meta = buildString {
                            if (!record.note.isNullOrBlank()) append("${record.note} · ")
                            append(dateFmt.format(Date(record.date)))
                        }
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "${if (record.type == 1) "+" else "-"}¥${String.format("%.2f", record.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (record.type == 1) IncomeColor else ExpenseColor
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}
