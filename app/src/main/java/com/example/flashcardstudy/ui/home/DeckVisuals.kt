package com.example.flashcardstudy.ui.home

import android.content.Context
import com.example.flashcardstudy.R

data class DeckVisualStyle(
    val bgColor: Int,
    val inkColor: Int,
    val accentColor: Int,
    val icon: String,
    val courseLabel: String
)

object DeckVisuals {
    private val emojiFallback = listOf("📚", "🧪", "💻", "🌍", "🎨", "🔥")
    private val colorMap = mapOf(
        "#4A7C3A" to Triple(
            R.color.sf_deck_bio_bg,
            R.color.sf_deck_bio_ink,
            R.color.sf_deck_bio_accent
        ),
        "#C66B1F" to Triple(
            R.color.sf_deck_spa_bg,
            R.color.sf_deck_spa_ink,
            R.color.sf_deck_spa_accent
        ),
        "#B53D2E" to Triple(
            R.color.sf_deck_his_bg,
            R.color.sf_deck_his_ink,
            R.color.sf_deck_his_accent
        ),
        "#0E8A5F" to Triple(
            R.color.sf_deck_eme_bg,
            R.color.sf_deck_eme_ink,
            R.color.sf_deck_eme_accent
        ),
        "#C84B7C" to Triple(
            R.color.sf_deck_ros_bg,
            R.color.sf_deck_ros_ink,
            R.color.sf_deck_ros_accent
        ),
        "#2E5BFF" to Triple(
            R.color.sf_deck_cal_bg,
            R.color.sf_deck_cal_ink,
            R.color.sf_deck_cal_accent
        ),
        "#6B4FE8" to Triple(
            R.color.sf_deck_neu_bg,
            R.color.sf_deck_neu_ink,
            R.color.sf_deck_neu_accent
        ),
        "#D18A00" to Triple(
            R.color.sf_deck_gld_bg,
            R.color.sf_deck_gld_ink,
            R.color.sf_deck_gld_accent
        ),
        "#0F7A8A" to Triple(
            R.color.sf_deck_tea_bg,
            R.color.sf_deck_tea_ink,
            R.color.sf_deck_tea_accent
        )
    )
    private val fallbackPalettes = listOf(
        Triple(R.color.sf_deck_bio_bg, R.color.sf_deck_bio_ink, R.color.sf_deck_bio_accent),
        Triple(R.color.sf_deck_spa_bg, R.color.sf_deck_spa_ink, R.color.sf_deck_spa_accent),
        Triple(R.color.sf_deck_his_bg, R.color.sf_deck_his_ink, R.color.sf_deck_his_accent),
        Triple(R.color.sf_deck_eme_bg, R.color.sf_deck_eme_ink, R.color.sf_deck_eme_accent),
        Triple(R.color.sf_deck_ros_bg, R.color.sf_deck_ros_ink, R.color.sf_deck_ros_accent),
        Triple(R.color.sf_deck_cal_bg, R.color.sf_deck_cal_ink, R.color.sf_deck_cal_accent),
        Triple(R.color.sf_deck_neu_bg, R.color.sf_deck_neu_ink, R.color.sf_deck_neu_accent),
        Triple(R.color.sf_deck_gld_bg, R.color.sf_deck_gld_ink, R.color.sf_deck_gld_accent),
        Triple(R.color.sf_deck_tea_bg, R.color.sf_deck_tea_ink, R.color.sf_deck_tea_accent)
    )

    fun styleFor(context: Context, deck: Deck, position: Int): DeckVisualStyle {
        val palette =
            colorMap[deck.color.uppercase()] ?: fallbackPalettes[position % fallbackPalettes.size]

        val course = deck.subtitle.ifBlank {
            val parts = deck.name.split(" ")
            if (parts.size >= 2) {
                "${parts.first()} ${parts.last()}"
            } else {
                "Study deck"
            }
        }

        return DeckVisualStyle(
            bgColor = context.getColor(palette.first),
            inkColor = context.getColor(palette.second),
            accentColor = context.getColor(palette.third),
            icon = resolveIcon(deck.icon, position),
            courseLabel = course.uppercase()
        )
    }

    private fun resolveIcon(rawIcon: String, position: Int): String {
        val icon = rawIcon.trim()
        if (icon.isBlank()) return emojiFallback[position % emojiFallback.size]
        return icon
    }
}
