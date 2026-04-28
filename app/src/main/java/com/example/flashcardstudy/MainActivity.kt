package com.example.flashcardstudy

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.content.res.Configuration
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
    private val appPrefs by lazy {
        getSharedPreferences(PREFS_APP_STATE, MODE_PRIVATE)
    }
    private val newDeckFragment = NewDeckFragment()
    private val addCardFragment = AddCardFragment()
    private val studyFragment = StudyFragment()
    private val calendarFragment = CalendarFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR

        RepositoryProvider.initialize(this)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val root = findViewById<View>(R.id.main)
        val content = findViewById<View>(R.id.main_frame_layout)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            content.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val navTopPadding = if (isLandscape) {
                0
            } else {
                resources.getDimensionPixelSize(R.dimen.bottom_nav_inset_top_padding)
            }
            val navBottomPadding = if (isLandscape) {
                0
            } else {
                resources.getDimensionPixelSize(R.dimen.bottom_nav_inset_bottom_padding)
            }
            val bottomInsetForNav = if (isLandscape) 0 else systemBars.bottom
            bottomNavigationView.setPadding(
                systemBars.left,
                navTopPadding,
                systemBars.right,
                navBottomPadding + bottomInsetForNav
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

        if (savedInstanceState == null) {
            if (appPrefs.getBoolean(KEY_HAS_SEEN_WELCOME, false)) {
                bottomNavigationView.visibility = View.VISIBLE
                suppressNavListener = true
                bottomNavigationView.selectedItemId = R.id.homeTab
                suppressNavListener = false
                replaceFragment(HomeFragment())
            } else {
                bottomNavigationView.visibility = View.GONE
                supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, WelcomeFragment())
                    .commit()
            }
        } else {
            restoreHostState(savedInstanceState)
            syncBottomNavWithCurrentFragment(
                fallbackTabId = savedInstanceState.getInt(KEY_SELECTED_TAB_ID, R.id.homeTab),
                fallbackNavVisible = savedInstanceState.getBoolean(KEY_BOTTOM_NAV_VISIBLE, true)
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_ACTIVE_DECK_ID, activeDeckId)
        outState.putString(KEY_ACTIVE_DECK_NAME, activeDeckName)
        outState.putString(KEY_ACTIVE_DECK_COLOR, activeDeckColor)
        outState.putInt(KEY_SELECTED_TAB_ID, bottomNavigationView.selectedItemId)
        outState.putBoolean(KEY_BOTTOM_NAV_VISIBLE, bottomNavigationView.visibility == View.VISIBLE)
    }

    fun onGetStarted() {
        appPrefs.edit().putBoolean(KEY_HAS_SEEN_WELCOME, true).apply()
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

    private fun restoreHostState(savedState: Bundle) {
        activeDeckId = savedState.getString(KEY_ACTIVE_DECK_ID)
        activeDeckName = savedState.getString(KEY_ACTIVE_DECK_NAME)
        activeDeckColor = savedState.getString(KEY_ACTIVE_DECK_COLOR, "#6C63FF")
    }

    private fun syncBottomNavWithCurrentFragment(fallbackTabId: Int, fallbackNavVisible: Boolean) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_frame_layout)
        if (currentFragment is WelcomeFragment) {
            bottomNavigationView.visibility = View.GONE
            return
        }

        bottomNavigationView.visibility = if (fallbackNavVisible) View.VISIBLE else View.GONE
        val selectedTabId = when (currentFragment) {
            is HomeFragment -> R.id.homeTab
            is NewDeckFragment -> R.id.newDeckTab
            is AddCardFragment -> R.id.addCardTab
            is StudyFragment -> R.id.studyTab
            is CalendarFragment -> R.id.calendarTab
            else -> fallbackTabId
        }

        suppressNavListener = true
        bottomNavigationView.selectedItemId = selectedTabId
        suppressNavListener = false
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    companion object {
        private const val PREFS_APP_STATE = "app_state_prefs"
        private const val KEY_HAS_SEEN_WELCOME = "key_has_seen_welcome"
        private const val KEY_ACTIVE_DECK_ID = "key_active_deck_id"
        private const val KEY_ACTIVE_DECK_NAME = "key_active_deck_name"
        private const val KEY_ACTIVE_DECK_COLOR = "key_active_deck_color"
        private const val KEY_SELECTED_TAB_ID = "key_selected_tab_id"
        private const val KEY_BOTTOM_NAV_VISIBLE = "key_bottom_nav_visible"
    }
}
