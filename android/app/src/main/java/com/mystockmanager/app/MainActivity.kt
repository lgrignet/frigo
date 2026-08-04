package com.mystockmanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mystockmanager.app.core.ExpiryWorker
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.core.SyncManager
import com.mystockmanager.app.data.repository.PrefsRepository
import com.mystockmanager.app.ui.dashboard.DashboardScreen
import com.mystockmanager.app.ui.items.AllItemsScreen
import com.mystockmanager.app.ui.items.ItemFormScreen
import com.mystockmanager.app.ui.login.LoginScreen
import com.mystockmanager.app.ui.navigation.Screen
import com.mystockmanager.app.ui.prefs.PrefsScreen
import com.mystockmanager.app.ui.shopping.ShoppingScreen
import com.mystockmanager.app.ui.storages.StoragesScreen
import com.mystockmanager.app.ui.theme.Accent
import com.mystockmanager.app.ui.theme.MyStockManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.activity.result.contract.ActivityResultContracts
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var syncManager: SyncManager
    @Inject lateinit var prefsRepository: PrefsRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission gérée
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Appliquer la langue le plus tôt possible
        val userId = sessionManager.getUserId().toString()
        lifecycleScope.launch {
            prefsRepository.getPrefs(userId).first()?.lang?.let { lang ->
                val appLocales = LocaleListCompat.forLanguageTags(lang)
                if (AppCompatDelegate.getApplicationLocales() != appLocales) {
                    AppCompatDelegate.setApplicationLocales(appLocales)
                }
            }
        }

        scheduleExpiryCheck()
        
        if (sessionManager.isLoggedIn()) {
            syncManager.startSync()
        }

        setContent {
            val userId = sessionManager.getUserId().toString()
            val prefs by prefsRepository.getPrefs(userId).collectAsState(initial = null)
            val isDarkTheme = when (prefs?.theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            MyStockManagerTheme(darkTheme = isDarkTheme, lang = prefs?.lang) {
                var isLoggedIn by remember { mutableStateOf(sessionManager.isLoggedIn()) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLoggedIn) {
                        MainScreen(onLogout = {
                            syncManager.stopSync()
                            sessionManager.clearSession()
                            isLoggedIn = false
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
                                else -> screen.label
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
            composable(Screen.Expiring.route) { DashboardScreen() }
            composable(Screen.AllItems.route) { 
                AllItemsScreen(onEditItem = { itemId -> 
                    navController.navigate(Screen.ItemForm.createRoute(itemId))
                }) 
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
        }
    }
}
