package com.example.flashcardstudy.ui.newdeck

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.flashcardstudy.R

class NewDeckFragment : Fragment(R.layout.fragment_tab_placeholder) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.placeholderText).text = "New Deck"
    }
}
