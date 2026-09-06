package com.mystockmanager.app.core

import androidx.annotation.StringRes
import com.mystockmanager.app.R

/**
 * Liste figée côté app (comme les langues fr/en/es/de/nl) plutôt que gérée en
 * base — voir §4.6 du plan recettes IA. [code] est l'identifiant stable envoyé
 * au serveur (clé de cache), indépendant de la langue d'affichage de l'app.
 */
enum class CuisineType(val code: String, @StringRes val labelRes: Int) {
    FRANCAISE("francais", R.string.cuisine_francais),
    ITALIENNE("italien", R.string.cuisine_italien),
    CHINOISE("chinois", R.string.cuisine_chinois),
    JAPONAISE("japonais", R.string.cuisine_japonais),
    INDIENNE("indien", R.string.cuisine_indien),
    THAILANDAISE("thailandais", R.string.cuisine_thailandais),
    MEXICAINE("mexicain", R.string.cuisine_mexicain),
    MEDITERRANEENNE("mediterraneen", R.string.cuisine_mediterraneen),
    AMERICAINE("americain", R.string.cuisine_americain),
    AUTRE("autre", R.string.cuisine_autre);

    companion object {
        fun fromCode(code: String): CuisineType? = entries.find { it.code == code }
    }
}
