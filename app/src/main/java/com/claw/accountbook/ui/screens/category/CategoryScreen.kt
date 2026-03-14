package com.claw.accountbook.ui.screens.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claw.accountbook.data.local.entity.CategoryEntity
import com.claw.accountbook.ui.theme.ExpenseColor
import com.claw.accountbook.ui.theme.IncomeColor
import com.claw.accountbook.viewmodel.RecordViewModel

/**
 * 分类管理屏幕
 */
@Composable
fun CategoryScreen(
    viewModel: RecordViewModel = hiltViewModel()
) {
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "分类管理",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("支出分类") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("收入分类") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val categories = if (selectedTab == 0) expenseCategories else incomeCategories

        if (categories.isEmpty()) {
            Text(
                text = "暂无分类数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    CategoryItem(category = category)
                }
            }
        }
    }
}

/**
 * 分类项
 */
@Composable
fun CategoryItem(category: CategoryEntity) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (category.type == 0) "支出" else "收入",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (category.type == 0) ExpenseColor else IncomeColor
                )
            }

            if (category.isDefault) {
                Text(
                    text = "默认",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
