package com.example.flashcardstudy.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.R

class DeckHeroAdapter(
    private var decks: List<Deck>,
    private val onDeckClick: (Deck) -> Unit
) : RecyclerView.Adapter<DeckHeroAdapter.HeroViewHolder>() {

    private data class DeckTone(val bg: Int, val ink: Int)

    private val fallbackTones = listOf(
        DeckTone("#E8F0E2".toColorInt(), "#1F4A2A".toColorInt()), // sage
        DeckTone("#FCE6CC".toColorInt(), "#5A3210".toColorInt()), // amber
        DeckTone("#EADBF5".toColorInt(), "#3A1E5A".toColorInt()), // violet
        DeckTone("#E2E8F5".toColorInt(), "#1F2C5A".toColorInt()), // indigo
        DeckTone("#F5DDD0".toColorInt(), "#5A2418".toColorInt()), // brick
        DeckTone("#1A1D24".toColorInt(), "#F4EFE6".toColorInt())  // dark
    )

    class HeroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView as CardView
        val cardLayout: ConstraintLayout = itemView.findViewById(R.id.heroCardLayout)
        val course: TextView = itemView.findViewById(R.id.heroCourse)
        val deckName: TextView = itemView.findViewById(R.id.heroDeckName)
        val dueCount: TextView = itemView.findViewById(R.id.heroDueCount)
        val playBtn: FrameLayout = itemView.findViewById(R.id.heroPlayBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeroViewHolder =
        HeroViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_deck_hero, parent, false)
        )

    override fun onBindViewHolder(holder: HeroViewHolder, position: Int) {
        val deck = decks[position]
        val tone = resolveTone(deck.color, position)

        holder.card.setCardBackgroundColor(tone.bg)
        holder.course.text = deck.subtitle.ifBlank { deck.name }
        holder.course.setTextColor(tone.ink)
        holder.deckName.text = deck.name
        holder.deckName.setTextColor(tone.ink)
        holder.dueCount.text = "${deck.cardCount} cards due"
        holder.dueCount.setTextColor(tone.ink)

        // Play button background uses ink color
        val playBg = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(
                Color.argb(
                    217,
                    Color.red(tone.ink),
                    Color.green(tone.ink),
                    Color.blue(tone.ink)
                )
            )
        }
        holder.playBtn.background = playBg

        holder.card.setOnClickListener { onDeckClick(deck) }
        holder.playBtn.setOnClickListener { onDeckClick(deck) }
    }

    override fun getItemCount() = decks.size

    fun updateDecks(newDecks: List<Deck>) {
        val oldDecks = decks
        decks = newDecks
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldDecks.size
            override fun getNewListSize(): Int = newDecks.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldDecks[oldItemPosition].id == newDecks[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldDecks[oldItemPosition] == newDecks[newItemPosition]
            }
        }).dispatchUpdatesTo(this)
    }

    private fun resolveTone(hexColor: String, position: Int): DeckTone {
        return when (hexColor.uppercase()) {
            "#FF5A1F", "#C66B1F" -> DeckTone("#FCE6CC".toColorInt(), "#5A3210".toColorInt())
            "#2E5BFF" -> DeckTone("#E2E8F5".toColorInt(), "#1F2C5A".toColorInt())
            "#6B4FE8" -> DeckTone("#EADBF5".toColorInt(), "#3A1E5A".toColorInt())
            "#B53D2E" -> DeckTone("#F5DDD0".toColorInt(), "#5A2418".toColorInt())
            "#4A7C3A", "#0E8A5F" -> DeckTone("#E8F0E2".toColorInt(), "#1F4A2A".toColorInt())
            "#1A1D24" -> DeckTone("#1A1D24".toColorInt(), "#F4EFE6".toColorInt())
            else -> {
                val parsed = runCatching { hexColor.toColorInt() }.getOrNull()
                if (parsed != null && hexColor.uppercase() != "#6C63FF") {
                    DeckTone(parsed, "#1A1D24".toColorInt())
                } else {
                    fallbackTones[position % fallbackTones.size]
                }
            }
        }
    }
}
