package com.example.flashcardstudy.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.R

class DeckAdapter(
    private var decks: List<Deck>,
    private val onDeckClick: (Deck) -> Unit,
    private val onDeckLongClick: (Deck) -> Unit
) : RecyclerView.Adapter<DeckAdapter.DeckViewHolder>() {

    private val fallbackAccents = listOf(
        "#4A7C3A".toColorInt(),
        "#C66B1F".toColorInt(),
        "#2E5BFF".toColorInt(),
        "#B53D2E".toColorInt(),
        "#1A1D24".toColorInt(),
        "#6B4FE8".toColorInt()
    )

    class DeckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView as CardView
        val cardLayout: ConstraintLayout = itemView.findViewById(R.id.deckCardLayout)
        val deckName: TextView = itemView.findViewById(R.id.deckName)
        val deckSubtitle: TextView = itemView.findViewById(R.id.deckSubtitle)
        val mastery: TextView = itemView.findViewById(R.id.deckCardCount)
        val cardCount: TextView = itemView.findViewById(R.id.deckMetaText)
        val progressBar: ProgressBar = itemView.findViewById(R.id.deckProgressBar)
        val ribbon: View = itemView.findViewById(R.id.deckRibbon)
        val playIcon: ImageView = itemView.findViewById(R.id.deckPlayIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder =
        DeckViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_deck, parent, false)
        )

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        val deck = decks[position]
        val accentColor = resolveAccentColor(deck.color, position)
        val masteryPct = (kotlin.math.abs(deck.id.hashCode()) % 66) + 25

        holder.card.setCardBackgroundColor(accentColor)
        holder.deckName.text = deck.name
        holder.deckSubtitle.text = deck.subtitle.ifBlank { "General" }
        holder.mastery.text = "$masteryPct%"
        holder.cardCount.text = "${deck.cardCount} cards"
        holder.progressBar.progress = masteryPct
        holder.ribbon.alpha = 0.9f
        holder.playIcon.alpha = 0.9f

        holder.itemView.setOnClickListener { onDeckClick(deck) }
        holder.itemView.setOnLongClickListener {
            onDeckLongClick(deck)
            true
        }
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

    private fun resolveAccentColor(colorHex: String, position: Int): Int {
        return when (val normalized = colorHex.uppercase()) {
            "#4A7C3A", "#C66B1F", "#2E5BFF", "#B53D2E", "#1A1D24", "#6B4FE8", "#FF5A1F", "#0E8A5F" -> {
                if (normalized == "#FF5A1F") "#C66B1F".toColorInt()
                else if (normalized == "#0E8A5F") "#4A7C3A".toColorInt()
                else normalized.toColorInt()
            }

            else -> {
                val parsed = runCatching { colorHex.toColorInt() }.getOrNull()
                if (parsed != null && normalized != "#6C63FF") parsed else fallbackAccents[position % fallbackAccents.size]
            }
        }
    }
}
