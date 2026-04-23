package com.example.flashcardstudy.ui.deckdetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.R

class DeckDetailAdapter(
    private var cards: List<Flashcard>,
    private val onCardLongClick: (Flashcard) -> Unit
) : RecyclerView.Adapter<DeckDetailAdapter.CardViewHolder>() {

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val question: TextView = itemView.findViewById(R.id.cardQuestion)
        val answer: TextView = itemView.findViewById(R.id.cardAnswer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]
        holder.question.text = card.question
        holder.answer.text = card.answer
        holder.itemView.setOnLongClickListener {
            onCardLongClick(card)
            true
        }
    }

    override fun getItemCount(): Int = cards.size

    fun submitCards(newCards: List<Flashcard>) {
        cards = newCards
        notifyDataSetChanged()
    }
}
