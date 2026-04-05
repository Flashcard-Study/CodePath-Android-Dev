package com.example.flashcardstudy.ui.study

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.flashcardstudy.R
import com.google.android.material.card.MaterialCardView

class StudyFragment : Fragment() {
    val question = "What is the powerhouse of the cell?"
    val answer = "Mitochondria"
    var showAnswer = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_study_placeholder, container, false)
        val card = view.findViewById<MaterialCardView>(R.id.studyCard)
        val cardTV = card.findViewById<TextView>(R.id.cardContent)
        cardTV.text = question
        card.setOnClickListener {
            if (showAnswer) {
                cardTV.text = question
            } else {
                cardTV.text = answer
            }
            showAnswer = !showAnswer
        }
        return view
    }

}

