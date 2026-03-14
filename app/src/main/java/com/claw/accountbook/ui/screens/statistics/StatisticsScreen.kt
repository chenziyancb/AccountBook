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
import com.claw.accountbook.ui.theme.ExpenseColor
import com.claw.accountbook.ui.theme.IncomeColor
import com.claw.accountbook.viewmodel.RecordViewModel
import com.claw.accountbook.data.local.dao.CategorySum

/**
 * 统计屏幕 - 带分类可视化图表
 */
@Composable
fun StatisticsScreen(
    viewModel: RecordViewModel = hiltViewModel()
) {
    val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()

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

        // 月度收支概览卡片
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
                    label = "本月支出",
                    amount = monthlyStats.expense,
                    color = ExpenseColor
                )
                OverviewItem(
                    label = "本月收入",
                    amount = monthlyStats.income,
                    color = IncomeColor
                )
                OverviewItem(
                    label = "结余",
                    amount = monthlyStats.balance,
                    color = if (monthlyStats.balance >= 0) IncomeColor else ExpenseColor
                )
            }
        }

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

                        // 进度条背景
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(barColor.copy(alpha = 0.15f))
                        ) {
                            // 进度条前景（带动画）
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

                // 汇总行
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
