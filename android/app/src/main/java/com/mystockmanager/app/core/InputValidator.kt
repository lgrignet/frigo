package com.mystockmanager.app.core

object InputValidator {
    // Même règle que le serveur (API/src/server.js, EMAIL_RE) : "quelque chose@quelque chose.quelque chose"
    private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

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

    // Filtre pour le code de récupération (lettres, chiffres, tirets), toujours en majuscules
    fun filterRecoveryCode(input: String): String {
        return input.filter { it.isLetterOrDigit() || it == '-' }.uppercase()
    }
}
