package com.example.flashcardstudy.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.MainActivity
import com.example.flashcardstudy.R

class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var deckAdapter: DeckAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.continueCard).setOnClickListener {
            viewModel.getMostRecentDeck()
        }

        deckAdapter = DeckAdapter(emptyList()) { deck ->
            (activity as? MainActivity)?.openStudyWithDeck(deck.id)
        }

        view.findViewById<RecyclerView>(R.id.deckRecyclerView).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deckAdapter
        }

        viewModel.decks.observe(viewLifecycleOwner) { dbDecks ->
            val uiDecks = dbDecks.map { Deck.fromDatabaseDeck(it) }
            deckAdapter.updateDecks(uiDecks)
        }

        viewModel.mostRecentDeck.observe(viewLifecycleOwner) { deck ->
            deck?.let {
                (activity as? MainActivity)?.openStudyWithDeck(it.id)
            }
        }
    }
}
