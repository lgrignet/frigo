package com.mystockmanager.app.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mystockmanager.app.R
import com.mystockmanager.app.data.local.entities.UnitEntity

object UnitTranslator {
    @Composable
    fun getTranslatedName(unit: UnitEntity): String {
        return when {
            unit.id == "unit_piece" || unit.name == "pièce(s)" || unit.name == "Piece(s)" -> stringResource(R.string.unit_piece_name)
            unit.id == "unit_kg" || unit.name == "Kilogramme" || unit.name == "Kilogram" -> stringResource(R.string.unit_kg_name)
            unit.id == "unit_g" || unit.name == "Gramme" || unit.name == "Gram" -> stringResource(R.string.unit_g_name)
            unit.id == "unit_l" || unit.name == "Litre" || unit.name == "Liter" -> stringResource(R.string.unit_l_name)
            unit.id == "unit_packet" || unit.name == "Paquet" || unit.name == "Packet(s)" -> stringResource(R.string.unit_packet_name)
            else -> unit.name
        }
    }

    @Composable
    fun getTranslatedLabel(unit: UnitEntity): String {
        return when {
            unit.id == "unit_piece" || unit.label == "pièce(s)" || unit.label == "pc" || unit.label == "st" -> stringResource(R.string.unit_piece_label)
            unit.id == "unit_kg" || unit.label == "kg" -> stringResource(R.string.unit_kg_label)
            unit.id == "unit_g" || unit.label == "g" -> stringResource(R.string.unit_g_label)
            unit.id == "unit_l" || unit.label == "L" -> stringResource(R.string.unit_l_label)
            unit.id == "unit_packet" || unit.label == "paquet(s)" || unit.label == "pkt" || unit.label == "pak" -> stringResource(R.string.unit_packet_label)
            else -> unit.label
        }
    }
    
    @Composable
    fun translateLabel(label: String): String {
        return when (label.lowercase().trim()) {
            "pièce(s)", "pc", "piece(s)", "st", "stuk(s)", "stück", "stk", "pieza(s)", "pz" -> stringResource(R.string.unit_piece_label)
            "kilogramme", "kg", "kilogram", "kilogramm", "kilogramo" -> stringResource(R.string.unit_kg_label)
            "gramme", "g", "gram", "gramm", "gramo" -> stringResource(R.string.unit_g_label)
            "litre", "l", "liter", "litro" -> stringResource(R.string.unit_l_label)
            "paquet", "paquet(s)", "pkt", "packet(s)", "pak", "pakken", "paket(e)", "paquete(s)", "paq" -> stringResource(R.string.unit_packet_label)
            else -> label
        }
    }
}
