package com.example.flashcardstudy.ui.deckdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.MainActivity
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.repository.RepositoryProvider
import kotlinx.coroutines.launch

class DeckDetailFragment : Fragment() {
    private var deckId: String? = null
    private var deckName: String? = null
    private var deckColor: String = "#6C63FF"
    private lateinit var adapter: DeckDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deckId = arguments?.getString(ARG_DECK_ID)
        deckName = arguments?.getString(ARG_DECK_NAME)
        deckColor = arguments?.getString(ARG_DECK_COLOR) ?: "#6C63FF"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_deck_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleView = view.findViewById<TextView>(R.id.deckDetailTitle)
        val emptyView = view.findViewById<TextView>(R.id.emptyState)
        val cardsView = view.findViewById<RecyclerView>(R.id.deckRecyclerView)
        val addCardButton = view.findViewById<Button>(R.id.addCardButton)
        val studyButton = view.findViewById<Button>(R.id.studyDeckButton)

        titleView.text = deckName ?: "Deck Detail"
        adapter = DeckDetailAdapter(emptyList())
        cardsView.layoutManager = LinearLayoutManager(requireContext())
        cardsView.adapter = adapter

        val currentDeckId = deckId
        if (currentDeckId == null) {
            emptyView.visibility = View.VISIBLE
            emptyView.text = "No deck selected"
            return
        }

        addCardButton.setOnClickListener {
            (activity as? MainActivity)?.openAddCardForDeck(
                deckId = currentDeckId,
                deckName = deckName ?: "Deck",
                deckColor = deckColor
            )
        }

        studyButton.setOnClickListener {
            (activity as? MainActivity)?.openStudyWithDeck(currentDeckId)
        }

        lifecycleScope.launch {
            val cards = RepositoryProvider.flashcardRepository.getFlashcardsForDeck(currentDeckId)
            adapter.submitCards(cards)
            emptyView.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    companion object {
        private const val ARG_DECK_ID = "deck_id"
        private const val ARG_DECK_NAME = "deck_name"
        private const val ARG_DECK_COLOR = "deck_color"

        fun newInstance(deckId: String, deckName: String, deckColor: String): DeckDetailFragment {
            return DeckDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DECK_ID, deckId)
                    putString(ARG_DECK_NAME, deckName)
                    putString(ARG_DECK_COLOR, deckColor)
                }
            }
        }
    }
}
