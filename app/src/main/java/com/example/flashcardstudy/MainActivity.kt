package com.example.flashcardstudy

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.flashcardstudy.data.repository.RepositoryProvider
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
    private var suppressNavListener = false
    private var activeDeckId: String? = null
    private var activeDeckName: String? = null
    private var activeDeckColor: String = "#6C63FF"
    private val newDeckFragment = NewDeckFragment()
    private val addCardFragment = AddCardFragment()
    private val studyFragment = StudyFragment()
    private val calendarFragment = CalendarFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RepositoryProvider.initialize(this)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val root = findViewById<View>(R.id.main)
        val content = findViewById<View>(R.id.main_frame_layout)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            content.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            bottomNavigationView.setPadding(
                systemBars.left,
                dpToPx(6),
                systemBars.right,
                dpToPx(8) + systemBars.bottom
            )
            insets
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            if (!suppressNavListener) {
                val fragment: Fragment = when (item.itemId) {
                    R.id.homeTab -> HomeFragment()
                    R.id.newDeckTab -> newDeckFragment
                    R.id.addCardTab -> {
                        val id = activeDeckId
                        val name = activeDeckName
                        if (id != null && name != null) {
                            AddCardFragment.newInstance(id, name, activeDeckColor)
                        } else {
                            addCardFragment
                        }
                    }

                    R.id.studyTab -> studyFragment
                    R.id.calendarTab -> calendarFragment
                    else -> HomeFragment()
                }
                replaceFragment(fragment)
            }
            true
        }

        bottomNavigationView.visibility = View.GONE
        supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, WelcomeFragment())
            .commit()
    }

    fun onGetStarted() {
        bottomNavigationView.visibility = View.VISIBLE
        bottomNavigationView.selectedItemId = R.id.homeTab
    }

    fun navigateToHome() {
        bottomNavigationView.selectedItemId = R.id.homeTab
    }

    fun setActiveDeck(deckId: String, deckName: String, deckColor: String = "#6C63FF") {
        activeDeckId = deckId
        activeDeckName = deckName
        activeDeckColor = deckColor
    }

    fun openAddCardForDeck(deckId: String, deckName: String, deckColor: String = "#6C63FF") {
        val fragment = AddCardFragment.newInstance(deckId, deckName, deckColor)
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame_layout, fragment)
            .addToBackStack(null)
            .commit()
        suppressNavListener = true
        bottomNavigationView.selectedItemId = R.id.addCardTab
        suppressNavListener = false
    }

    fun openDeckDetail(deckId: String, deckName: String, deckColor: String = "#6C63FF") {
        val fragment = DeckDetailFragment.newInstance(deckId, deckName, deckColor)
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame_layout, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun openStudyWithDeck(deckId: String) {
        val newStudyFragment = StudyFragment.newInstance(deckId)

        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame_layout, newStudyFragment)
            .commit()

        suppressNavListener = true
        bottomNavigationView.selectedItemId = R.id.studyTab
        suppressNavListener = false
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, fragment).commit()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
