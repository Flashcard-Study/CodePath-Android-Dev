package com.example.flashcardstudy.ui.deckdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.R
import com.example.flashcardstudy.deckId
import com.example.flashcardstudy.deck_titles
import com.example.flashcardstudy.flashcards

class DeckDetailFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_deck_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deckTitle = view.findViewById<TextView>(R.id.deckDetailTitle)
        deckTitle.text = deck_titles[deckId]
        val cards = view.findViewById<RecyclerView>(R.id.deckRecyclerView)
        cards.adapter = DeckDetailAdapter(flashcards[deckId])
        cards.layoutManager = LinearLayoutManager(requireContext())
        val empty = view.findViewById<TextView>(R.id.emptyState)
        if (flashcards[deckId].isEmpty()) {
            empty.visibility = View.VISIBLE
        }
    }
}
