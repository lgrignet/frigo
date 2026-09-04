package com.mystockmanager.app.ui.navigation

sealed class Screen(val route: String, val icon: String = "") {
    object Expiring : Screen("expiring", "⏰")
    object AllItems : Screen("all", "📦")
    object Shopping : Screen("shopping", "🛒")
    object Storages : Screen("storages", "🧊")
    object Prefs : Screen("prefs", "⚙️")
    object ItemForm : Screen("item_form/{itemId}") {
        fun createRoute(itemId: String = "new") = "item_form/$itemId"
    }
}
