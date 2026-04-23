package com.example.flashcardstudy.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.MainActivity
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.repository.RepositoryProvider
import kotlinx.coroutines.launch

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

        deckAdapter = DeckAdapter(
            emptyList(),
            onDeckClick = { deck ->
                (activity as? MainActivity)?.setActiveDeck(deck.id, deck.name, deck.color)
                (activity as? MainActivity)?.openDeckDetail(deck.id, deck.name, deck.color)
            },
            onDeckLongClick = { deck ->
                showDeleteDeckDialog(deck)
            }
        )

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
                (activity as? MainActivity)?.openDeckDetail(it.id, it.name, it.color)
            }
        }
    }

    private fun showDeleteDeckDialog(deck: Deck) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete deck?")
            .setMessage("This will remove \"${deck.name}\" and all of its cards.")
            .setPositiveButton("Delete") { dialog, _ ->
                dialog.dismiss()
                lifecycleScope.launch {
                    val deleted = RepositoryProvider.flashcardRepository.deleteDeck(deck.id)
                    if (deleted) {
                        viewModel.loadDecks()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
