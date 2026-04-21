package com.example.flashcardstudy.ui.deckdetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.R

class DeckDetailAdapter(
    private val cards: List<Flashcard>
) : RecyclerView.Adapter<DeckDetailAdapter.DeckDetailViewHolder>() {
    class DeckDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val content: TextView = itemView.findViewById(R.id.cardContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckDetailViewHolder =
        DeckDetailViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        )

    override fun onBindViewHolder(holder: DeckDetailViewHolder, position: Int) {
        val card = cards[position]
        holder.content.text = card.question
        holder.itemView.setOnClickListener {
            if (holder.content.text == card.question) {
                holder.content.text = card.answer
            } else {
                holder.content.text = card.question
            }
        }
    }

    override fun getItemCount() = cards.size
}
