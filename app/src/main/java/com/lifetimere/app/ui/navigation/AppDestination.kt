package com.lifetimere.app.ui.navigation

enum class AppDestination(val route: String, val label: String, val symbol: String) {
    TODAY("today", "今日", "日"),
    CHARACTER("character", "角色", "人"),
    WORLD("world", "世界", "界"),
    HISTORY("history", "回顾", "录"),
}
