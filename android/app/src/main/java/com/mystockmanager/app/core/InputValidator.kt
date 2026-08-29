package com.mystockmanager.app.core

object InputValidator {
    // Permet lettres, chiffres, espaces, tirets et apostrophes (pour les noms de produits/lieux)
    fun filterAlphanumericSpace(input: String): String {
        return input.filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '\'' || it == '.' }
    }

    // Permet uniquement les chiffres
    fun filterDigits(input: String): String {
        return input.filter { it.isDigit() }
    }

    // Permet les chiffres et un seul point/virgule pour les nombres décimaux
    fun filterDecimal(input: String): String {
        var hasSeparator = false
        return input.filter { char ->
            if (char.isDigit()) true
            else if ((char == '.' || char == ',') && !hasSeparator) {
                hasSeparator = true
                true
            } else false
        }.replace(',', '.')
    }

    // Filtre pour email (lettres, chiffres, @, ., -, _)
    fun filterEmail(input: String): String {
        return input.filter { it.isLetterOrDigit() || it == '@' || it == '.' || it == '-' || it == '_' }
    }
}
