package com.example.flashcardstudy

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.flashcardstudy.ui.deckdetail.DeckDetailFragment
import com.example.flashcardstudy.ui.home.HomeFragment
import com.example.flashcardstudy.ui.newdeck.NewDeckFragment
import com.example.flashcardstudy.ui.addcard.AddCardFragment
import com.example.flashcardstudy.ui.study.StudyFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val homeFragment = HomeFragment()
        val newDeckFragment = NewDeckFragment()
        val addCardFragment = AddCardFragment()
        val studyFragment = StudyFragment()

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.homeTab -> homeFragment
                R.id.newDeckTab -> newDeckFragment
                R.id.addCardTab -> addCardFragment
                R.id.studyTab -> studyFragment
                else -> homeFragment
            }
            replaceFragment(fragment)
            true
        }

        bottomNavigationView.selectedItemId = R.id.homeTab
    }

    fun openDeckDetail() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_frame_layout, DeckDetailFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_frame_layout, fragment)
            .commit()
    }
}