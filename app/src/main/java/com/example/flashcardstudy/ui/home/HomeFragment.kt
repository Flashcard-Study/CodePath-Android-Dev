package com.example.flashcardstudy.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.MainActivity
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.repository.RepositoryProvider
import com.example.flashcardstudy.data.transfer.DeckTransferException
import com.example.flashcardstudy.data.transfer.DeckTransferParser
import com.example.flashcardstudy.data.transfer.DeckTransferSchema
import com.example.flashcardstudy.data.transfer.DeckTransferSerializer
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var jumpBackAdapter: JumpBackDeckAdapter
    private lateinit var libraryAdapter: LibraryDeckAdapter
    private var currentDecks: List<Deck> = emptyList()

    private var pendingExportJson: String? = null
    private var pendingExportFileName: String = "flashcard_deck.json"

    private val importDeckJsonLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importDeckFromUri(uri)
        }

    private val saveDeckJsonLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(DeckTransferSchema.MIME_TYPE_JSON)) { uri ->
            if (uri != null) saveExportJsonToUri(uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val onDeckClick: (Deck) -> Unit = { deck ->
            (activity as? MainActivity)?.setActiveDeck(deck.id, deck.name, deck.color)
            (activity as? MainActivity)?.openStudyWithDeck(deck.id)
        }
        val onDeckLongClick: (Deck) -> Unit = { deck ->
            showDeleteDeckDialog(deck)
        }

        jumpBackAdapter = JumpBackDeckAdapter(emptyList(), onDeckClick, onDeckLongClick)
        libraryAdapter = LibraryDeckAdapter(emptyList(), onDeckClick, onDeckLongClick)

        view.findViewById<RecyclerView>(R.id.jumpBackRecycler).apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = jumpBackAdapter
        }

        view.findViewById<RecyclerView>(R.id.libraryRecycler).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = libraryAdapter
        }

        view.findViewById<View>(R.id.btnImport).setOnClickListener {
            importDeckJsonLauncher.launch(arrayOf(DeckTransferSchema.MIME_TYPE_JSON, "text/plain"))
        }
        view.findViewById<View>(R.id.btnExport).setOnClickListener {
            showExportDeckPicker()
        }

        viewModel.decks.observe(viewLifecycleOwner) { uiDecks ->
            currentDecks = uiDecks
            val jumpBackDecks = uiDecks.take(3)
            jumpBackAdapter.updateDecks(jumpBackDecks)
            libraryAdapter.updateDecks(uiDecks)
            view.findViewById<TextView>(R.id.jumpBackCount).text = "${jumpBackDecks.size} ACTIVE"
            view.findViewById<TextView>(R.id.libraryCount).text = "${uiDecks.size} DECKS"
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDecks()
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
                        Toast.makeText(
                            requireContext(),
                            "Deleted \"${deck.name}\".",
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.loadDecks()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Could not delete deck.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun importDeckFromUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val json = requireContext().contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: throw DeckTransferException("Could not read selected file.")

                val payload = DeckTransferParser.parse(json)
                val repository = RepositoryProvider.flashcardRepository
                val existingNames = repository.getDecks().map { it.name }.toSet()
                val resolvedName = resolveUniqueDeckName(payload.deck.name, existingNames)
                val newDeckId = UUID.randomUUID().toString()

                val importedDeck = payload.deck.copy(
                    id = newDeckId,
                    name = resolvedName,
                    cardCount = payload.cards.size
                )
                val importedCards = payload.cards.map { card ->
                    com.example.flashcardstudy.Flashcard(
                        id = UUID.randomUUID().toString(),
                        deckId = newDeckId,
                        question = card.question,
                        answer = card.answer
                    )
                }

                val imported = repository.importDeckWithCards(importedDeck, importedCards)
                if (imported) {
                    Toast.makeText(
                        requireContext(),
                        "Imported \"${importedDeck.name}\" with ${importedCards.size} cards.",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.loadDecks()
                } else {
                    Toast.makeText(requireContext(), "Import failed.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: DeckTransferException) {
                Toast.makeText(
                    requireContext(),
                    e.message ?: "Invalid deck file.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Import failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resolveUniqueDeckName(base: String, existing: Set<String>): String {
        if (base !in existing) return base
        var suffix = 2
        while (true) {
            val candidate = "$base ($suffix)"
            if (candidate !in existing) return candidate
            suffix += 1
        }
    }

    private fun showExportDeckPicker() {
        if (currentDecks.isEmpty()) {
            Toast.makeText(requireContext(), "No decks available to export.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val names = currentDecks.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Choose a deck to export")
            .setItems(names) { _, which ->
                exportCurrentDeck(currentDecks[which].id)
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
                pendingExportFileName = "${sanitizedName}_deck.json"
                pendingExportJson = json
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
}
