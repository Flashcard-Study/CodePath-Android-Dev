package com.example.flashcardstudy.ui.newdeck

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.flashcardstudy.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class NewDeckFragment : Fragment(R.layout.fragment_new_deck) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchPublic = view.findViewById<MaterialSwitch>(R.id.switchPublic)
        val tvLabel = view.findViewById<TextView>(R.id.tvVisibilityLabel)
        val tvSub = view.findViewById<TextView>(R.id.tvVisibilitySub)
        val tilDeckName = view.findViewById<TextInputLayout>(R.id.tilDeckName)
        val etDeckName = view.findViewById<TextInputEditText>(R.id.etDeckName)
        val btnCreate = view.findViewById<MaterialButton>(R.id.btnCreateDeck)

        switchPublic.setOnCheckedChangeListener { _, isChecked ->
            tvLabel.text = if (isChecked) "Public deck" else "Private deck"
            tvSub.text =
                if (isChecked) "Anyone can find this deck" else "Only you can see this deck"
        }

        btnCreate.setOnClickListener {
            val name = etDeckName.text?.toString()?.trim()
            if (name.isNullOrEmpty()) {
                tilDeckName.error = "Please enter a deck name"
            } else {
                tilDeckName.error = null
                Snackbar.make(view, "\"$name\" deck created!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
