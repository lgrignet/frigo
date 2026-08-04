package com.mystockmanager.app.ui.navigation

sealed class Screen(val route: String, val label: String = "", val icon: String = "") {
    object Expiring : Screen("expiring", "Péremption", "⏰")
    object AllItems : Screen("all", "Produits", "📦")
    object Shopping : Screen("shopping", "Courses", "🛒")
    object Storages : Screen("storages", "Rangements", "🧊")
    object Prefs : Screen("prefs", "Paramètres", "⚙️")
    object ItemForm : Screen("item_form/{itemId}") {
        fun createRoute(itemId: String = "new") = "item_form/$itemId"
    }
}
