package com.example.tiktac

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

object AppFonts {
    val Default = FontFamily.Default
    val Monospace = FontFamily.Monospace
    val Serif = FontFamily.Serif
    
    val Pacifico = FontFamily(Font(R.font.custom_font))
    val AmaticSC = FontFamily(Font(R.font.amatic_sc))
    val Bangers = FontFamily(Font(R.font.bangers))
    val Creepster = FontFamily(Font(R.font.creepster))
    val IndieFlower = FontFamily(Font(R.font.indie_flower))
    val Lobster = FontFamily(Font(R.font.lobster))
    val PermanentMarker = FontFamily(Font(R.font.permanent_marker))
    val PressStart2P = FontFamily(Font(R.font.press_start_2p))
    val Satisfy = FontFamily(Font(R.font.satisfy))

    // Master List: Name -> FontFamily
    val AllFonts = listOf(
        "System" to Default,
        "Pacifico (Fun)" to Pacifico,
        "Monospace" to Monospace,
        "Serif" to Serif,
        "Amatic SC" to AmaticSC,
        "Bangers" to Bangers,
        "Creepster (Spooky)" to Creepster,
        "Indie Flower" to IndieFlower,
        "Lobster" to Lobster,
        "Permanent Marker" to PermanentMarker,
        "Press Start 2P (8-bit)" to PressStart2P,
        "Satisfy" to Satisfy
    )
    
    fun getFont(index: Int): FontFamily {
        return AllFonts.getOrElse(index) { AllFonts[0] }.second
    }
    
    fun getFontName(index: Int): String {
        return AllFonts.getOrElse(index) { AllFonts[0] }.first
    }
}
