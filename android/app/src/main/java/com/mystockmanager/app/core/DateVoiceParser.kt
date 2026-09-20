package com.mystockmanager.app.core

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object DateVoiceParser {

    fun parse(text: String, language: String): LocalDate? {
        val normalized = text.lowercase().trim()
        val today = LocalDate.now()

        // 1. Gérer les expressions relatives simples
        val relativeDate = parseRelative(normalized, language, today)
        if (relativeDate != null) return relativeDate

        // 2. Gérer les expressions "Dans X [unité]"
        val inXDate = parseInX(normalized, language, today)
        if (inXDate != null) return inXDate

        // 3. Gérer les dates absolues (31 décembre, etc.)
        val absoluteDate = parseAbsolute(normalized, language, today)
        if (absoluteDate != null) return absoluteDate

        return null
    }

    private fun parseRelative(text: String, lang: String, today: LocalDate): LocalDate? {
        return when (lang) {
            "fr" -> when {
                text.contains("demain") -> today.plusDays(1)
                text.contains("après-demain") || text.contains("apres demain") -> today.plusDays(2)
                text.contains("fin du mois") -> today.with(TemporalAdjusters.lastDayOfMonth())
                text.contains("fin de l'année") || text.contains("fin d'année") -> today.with(TemporalAdjusters.lastDayOfYear())
                else -> null
            }
            "en" -> when {
                text.contains("tomorrow") -> today.plusDays(1)
                text.contains("day after tomorrow") -> today.plusDays(2)
                text.contains("end of month") -> today.with(TemporalAdjusters.lastDayOfMonth())
                text.contains("end of year") -> today.with(TemporalAdjusters.lastDayOfYear())
                else -> null
            }
            "nl" -> when {
                text.contains("morgen") -> today.plusDays(1)
                text.contains("overmorgen") -> today.plusDays(2)
                text.contains("einde van de maand") -> today.with(TemporalAdjusters.lastDayOfMonth())
                text.contains("einde van het jaar") -> today.with(TemporalAdjusters.lastDayOfYear())
                else -> null
            }
            "de" -> when {
                text.contains("morgen") -> today.plusDays(1)
                text.contains("übermorgen") || text.contains("ubermorgen") -> today.plusDays(2)
                text.contains("ende des monats") -> today.with(TemporalAdjusters.lastDayOfMonth())
                text.contains("ende des jahres") -> today.with(TemporalAdjusters.lastDayOfYear())
                else -> null
            }
            "es" -> when {
                text.contains("mañana") -> today.plusDays(1)
                text.contains("pasado mañana") -> today.plusDays(2)
                text.contains("fin de mes") -> today.with(TemporalAdjusters.lastDayOfMonth())
                text.contains("fin de año") -> today.with(TemporalAdjusters.lastDayOfYear())
                else -> null
            }
            else -> null
        }
    }

    private fun parseInX(text: String, lang: String, today: LocalDate): LocalDate? {
        val number = extractNumber(text) ?: return null
        
        return when (lang) {
            "fr" -> when {
                text.contains("jour") -> today.plusDays(number.toLong())
                text.contains("semaine") -> today.plusWeeks(number.toLong())
                text.contains("mois") -> today.plusMonths(number.toLong())
                text.contains("an") -> today.plusYears(number.toLong())
                else -> null
            }
            "en" -> when {
                text.contains("day") -> today.plusDays(number.toLong())
                text.contains("week") -> today.plusWeeks(number.toLong())
                text.contains("month") -> today.plusMonths(number.toLong())
                text.contains("year") -> today.plusYears(number.toLong())
                else -> null
            }
            "nl" -> when {
                text.contains("dag") -> today.plusDays(number.toLong())
                text.contains("week") -> today.plusWeeks(number.toLong())
                text.contains("maand") -> today.plusMonths(number.toLong())
                text.contains("jaar") -> today.plusYears(number.toLong())
                else -> null
            }
            "de" -> when {
                text.contains("tag") -> today.plusDays(number.toLong())
                text.contains("woche") -> today.plusWeeks(number.toLong())
                text.contains("monat") -> today.plusMonths(number.toLong())
                text.contains("jahr") -> today.plusYears(number.toLong())
                else -> null
            }
            "es" -> when {
                text.contains("día") || text.contains("dia") -> today.plusDays(number.toLong())
                text.contains("semana") -> today.plusWeeks(number.toLong())
                text.contains("mes") -> today.plusMonths(number.toLong())
                text.contains("año") || text.contains("ano") -> today.plusYears(number.toLong())
                else -> null
            }
            else -> null
        }
    }

    private fun parseAbsolute(text: String, lang: String, today: LocalDate): LocalDate? {
        val day = extractNumber(text) ?: return null
        val month = extractMonth(text, lang) ?: return null
        
        // On cherche une année, si absente on prend l'année en cours
        // ou l'année prochaine si le mois est déjà passé
        val year = extractYear(text) ?: run {
            if (month < today.monthValue || (month == today.monthValue && day < today.dayOfMonth)) {
                today.year + 1
            } else {
                today.year
            }
        }

        return try {
            LocalDate.of(year, month, day)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractNumber(text: String): Int? {
        val digitRegex = Regex("(\\d+)")
        val match = digitRegex.find(text)
        if (match != null) return match.groupValues[1].toInt()
        
        // Petit dictionnaire de nombres en lettres pour les cas simples
        val names = mapOf(
            "un" to 1, "une" to 1, "one" to 1, "een" to 1, "eins" to 1, "uno" to 1, "una" to 1,
            "deux" to 2, "two" to 2, "twee" to 2, "zwei" to 2, "dos" to 2,
            "trois" to 3, "three" to 3, "drie" to 3, "drei" to 3, "tres" to 3,
            "quatre" to 4, "four" to 4, "vier" to 4, "vier" to 4, "cuatro" to 4,
            "cinq" to 5, "five" to 5, "vijf" to 5, "fünf" to 5, "cinco" to 5,
            "six" to 6, "six" to 6, "zes" to 6, "sechs" to 6, "seis" to 6,
            "sept" to 7, "seven" to 7, "zeven" to 7, "sieben" to 7, "siete" to 7,
            "huit" to 8, "eight" to 8, "acht" to 8, "acht" to 8, "ocho" to 8,
            "neuf" to 9, "nine" to 9, "negen" to 9, "neun" to 9, "nueve" to 9,
            "dix" to 10, "ten" to 10, "tien" to 10, "zehn" to 10, "diez" to 10
        )
        
        for ((name, value) in names) {
            if (text.contains(name)) return value
        }
        
        return null
    }

    private fun extractMonth(text: String, lang: String): Int? {
        val months = mapOf(
            "fr" to listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"),
            "en" to listOf("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"),
            "nl" to listOf("januari", "februari", "maart", "april", "mei", "juni", "juli", "augustus", "september", "oktober", "november", "december"),
            "de" to listOf("januar", "februar", "märz", "april", "mai", "juni", "juli", "august", "september", "oktober", "november", "dezember"),
            "es" to listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
        )

        val list = months[lang] ?: months["en"]!!
        list.forEachIndexed { index, name ->
            if (text.contains(name)) return index + 1
        }
        
        // Support numérique (ex: "12 10 2026")
        val parts = text.split(" ", "/", "-", ".")
        if (parts.size >= 2) {
            parts.forEach { part ->
                val m = part.toIntOrNull()
                if (m != null && m in 1..12) return m
            }
        }

        return null
    }

    private fun extractYear(text: String): Int? {
        val yearRegex = Regex("(20\\d{2})") // Cherche 2024, 2025, etc.
        val match = yearRegex.find(text)
        return match?.groupValues[1]?.toInt()
    }
}
