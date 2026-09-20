package com.mystockmanager.app.ui.components

import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

/**
 * Bouton 📖 d'accès à l'écran « Mes recettes ». Affiché sur la ligne du titre de
 * chaque onglet principal, de sorte qu'il reste accessible sans occuper de bande
 * dédiée au-dessus du titre.
 */
@Composable
fun RecipesIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Text("📖", fontSize = 20.sp)
    }
}
