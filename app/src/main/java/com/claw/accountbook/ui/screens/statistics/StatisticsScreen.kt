package com.claw.accountbook.ui.screens.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.claw.accountbook.data.local.dao.CategorySum
import com.claw.accountbook.data.local.dao.DailyTotal
import com.claw.accountbook.data.local.dao.MonthlyTotal
import com.claw.accountbook.ui.theme.ExpenseColor
import com.claw.accountbook.ui.theme.IncomeColor
import com.claw.accountbook.viewmodel.RecordViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 统计屏幕 - 支持周/月/年维度切换
 */
@Composable
fun StatisticsScreen(
    viewModel: RecordViewModel = hiltViewModel()
) {
    val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val weeklyStats by viewModel.weeklyStats.collectAsStateWithLifecycle()
    val yearlyStats by viewModel.yearlyStats.collectAsStateWithLifecycle()

    // 当前选中的统计维度：0=月, 1=周, 2=年
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "数据统计",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 维度切换 Tab
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("本月") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("本周") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("本年") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> {
                // ==================== 月统计 ====================
                // 月度收支概览卡片
                OverviewCard(
                    label = "本月",
                    income = monthlyStats.income,
                    expense = monthlyStats.expense,
                    balance = monthlyStats.balance
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 支出分类图表
                CategoryBarChart(
                    title = "本月支出分类",
                    items = monthlyStats.expenseByCategory,
                    barColor = ExpenseColor,
                    emptyText = "暂无支出数据"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 收入分类图表
                CategoryBarChart(
                    title = "本月收入分类",
                    items = monthlyStats.incomeByCategory,
                    barColor = IncomeColor,
                    emptyText = "暂无收入数据"
                )
            }

            1 -> {
                // ==================== 周统计 ====================
                val weekLabel = remember(weeklyStats.startOfWeek, weeklyStats.endOfWeek) {
                    val fmt = SimpleDateFormat("MM/dd", Locale.getDefault())
                    if (weeklyStats.startOfWeek == 0L) "本周"
                    else "${fmt.format(Date(weeklyStats.startOfWeek))} - ${fmt.format(Date(weeklyStats.endOfWeek))}"
                }

                OverviewCard(
                    label = weekLabel,
                    income = weeklyStats.totalIncome,
                    expense = weeklyStats.totalExpense,
                    balance = weeklyStats.totalIncome - weeklyStats.totalExpense
                )

                Spacer(modifier = Modifier.height(16.dp))

                WeeklyBarChart(
                    title = "本周每日支出",
                    dailyTotals = weeklyStats.dailyTotals,
                    type = 0,  // 支出
                    barColor = ExpenseColor,
                    startOfWeek = weeklyStats.startOfWeek
                )

                Spacer(modifier = Modifier.height(16.dp))

                WeeklyBarChart(
                    title = "本周每日收入",
                    dailyTotals = weeklyStats.dailyTotals,
                    type = 1,  // 收入
                    barColor = IncomeColor,
                    startOfWeek = weeklyStats.startOfWeek
                )
            }

            2 -> {
                // ==================== 年统计 ====================
                OverviewCard(
                    label = "${yearlyStats.year}年",
                    income = yearlyStats.totalIncome,
                    expense = yearlyStats.totalExpense,
                    balance = yearlyStats.totalIncome - yearlyStats.totalExpense
                )

                Spacer(modifier = Modifier.height(16.dp))

                YearlyBarChart(
                    title = "${yearlyStats.year}年月度支出",
                    monthlyTotals = yearlyStats.monthlyTotals,
                    type = 0,  // 支出
                    barColor = ExpenseColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                YearlyBarChart(
                    title = "${yearlyStats.year}年月度收入",
                    monthlyTotals = yearlyStats.monthlyTotals,
                    type = 1,  // 收入
                    barColor = IncomeColor
                )
            }
        }
    }
}

/**
 * 收支概览卡片
 */
@Composable
private fun OverviewCard(
    label: String,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OverviewItem(
                label = "$label 支出",
                amount = expense,
                color = ExpenseColor
            )
            OverviewItem(
                label = "$label 收入",
                amount = income,
                color = IncomeColor
            )
            OverviewItem(
                label = "结余",
                amount = balance,
                color = if (balance >= 0) IncomeColor else ExpenseColor
            )
        }
    }
}

