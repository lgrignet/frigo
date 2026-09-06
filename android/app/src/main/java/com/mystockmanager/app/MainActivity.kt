package com.mystockmanager.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mystockmanager.app.core.ExpiryWorker
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.core.SyncManager
import com.mystockmanager.app.data.local.dao.UserDao
import com.mystockmanager.app.data.repository.AuthRepository
import com.mystockmanager.app.data.repository.PrefsRepository
import com.mystockmanager.app.ui.dashboard.DashboardScreen
import com.mystockmanager.app.ui.items.AllItemsScreen
import com.mystockmanager.app.ui.items.ItemFormScreen
import com.mystockmanager.app.ui.login.LoginScreen
import com.mystockmanager.app.ui.navigation.Screen
import com.mystockmanager.app.ui.prefs.PrefsScreen
import com.mystockmanager.app.ui.recipes.CuisineTypePickerScreen
import com.mystockmanager.app.ui.recipes.RecipeDetailScreen
import com.mystockmanager.app.ui.recipes.RecipeResultsScreen
import com.mystockmanager.app.ui.shopping.ShoppingScreen
import com.mystockmanager.app.ui.storages.StoragesScreen
import com.mystockmanager.app.ui.components.AdBanner
import com.mystockmanager.app.ui.theme.Accent
import com.mystockmanager.app.ui.theme.MyStockManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var syncManager: SyncManager
    @Inject lateinit var prefsRepository: PrefsRepository
    @Inject lateinit var userDao: UserDao
    @Inject lateinit var authRepository: AuthRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission gérée
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        scheduleExpiryCheck()

        lifecycleScope.launch {
            val userId = sessionManager.getUserId()
            
            if (userId != -1L) {
                val user = userDao.getUserById(userId)
                if (user != null) {
                    // Sync session names with DB if they differ
                    if (sessionManager.getFirstName() != user.firstName || sessionManager.getLastName() != user.lastName) {
                        sessionManager.setSession(user.id, user.email, user.syncChannelGuid, user.firstName, user.lastName)
                    }
                    // Rattache à api.noshi.be les comptes créés avant l'existence du service.
                    authRepository.migrateIfNeeded()
                } else {
                    // Critical: User exists in session but GONE from DB (after wipe/migration)
                    sessionManager.clearSession()
                    finish()
                    startActivity(intent)
                    return@launch
                }
            }

            val prefs = prefsRepository.getPrefs(userId.toString()).first()
            prefs?.lang?.let { lang ->
                val appLocales = LocaleListCompat.forLanguageTags(lang)
                if (AppCompatDelegate.getApplicationLocales() != appLocales) {
                    AppCompatDelegate.setApplicationLocales(appLocales)
                }
            }
        }
        
        if (sessionManager.isLoggedIn()) {
            syncManager.startSync()
        }

        setContent {
            val userId = sessionManager.getUserId().toString()
            val prefs by prefsRepository.getPrefs(userId).collectAsState(initial = null)
            
            // Logique de thème
            val isDarkTheme = when (prefs?.theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            // Forcer la langue au niveau de Compose
            val locale = remember(prefs?.lang) { 
                if (prefs?.lang != null) Locale(prefs!!.lang) else Locale.getDefault() 
            }
            val configuration = LocalConfiguration.current
            val localizedConfig = Configuration(configuration).apply {
                setLocale(locale)
            }

            CompositionLocalProvider(LocalConfiguration provides localizedConfig) {
                MyStockManagerTheme(darkTheme = isDarkTheme, lang = prefs?.lang) {
                    var isLoggedIn by remember { mutableStateOf(sessionManager.isLoggedIn()) }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (isLoggedIn) {
                            MainScreen(onLogout = {
                                syncManager.stopSync()
                                isLoggedIn = false
                                lifecycleScope.launch { authRepository.logout() }
                            })
                        } else {
                            LoginScreen(onLoginSuccess = {
                                isLoggedIn = true
                                syncManager.startSync()
                            })
                        }
                    }
                }
            }
        }
    }

    private fun scheduleExpiryCheck() {
        val workRequest = PeriodicWorkRequestBuilder<ExpiryWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "expiry_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Expiring,
        Screen.AllItems,
        Screen.Shopping,
        Screen.Storages,
        Screen.Prefs
    )

    Scaffold(
        bottomBar = {
            Column {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Text(screen.icon, fontSize = 20.sp) },
                            label = { 
                                val label = when(screen) {
                                    Screen.Expiring -> stringResource(R.string.tab_expiring)
                                    Screen.AllItems -> stringResource(R.string.tab_products)
                                    Screen.Shopping -> stringResource(R.string.tab_shopping)
                                    Screen.Storages -> stringResource(R.string.tab_storages)
                                    Screen.Prefs -> stringResource(R.string.tab_settings)
                                    else -> ""
                                }
                                Text(label, fontSize = 10.sp) 
                            },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
                AdBanner()
            }
        },
        floatingActionButton = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            // Limiter le bouton d'ajout de produit uniquement à l'onglet "Produits"
            if (currentRoute == Screen.AllItems.route) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.ItemForm.createRoute()) },
                    containerColor = Accent,
                    contentColor = Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text("＋", fontSize = 24.sp, color = Color.Black)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Expiring.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Expiring.route) {
                DashboardScreen(
                    onSearchRecipe = { itemId ->
                        navController.navigate(Screen.CuisineTypePicker.createRoute(listOf(itemId)))
                    }
                )
            }
            composable(Screen.AllItems.route) {
                AllItemsScreen(
                    onEditItem = { itemId ->
                        navController.navigate(Screen.ItemForm.createRoute(itemId))
                    },
                    onSearchRecipes = { itemIds ->
                        navController.navigate(Screen.CuisineTypePicker.createRoute(itemIds))
                    }
                )
            }
            composable(Screen.Shopping.route) { 
                ShoppingScreen() 
            }
            composable(Screen.Storages.route) { 
                StoragesScreen() 
            }
            composable(Screen.Prefs.route) {
                PrefsScreen(onLogout = onLogout)
            }
            composable(
                route = Screen.ItemForm.route,
                arguments = listOf(navArgument("itemId") { defaultValue = "new" })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                ItemFormScreen(
                    itemId = if (itemId == "new") null else itemId,
                    onSaveSuccess = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Screen.CuisineTypePicker.route) { backStackEntry ->
                val itemIds = backStackEntry.arguments?.getString("itemIds")?.split(",") ?: emptyList()
                CuisineTypePickerScreen(
                    onCuisineSelected = { cuisine ->
                        navController.navigate(Screen.RecipeResults.createRoute(itemIds, cuisine.code))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.RecipeResults.route) { backStackEntry ->
                val itemIds = backStackEntry.arguments?.getString("itemIds")?.split(",") ?: emptyList()
                val cuisineType = backStackEntry.arguments?.getString("cuisineType") ?: ""
                RecipeResultsScreen(
                    itemIds = itemIds,
                    cuisineType = cuisineType,
                    onRecipeSelected = { recipeId ->
                        navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.RecipeDetail.route) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                RecipeDetailScreen(
                    recipeId = recipeId,
                    onDone = {
                        // Revient à l'onglet d'origine (Tous les produits ou Bientôt périmé)
                        // selon le point d'entrée emprunté pour cette recherche de recette.
                        if (!navController.popBackStack(Screen.AllItems.route, false)) {
                            navController.popBackStack(Screen.Expiring.route, false)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
