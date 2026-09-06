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
    // itemIds : identifiants d'articles séparés par des virgules (des UUID, jamais de virgule dedans)
    object CuisineTypePicker : Screen("cuisine_picker/{itemIds}") {
        fun createRoute(itemIds: List<String>) = "cuisine_picker/${itemIds.joinToString(",")}"
    }
    object RecipeResults : Screen("recipe_results/{itemIds}/{cuisineType}") {
        fun createRoute(itemIds: List<String>, cuisineType: String) = "recipe_results/${itemIds.joinToString(",")}/$cuisineType"
    }
    object RecipeDetail : Screen("recipe_detail/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe_detail/$recipeId"
    }
}
