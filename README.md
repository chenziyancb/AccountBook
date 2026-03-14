# AccountBook - 记账本 Android App

一款基于 Jetpack Compose 开发的 Android 个人记账应用，采用 MVVM 架构设计。

## 功能特性

- **用户系统**：注册、登录、Session 持久化管理
- **账务记录**：支持收入/支出记录，含金额、分类、备注、日期
- **首页概览**：月度收支汇总，交易记录列表，快速新增/编辑/删除
- **统计分析**：分类支出横向进度条图表，月度收支可视化对比
- **分类管理**：自定义收支分类，预置 8 个默认分类
- **设置页面**：账号信息，退出登录

## 技术栈

| 技术 | 说明 |
|------|------|
| Jetpack Compose | 声明式 UI 框架 |
| MVVM | 架构模式（ViewModel + Repository） |
| Room | 本地数据库（SQLite ORM） |
| Hilt | 依赖注入 |
| Kotlin Coroutines + Flow | 异步数据流 |
| Navigation Compose | 页面导航 |
| Material3 | UI 设计规范 |

## 项目结构

```
app/src/main/java/com/claw/accountbook/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAO 接口
│   │   ├── entity/       # 数据库实体类
│   │   ├── AccountBookDatabase.kt
│   │   └── SessionManager.kt
│   └── repository/       # 数据仓库层
├── di/                   # Hilt 依赖注入模块
├── ui/
│   ├── screens/          # 各功能页面
│   │   ├── auth/         # 登录/注册
│   │   ├── home/         # 首页
│   │   ├── statistics/   # 统计
│   │   ├── category/     # 分类管理
│   │   └── settings/     # 设置
│   └── theme/            # 主题配置
├── viewmodel/            # ViewModel 层
├── AccountBookApp.kt     # Application 入口
└── MainActivity.kt       # Activity 入口
```

## 环境要求

- Android Studio Hedgehog 或更高版本
- Android SDK 34
- Kotlin 1.9.22
- Gradle 8.2.0

## 快速开始

```bash
git clone https://github.com/chenziyancb/AccountBook.git
```

在 Android Studio 中打开项目，等待 Gradle 同步完成后即可运行。

## 开发团队

由多个 AI Agent 协作开发（飞书任务链协同）：
- 小飞 - 项目规划
- 小飞熊2 - 架构搭建
- 小腾熊（WorkBuddy Agent）- 功能实现与统计可视化
