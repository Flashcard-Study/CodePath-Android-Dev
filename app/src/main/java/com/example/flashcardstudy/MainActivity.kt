package com.example.flashcardstudy

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.flashcardstudy.ui.addcard.AddCardFragment
import com.example.flashcardstudy.ui.calendar.CalendarFragment
import com.example.flashcardstudy.ui.deckdetail.DeckDetailFragment
import com.example.flashcardstudy.ui.home.HomeFragment
import com.example.flashcardstudy.ui.newdeck.NewDeckFragment
import com.example.flashcardstudy.ui.study.StudyFragment
import com.example.flashcardstudy.ui.welcome.WelcomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private val homeFragment = HomeFragment()
    private val newDeckFragment = NewDeckFragment()
    private val addCardFragment = AddCardFragment()
    private val studyFragment = StudyFragment()
    private val calendarFragment = CalendarFragment()

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (bottomNavigationView.selectedItemId != R.id.homeTab) {
                bottomNavigationView.selectedItemId = R.id.homeTab
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.homeTab -> homeFragment
                R.id.newDeckTab -> newDeckFragment
                R.id.addCardTab -> addCardFragment
                R.id.studyTab -> studyFragment
                R.id.calendarTab -> calendarFragment
                else -> homeFragment
            }
            replaceFragment(fragment)
            true
        }

        bottomNavigationView.visibility = View.GONE
        supportFragmentManager.beginTransaction().setReorderingAllowed(true)
            .replace(R.id.main_frame_layout, WelcomeFragment()).commit()
    }

    fun onGetStarted() {
        bottomNavigationView.visibility = View.VISIBLE
        bottomNavigationView.selectedItemId = R.id.homeTab
        backPressedCallback.isEnabled = true
    }

    fun openDeckDetail() {
        supportFragmentManager.beginTransaction().setReorderingAllowed(true)
            .replace(R.id.main_frame_layout, DeckDetailFragment()).addToBackStack(null).commit()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().setReorderingAllowed(true)
            .replace(R.id.main_frame_layout, fragment).commit()
    }
}
