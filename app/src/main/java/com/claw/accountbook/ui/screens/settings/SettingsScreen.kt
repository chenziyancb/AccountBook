package com.claw.accountbook.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.claw.accountbook.viewmodel.UserViewModel

/**
 * 设置屏幕 — 支持修改用户名、修改密码、找回密码
 */
@Composable
fun SettingsScreen(
    onLogout: () -> Unit = {},
    viewModel: UserViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // 对话框显示状态
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // 消息提示 Snackbar
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 用户信息卡片（可点击编辑）──
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.username ?: "未登录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (currentUser?.email != null) {
                            Text(
                                text = currentUser!!.email!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = "未绑定邮箱",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑个人信息",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { showEditUsernameDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 账号安全 ──
            Text(
                text = "账号安全",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("修改用户名") },
                        supportingContent = {
                            Text(currentUser?.username ?: "—")
                        },
                        leadingContent = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showEditUsernameDialog = true }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("修改密码") },
                        supportingContent = { Text("定期修改密码保障账号安全") },
                        leadingContent = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showChangePasswordDialog = true }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("找回密码") },
                        supportingContent = { Text("通过绑定邮箱重置密码") },
                        leadingContent = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showForgotPasswordDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 关于 ──
            Text(
                text = "关于",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("关于记账本") },
                        supportingContent = { Text("v1.0.0") },
                        leadingContent = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showAboutDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 退出登录 ──
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "退出登录",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable { showLogoutDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 底部应用信息 ──
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "记账本",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "一个简单好用的记账应用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "© 2024 Claw Studio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // ══════════════ 修改用户名对话框 ══════════════
    if (showEditUsernameDialog) {
        var newUsername by remember { mutableStateOf(currentUser?.username ?: "") }
        AlertDialog(
            onDismissRequest = { showEditUsernameDialog = false },
            title = { Text("修改用户名") },
            text = {
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("新用户名") },
                    supportingText = { Text("3-20 个字符") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateUsername(newUsername.trim())
                        showEditUsernameDialog = false
                    },
                    enabled = newUsername.trim().length in 3..20
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUsernameDialog = false }) { Text("取消") }
            }
        )
    }

    // ══════════════ 修改密码对话框 ══════════════
    if (showChangePasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var oldPwdVisible by remember { mutableStateOf(false) }
        var newPwdVisible by remember { mutableStateOf(false) }
        var confirmError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("修改密码") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("原密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (oldPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { oldPwdVisible = !oldPwdVisible }) {
                                Icon(
                                    if (oldPwdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            confirmError = false
                        },
                        label = { Text("新密码") },
                        supportingText = { Text("至少 6 个字符") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (newPwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { newPwdVisible = !newPwdVisible }) {
                                Icon(
                                    if (newPwdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmError = false
                        },
                        label = { Text("确认新密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = confirmError,
                        supportingText = if (confirmError) {
                            { Text("两次密码不一致", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPassword != confirmPassword) {
                            confirmError = true
                            return@TextButton
                        }
                        viewModel.updatePassword(oldPassword, newPassword)
                        showChangePasswordDialog = false
                    },
                    enabled = oldPassword.isNotBlank() && newPassword.length >= 6 && confirmPassword.isNotBlank()
                ) {
                    Text("确认修改")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("取消") }
            }
        )
    }

    // ══════════════ 找回密码对话框 ══════════════
    if (showForgotPasswordDialog) {
        var email by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var pwdVisible by remember { mutableStateOf(false) }
        var confirmError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("找回密码") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "输入注册时绑定的邮箱，设置新密码",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("邮箱") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            confirmError = false
                        },
                        label = { Text("新密码") },
                        supportingText = { Text("至少 6 个字符") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (pwdVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { pwdVisible = !pwdVisible }) {
                                Icon(
                                    if (pwdVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmError = false
                        },
                        label = { Text("确认新密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = confirmError,
                        supportingText = if (confirmError) {
                            { Text("两次密码不一致", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPassword != confirmPassword) {
                            confirmError = true
                            return@TextButton
                        }
                        viewModel.resetPasswordByEmail(email.trim(), newPassword)
                        showForgotPasswordDialog = false
                    },
                    enabled = email.contains("@") && newPassword.length >= 6 && confirmPassword.isNotBlank()
                ) {
                    Text("重置密码")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) { Text("取消") }
            }
        )
    }

    // ══════════════ 关于对话框 ══════════════
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于记账本") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("记账本 v1.0.0")
                    Text("一个基于 Kotlin + Jetpack Compose 开发的记账应用，采用 MVVM 架构和 Room 数据库。")
                    Text("功能特点：")
                    Text("• 简单易用的记账功能")
                    Text("• 多账本管理")
                    Text("• 周 / 月 / 年维度统计")
                    Text("• 分类可视化图表")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("确定") }
            }
        )
    }

    // ══════════════ 退出登录确认对话框 ══════════════
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账户吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    }
                ) {
                    Text("退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            }
        )
    }
}
