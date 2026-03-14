package com.claw.accountbook.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.claw.accountbook.viewmodel.UserViewModel

/**
 * 注册页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: UserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    // 注册成功跳转
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onRegisterSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建账户") },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 欢迎文字
                Text(
                    text = "欢迎加入记账本",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "创建账户，开始记录你的财务生活",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 注册表单卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        // 用户名
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                usernameError = null
                            },
                            label = { Text("用户名 *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            isError = usernameError != null,
                            supportingText = usernameError?.let { { Text(it) } }
                                ?: { Text("3-20个字符，仅支持字母、数字和下划线", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 邮箱（选填）
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = null
                            },
                            label = { Text("邮箱（选填）") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            isError = emailError != null,
                            supportingText = emailError?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 密码
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                                if (confirmPassword.isNotEmpty() && confirmPassword != it) {
                                    confirmPasswordError = "两次输入的密码不一致"
                                } else {
                                    confirmPasswordError = null
                                }
                            },
                            label = { Text("密码 *") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            isError = passwordError != null,
                            supportingText = passwordError?.let { { Text(it) } }
                                ?: { Text("至少6个字符", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 确认密码
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                confirmPasswordError = if (it != password) "两次输入的密码不一致" else null
                            },
                            label = { Text("确认密码 *") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        if (confirmPasswordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = if (confirmPasswordVisible) "隐藏密码" else "显示密码"
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            isError = confirmPasswordError != null,
                            supportingText = confirmPasswordError?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    performRegister(
                                        username, password, confirmPassword, email,
                                        onUsernameError = { usernameError = it },
                                        onPasswordError = { passwordError = it },
                                        onConfirmPasswordError = { confirmPasswordError = it },
                                        onEmailError = { emailError = it },
                                        onValid = {
                                            viewModel.register(username, password, email.ifBlank { null })
                                        }
                                    )
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 错误提示
                        if (uiState.error != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = uiState.error ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // 注册按钮
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                performRegister(
                                    username, password, confirmPassword, email,
                                    onUsernameError = { usernameError = it },
                                    onPasswordError = { passwordError = it },
                                    onConfirmPasswordError = { confirmPasswordError = it },
                                    onEmailError = { emailError = it },
                                    onValid = {
                                        viewModel.register(username, password, email.ifBlank { null })
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("注册", fontSize = 16.sp)
                            }
                        }
                    }
                }

                // 已有账户入口
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已有账户？",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onNavigateToLogin) {
                        Text("立即登录")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun performRegister(
    username: String,
    password: String,
    confirmPassword: String,
    email: String,
    onUsernameError: (String) -> Unit,
    onPasswordError: (String) -> Unit,
    onConfirmPasswordError: (String) -> Unit,
    onEmailError: (String) -> Unit,
    onValid: () -> Unit
) {
    var valid = true

    // 用户名验证
    when {
        username.isBlank() -> { onUsernameError("请输入用户名"); valid = false }
        username.length < 3 -> { onUsernameError("用户名至少3个字符"); valid = false }
        username.length > 20 -> { onUsernameError("用户名不能超过20个字符"); valid = false }
        !username.matches(Regex("[a-zA-Z0-9_\u4e00-\u9fa5]+")) -> {
            onUsernameError("用户名只能包含字母、数字、下划线或中文"); valid = false
        }
    }

    // 密码验证
    when {
        password.isBlank() -> { onPasswordError("请输入密码"); valid = false }
        password.length < 6 -> { onPasswordError("密码至少6个字符"); valid = false }
    }

    // 确认密码验证
    if (password.isNotBlank() && confirmPassword != password) {
        onConfirmPasswordError("两次输入的密码不一致"); valid = false
    }

    // 邮箱验证（选填）
    if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        onEmailError("请输入有效的邮箱地址"); valid = false
    }

    if (valid) onValid()
}
