package com.example.flashcardstudy.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.R
import com.google.android.material.card.MaterialCardView

class JumpBackDeckAdapter(
    private var decks: List<Deck>,
    private val onDeckClick: (Deck) -> Unit,
    private val onDeckLongClick: (Deck) -> Unit
) : RecyclerView.Adapter<JumpBackDeckAdapter.JumpBackDeckViewHolder>() {

    class JumpBackDeckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.jumpDeckCard)
        val course: TextView = itemView.findViewById(R.id.jumpDeckCourse)
        val icon: TextView = itemView.findViewById(R.id.jumpDeckIcon)
        val name: TextView = itemView.findViewById(R.id.jumpDeckName)
        val due: TextView = itemView.findViewById(R.id.jumpDeckDue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JumpBackDeckViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_jump_deck, parent, false)
        return JumpBackDeckViewHolder(view)
    }

    override fun onBindViewHolder(holder: JumpBackDeckViewHolder, position: Int) {
        val deck = decks[position]
        val style = DeckVisuals.styleFor(holder.itemView.context, deck, position)
        holder.card.setCardBackgroundColor(style.bgColor)
        holder.course.text = style.courseLabel
        holder.icon.text = style.icon
        holder.name.text = deck.name
        holder.name.setTextColor(style.inkColor)
        holder.due.text = "${deck.cardCount} cards due"
        holder.due.setTextColor(style.inkColor)
        holder.itemView.setOnClickListener { onDeckClick(deck) }
        holder.itemView.setOnLongClickListener {
            onDeckLongClick(deck)
            true
        }
    }

    override fun getItemCount(): Int = decks.size

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
}
