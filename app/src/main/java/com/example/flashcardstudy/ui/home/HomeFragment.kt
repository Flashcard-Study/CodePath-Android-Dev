package com.example.flashcardstudy.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.MainActivity
import com.example.flashcardstudy.R

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.continueCard).setOnClickListener {
            (activity as? MainActivity)?.openDeckDetail()
        }

        view.findViewById<RecyclerView>(R.id.deckRecyclerView).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = DeckAdapter(MOCK_DECKS) {
                (activity as? MainActivity)?.openDeckDetail()
            }
        }
    }

    companion object {
        private val MOCK_DECKS = listOf(
            Deck("Biology", "#009628", 12),
            Deck("Spanish", "#E86C00", 8),
            Deck("Mathematics", "#4A90D9", 20),
            Deck("World History", "#9B59B6", 15)
        )
    }
}