@Composable
private fun OverviewItem(
    label: String,
    amount: Double,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "¥${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 分类横向进度条图表（月统计用）
 */
@Composable
private fun CategoryBarChart(
    title: String,
    items: List<CategorySum>,
    barColor: Color,
    emptyText: String
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(items) { visible = true }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (items.isEmpty()) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxTotal = items.maxOf { it.total }.coerceAtLeast(0.01)

                items.forEach { categorySum ->
                    val fraction = (categorySum.total / maxTotal).toFloat().coerceIn(0f, 1f)
                    val animatedFraction by animateFloatAsState(
                        targetValue = if (visible) fraction else 0f,
                        animationSpec = tween(durationMillis = 600),
                        label = "bar_anim_${categorySum.categoryId}"
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = categorySum.categoryName.ifBlank { "分类${categorySum.categoryId}" },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "¥${String.format("%.2f", categorySum.total)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = barColor
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(barColor.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(barColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "合计",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "¥${String.format("%.2f", items.sumOf { it.total })}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                }
            }
        }
    }
}

/**
 * 周统计柱状图 — 显示周一到周日每日收支
 */
@Composable
private fun WeeklyBarChart(
    title: String,
    dailyTotals: List<DailyTotal>,
    type: Int,
    barColor: Color,
    startOfWeek: Long
) {
    // 过滤当前类型数据，建立 day → total 映射
    val dayMap = dailyTotals.filter { it.type == type }.associateBy { it.day }

    // 生成本周7天日期（周一~周日）
    val days = remember(startOfWeek) {
        if (startOfWeek == 0L) return@remember emptyList()
        val cal = Calendar.getInstance().apply { timeInMillis = startOfWeek }
        (0..6).map { offset ->
            val c = Calendar.getInstance().apply {
                timeInMillis = startOfWeek
                add(Calendar.DAY_OF_MONTH, offset)
            }
            Triple(
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.MONTH) + 1,
                c
            )
        }
    }

    val weekDayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (days.isEmpty() || dayMap.isEmpty()) {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxTotal = dayMap.values.maxOfOrNull { it.total }?.coerceAtLeast(0.01) ?: 0.01

                days.forEachIndexed { index, (dayOfMonth, month, _) ->
                    val entry = dayMap[dayOfMonth]
                    val amount = entry?.total ?: 0.0
                    val fraction = (amount / maxTotal).toFloat().coerceIn(0f, 1f)

                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(dailyTotals) { visible = true }
                    val animatedFraction by animateFloatAsState(
                        targetValue = if (visible) fraction else 0f,
                        animationSpec = tween(durationMillis = 500 + index * 60),
                        label = "week_bar_$index"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = weekDayLabels[index],
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(36.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(barColor.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(barColor)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (amount > 0) "¥${String.format("%.0f", amount)}" else "-",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "本周合计", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "¥${String.format("%.2f", dayMap.values.sumOf { it.total })}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                }
            }
        }
    }
}

/**
 * 年统计柱状图 — 显示 1~12 月每月收支
 */
@Composable
private fun YearlyBarChart(
    title: String,
    monthlyTotals: List<MonthlyTotal>,
    type: Int,
    barColor: Color
) {
    // 过滤当前类型，建立 month(1-12) → total 映射
    val monthMap = monthlyTotals.filter { it.type == type }.associateBy { it.month }
    val monthLabels = listOf("1月", "2月", "3月", "4月", "5月", "6月",
                             "7月", "8月", "9月", "10月", "11月", "12月")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (monthMap.isEmpty()) {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxTotal = monthMap.values.maxOfOrNull { it.total }?.coerceAtLeast(0.01) ?: 0.01

                (1..12).forEachIndexed { index, month ->
                    val amount = monthMap[month]?.total ?: 0.0
                    val fraction = (amount / maxTotal).toFloat().coerceIn(0f, 1f)

                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(monthlyTotals) { visible = true }
                    val animatedFraction by animateFloatAsState(
                        targetValue = if (visible) fraction else 0f,
                        animationSpec = tween(durationMillis = 500 + index * 40),
                        label = "year_bar_$month"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = monthLabels[index],
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(36.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(barColor.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(barColor)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (amount > 0) "¥${String.format("%.0f", amount)}" else "-",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "全年合计", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "¥${String.format("%.2f", monthMap.values.sumOf { it.total })}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                }
            }
        }
    }
}
