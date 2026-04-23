package com.example.flashcardstudy.ui.deckdetail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.MainActivity
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.repository.RepositoryProvider
import com.example.flashcardstudy.data.transfer.DeckTransferSchema
import com.example.flashcardstudy.data.transfer.DeckTransferSerializer
import kotlinx.coroutines.launch
import java.io.File

class DeckDetailFragment : Fragment() {
    private var deckId: String? = null
    private var deckName: String? = null
    private var deckColor: String = "#6C63FF"
    private lateinit var adapter: DeckDetailAdapter
    private var pendingExportJson: String? = null
    private var pendingExportFileName: String = "flashcard_deck.json"
    private var currentDeckId: String? = null
    private lateinit var emptyView: TextView

    private val saveDeckJsonLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(DeckTransferSchema.MIME_TYPE_JSON)) { uri ->
            if (uri != null) {
                saveExportJsonToUri(uri)
            }
        }

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
        emptyView = view.findViewById(R.id.emptyState)
        val cardsView = view.findViewById<RecyclerView>(R.id.deckRecyclerView)
        val addCardButton = view.findViewById<Button>(R.id.addCardButton)
        val studyButton = view.findViewById<Button>(R.id.studyDeckButton)
        val exportButton = view.findViewById<Button>(R.id.exportDeckButton)

        titleView.text = deckName ?: "Deck Detail"
        adapter = DeckDetailAdapter(emptyList()) { card ->
            showDeleteCardDialog(card)
        }
        cardsView.layoutManager = LinearLayoutManager(requireContext())
        cardsView.adapter = adapter

        currentDeckId = deckId
        val deckIdValue = currentDeckId ?: run {
            emptyView.visibility = View.VISIBLE
            emptyView.text = "No deck selected"
            return
        }

        addCardButton.setOnClickListener {
            (activity as? MainActivity)?.openAddCardForDeck(
                deckId = deckIdValue,
                deckName = deckName ?: "Deck",
                deckColor = deckColor
            )
        }

        studyButton.setOnClickListener {
            (activity as? MainActivity)?.openStudyWithDeck(deckIdValue)
        }
        exportButton.setOnClickListener {
            exportCurrentDeck(deckIdValue)
        }

        loadCards(deckIdValue)
    }

    private fun loadCards(deckId: String) {
        lifecycleScope.launch {
            val cards = RepositoryProvider.flashcardRepository.getFlashcardsForDeck(deckId)
            adapter.submitCards(cards)
            emptyView.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showDeleteCardDialog(card: com.example.flashcardstudy.Flashcard) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete card?")
            .setMessage("This will remove this flashcard from the deck.")
            .setPositiveButton("Delete") { dialog, _ ->
                dialog.dismiss()
                lifecycleScope.launch {
                    val deleted = RepositoryProvider.flashcardRepository.deleteFlashcard(card.id)
                    if (deleted) {
                        currentDeckId?.let { loadCards(it) }
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun exportCurrentDeck(deckId: String) {
        lifecycleScope.launch {
            try {
                val repository = RepositoryProvider.flashcardRepository
                val deck = repository.getDecks().firstOrNull { it.id == deckId }
                if (deck == null) {
                    Toast.makeText(requireContext(), "Deck not found.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val cards = repository.getFlashcardsForDeck(deckId)
                val json = DeckTransferSerializer.toJson(deck, cards)
                val sanitizedName = deck.name
                    .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    .trim('_')
                    .ifBlank { "deck" }
                val exportFileName = "${sanitizedName}_deck.json"

                pendingExportJson = json
                pendingExportFileName = exportFileName
                showExportOptions()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Failed to export deck.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun showExportOptions() {
        AlertDialog.Builder(requireContext())
            .setTitle("Export deck")
            .setItems(arrayOf("Save JSON file", "Share JSON file")) { _, which ->
                when (which) {
                    0 -> saveDeckJsonLauncher.launch(pendingExportFileName)
                    1 -> shareExportJson()
                }
            }
            .show()
    }

    private fun saveExportJsonToUri(uri: Uri) {
        val json = pendingExportJson ?: return
        try {
            val outputStream = requireContext().contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Could not open output stream.")
            outputStream.bufferedWriter().use { it.write(json) }
            Toast.makeText(requireContext(), "Deck export saved.", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Failed to save deck export.", Toast.LENGTH_SHORT)
                .show()
        } finally {
            pendingExportJson = null
        }
    }

    private fun shareExportJson() {
        val json = pendingExportJson ?: return
        try {
            val exportDir = File(requireContext().cacheDir, "exports").apply { mkdirs() }
            val outputFile = File(exportDir, pendingExportFileName)
            outputFile.writeText(json)

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                outputFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = DeckTransferSchema.MIME_TYPE_JSON
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share deck export"))
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Failed to share deck export.", Toast.LENGTH_SHORT)
                .show()
        } finally {
            pendingExportJson = null
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
