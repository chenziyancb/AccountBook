package com.claw.accountbook.ui.screens.accountbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claw.accountbook.data.local.entity.AccountBookEntity
import com.claw.accountbook.viewmodel.AccountBookViewModel

/**
 * 账本管理页面 — 展示所有账本，支持新建、编辑、删除、设为默认
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBooksScreen(
    viewModel: AccountBookViewModel = hiltViewModel()
) {
    val accountBooks by viewModel.accountBooks.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingBook by remember { mutableStateOf<AccountBookEntity?>(null) }
    var deletingBook by remember { mutableStateOf<AccountBookEntity?>(null) }

    // 消息提示
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建账本")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "账本管理",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "共 ${accountBooks.size} 个账本",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (accountBooks.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无账本",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右下角 + 新建第一个账本",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accountBooks, key = { it.id }) { book ->
                        AccountBookCard(
                            accountBook = book,
                            onEdit = { editingBook = it },
                            onDelete = { deletingBook = it },
                            onSetDefault = { viewModel.setDefaultAccountBook(it.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) } // FAB 间距
                }
            }
        }
    }

    // 新建账本对话框
    if (showCreateDialog) {
        AccountBookFormDialog(
            title = "新建账本",
            initialName = "",
            initialDesc = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc ->
                viewModel.createAccountBook(name, desc)
                showCreateDialog = false
            }
        )
    }

    // 编辑账本对话框
    editingBook?.let { book ->
        AccountBookFormDialog(
            title = "编辑账本",
            initialName = book.name,
            initialDesc = book.description ?: "",
            onDismiss = { editingBook = null },
            onConfirm = { name, _ ->
                viewModel.renameAccountBook(book, name)
                editingBook = null
            }
        )
    }

    // 删除确认对话框
    deletingBook?.let { book ->
        AlertDialog(
            onDismissRequest = { deletingBook = null },
            title = { Text("删除账本") },
            text = {
                Text(
                    "确定要删除账本「${book.name}」吗？\n" +
                    if (book.isDefault) "⚠️ 这是默认账本，删除后将切换到其他账本。" else "该账本下的所有记录也将被删除。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccountBook(book)
                        deletingBook = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingBook = null }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 账本卡片 — 展示单个账本信息及操作菜单
 */
@Composable
fun AccountBookCard(
    accountBook: AccountBookEntity,
    onEdit: (AccountBookEntity) -> Unit,
    onDelete: (AccountBookEntity) -> Unit,
    onSetDefault: (AccountBookEntity) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { menuExpanded = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：图标 + 名称/描述
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 账本图标
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (accountBook.isDefault)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = if (accountBook.isDefault)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = accountBook.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (accountBook.isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "默认",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (!accountBook.description.isNullOrBlank()) {
                        Text(
                            text = accountBook.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 右侧：更多操作按钮
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多操作"
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    // 编辑
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit(accountBook)
                        }
                    )
                    // 设为默认
                    if (!accountBook.isDefault) {
                        DropdownMenuItem(
                            text = { Text("设为默认") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSetDefault(accountBook)
                            }
                        )
                    }
                    // 删除
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete(accountBook)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 账本表单对话框 — 用于新建/编辑账本
 */
@Composable
fun AccountBookFormDialog(
    title: String,
    initialName: String,
    initialDesc: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDesc) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("账本名称") },
                    placeholder = { Text("例如：家庭账本、旅行账本") },
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
                    placeholder = { Text("简短描述这个账本的用途") },
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
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
