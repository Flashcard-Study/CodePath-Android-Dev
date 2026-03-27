package com.example.flashcardstudy.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.flashcardstudy.MainActivity
import com.example.flashcardstudy.R

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.continueCard).setOnClickListener {
            (activity as? MainActivity)?.openDeckDetail()
        }
        view.findViewById<View>(R.id.deckBiology).setOnClickListener {
            (activity as? MainActivity)?.openDeckDetail()
        }
        view.findViewById<View>(R.id.deckSpanish).setOnClickListener {
            (activity as? MainActivity)?.openDeckDetail()
        }
    }
}

