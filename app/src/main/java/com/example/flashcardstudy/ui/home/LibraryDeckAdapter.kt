package com.example.flashcardstudy.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.R
import com.google.android.material.card.MaterialCardView

class LibraryDeckAdapter(
    private var decks: List<Deck>,
    private val onDeckClick: (Deck) -> Unit,
    private val onDeckLongClick: (Deck) -> Unit
) : RecyclerView.Adapter<LibraryDeckAdapter.LibraryDeckViewHolder>() {

    class LibraryDeckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.libraryDeckCard)
        val icon: TextView = itemView.findViewById(R.id.libraryDeckIcon)
        val mastery: TextView = itemView.findViewById(R.id.libraryDeckMastery)
        val course: TextView = itemView.findViewById(R.id.libraryDeckCourse)
        val name: TextView = itemView.findViewById(R.id.libraryDeckName)
        val progress: ProgressBar = itemView.findViewById(R.id.libraryDeckProgress)
        val count: TextView = itemView.findViewById(R.id.libraryDeckCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryDeckViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_library_deck, parent, false)
        return LibraryDeckViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibraryDeckViewHolder, position: Int) {
        val deck = decks[position]
        val style = DeckVisuals.styleFor(holder.itemView.context, deck, position)
        val masteryPercent = deck.masteryPercent

        holder.card.setCardBackgroundColor(style.accentColor)
        holder.icon.text = style.icon
        holder.course.text = style.courseLabel
        holder.name.text = deck.name
        if (masteryPercent != null) {
            holder.mastery.visibility = View.VISIBLE
            holder.mastery.text = "$masteryPercent%"
            holder.progress.visibility = View.VISIBLE
            holder.progress.progress = masteryPercent
            holder.count.text = "${deck.cardCount} cards"
        } else {
            holder.mastery.visibility = View.GONE
            holder.progress.visibility = View.GONE
            holder.count.text = "${deck.cardCount} cards - no tracking yet"
        }
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
