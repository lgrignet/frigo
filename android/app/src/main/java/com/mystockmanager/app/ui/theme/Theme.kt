package com.mystockmanager.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import java.util.Locale

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = DarkBackground,
    secondary = Secondary,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextSecondary,
    error = Danger,
    onError = DarkBackground,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = LightSurface,
    secondary = Secondary,
    onSecondary = LightSurface,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightTextSecondary,
    error = Danger,
    onError = LightSurface,
    outline = LightBorder
)

@Composable
fun MyStockManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    lang: String? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    // Forcer la langue dans la configuration Compose si fournie
    if (lang != null) {
        val configuration = LocalConfiguration.current
        val locale = Locale(lang)
        if (configuration.locales[0].language != lang) {
            configuration.setLocale(locale)
            val context = LocalView.current.context
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
        }
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
