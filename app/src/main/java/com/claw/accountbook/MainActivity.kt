package com.claw.accountbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.claw.accountbook.ui.screens.MainScreen
import com.claw.accountbook.ui.screens.auth.LoginScreen
import com.claw.accountbook.ui.screens.auth.RegisterScreen
import com.claw.accountbook.ui.theme.AccountBookTheme
import com.claw.accountbook.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用路由常量
 */
object AppRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AccountBookTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(userViewModel = userViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(userViewModel: UserViewModel) {
    val navController = rememberNavController()
    val uiState by userViewModel.uiState.collectAsState()

    // 根据登录状态决定起始路由
    val startDestination = remember { AppRoutes.LOGIN }

    // 监听登录状态变化，已登录则跳转主页
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            navController.navigate(AppRoutes.MAIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    // 导航由上方的 LaunchedEffect(uiState.isLoggedIn) 统一处理，此处不再重复 navigate
                },
                onNavigateToRegister = {
                    navController.navigate(AppRoutes.REGISTER)
                },
                viewModel = userViewModel
            )
        }

        composable(AppRoutes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    // 导航由上方的 LaunchedEffect(uiState.isLoggedIn) 统一处理，此处不再重复 navigate
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                viewModel = userViewModel
            )
        }

        composable(AppRoutes.MAIN) {
            MainScreen(
                onLogout = {
                    userViewModel.logout()
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
