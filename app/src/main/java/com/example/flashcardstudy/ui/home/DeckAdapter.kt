package com.example.flashcardstudy.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.R
import com.example.flashcardstudy.deckId
import com.google.android.material.card.MaterialCardView

class DeckAdapter(
    private val decks: List<Deck>,
    private val onDeckClick: (Deck) -> Unit
) : RecyclerView.Adapter<DeckAdapter.DeckViewHolder>() {
    class DeckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView as MaterialCardView
        val ribbon: View = itemView.findViewById(R.id.deckRibbon)
        val deckName: TextView = itemView.findViewById(R.id.deckName)
        val cardCount: TextView = itemView.findViewById(R.id.deckCardCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder =
        DeckViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_deck, parent, false)
        )

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        val deck = decks[position]
        val color = deck.color.toColorInt()
        holder.deckName.text = deck.name
        holder.cardCount.text = "${deck.cardCount} cards"
        holder.ribbon.setBackgroundColor(color)
        holder.card.setCardBackgroundColor(ColorUtils.blendARGB(Color.WHITE, color, 0.15f))
        holder.itemView.setOnClickListener {
            onDeckClick(deck)
            deckId = position
        }
    }

    override fun getItemCount() = decks.size
}
