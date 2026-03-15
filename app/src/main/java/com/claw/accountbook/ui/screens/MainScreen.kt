package com.claw.accountbook.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.claw.accountbook.ui.screens.accountbook.AccountBooksScreen
import com.claw.accountbook.ui.screens.home.HomeScreen
import com.claw.accountbook.ui.screens.settings.SettingsScreen
import com.claw.accountbook.ui.screens.statistics.StatisticsScreen
import com.claw.accountbook.viewmodel.AccountBookViewModel
import com.claw.accountbook.viewmodel.RecordViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 导航项目
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Statistics : Screen("statistics", "统计", Icons.Default.PieChart)
    object AccountBooks : Screen("account_books", "账本", Icons.Default.Book)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Statistics,
    Screen.AccountBooks,
    Screen.Settings
)

/**
 * 主屏幕 - 包含底部导航
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit = {}) {
    val navController = rememberNavController()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加记录")
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen()
            }
            composable(Screen.AccountBooks.route) {
                AccountBooksScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onLogout = onLogout)
            }
        }
    }

    if (showAddDialog) {
        AddRecordDialog(onDismiss = { showAddDialog = false })
    }
}

/**
 * 添加记录对话框 — 新记录自动关联当前选中的账本，支持日期选择
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordDialog(
    onDismiss: () -> Unit,
    viewModel: RecordViewModel = hiltViewModel(),
    accountBookViewModel: AccountBookViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var amountError by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    // 日期选择状态（默认今天）
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }

    // 当前选中账本
    val selectedBookId by accountBookViewModel.selectedAccountBookId.collectAsStateWithLifecycle()
    val selectedBookName by accountBookViewModel.selectedAccountBookName.collectAsStateWithLifecycle()

    // 根据 Tab 切换获取对应分类列表
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()
    val categories = if (selectedTab == 0) expenseCategories else incomeCategories

    // 当前选中分类
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    // Tab 切换时重置分类选择
    LaunchedEffect(selectedTab) {
        selectedCategoryIndex = 0
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 保存成功后关闭对话框
    LaunchedEffect(uiState.message) {
        if (uiState.message == "记录添加成功") {
            viewModel.clearMessage()
            onDismiss()
        }
    }

    // 日期选择器对话框
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate = it }
                        showDatePickerDialog = false
                    }
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加记录") },
        text = {
            Column {
                // 当前账本提示
                if (selectedBookId != -1L) {
                    Text(
                        text = "账本：$selectedBookName",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 收支类型 Tab
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("支出") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("收入") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 金额输入
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        amountError = false
                    },
                    label = { Text("金额") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("请输入有效金额") }
                    } else null
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 分类选择下拉框
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = categories.getOrNull(selectedCategoryIndex)?.name ?: "请选择分类",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        categories.forEachIndexed { index, category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryIndex = index
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 日期选择
                OutlinedTextField(
                    value = dateFormatter.format(Date(selectedDate)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("日期") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "选择日期",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePickerDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 备注输入
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // 错误提示
                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue == null || amountValue <= 0) {
                        amountError = true
                        return@TextButton
                    }
                    val category = categories.getOrNull(selectedCategoryIndex)
                    val categoryId = category?.id ?: 0L
                    val categoryName = category?.name ?: ""

                    // 确定目标账本ID（当前选中账本，"全部"时用默认1L）
                    val targetBookId = if (selectedBookId == -1L) 1L else selectedBookId

                    // 设置记录类型并保存，关联当前账本和选择的日期
                    viewModel.setRecordType(selectedTab)
                    viewModel.addRecord(
                        amount = amountValue,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        note = note.takeIf { it.isNotBlank() },
                        accountBookId = targetBookId,
                        date = selectedDate
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
