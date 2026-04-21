package com.example.flashcardstudy

import android.os.Bundle
import android.view.View
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

var deckId = 0;
var deck_titles = mutableListOf("Biology", "Spanish", "Mathematics", "World History")
var flashcards = mutableListOf(
    mutableListOf(
        Flashcard("What is the powerhouse of the cell?", "Mitochondria"),
        Flashcard("Which molecule carries genetic information?", "DNA"),
        Flashcard("What is the main function of red blood cells?", "Carrying oxygen"),
        Flashcard("What is the term for keeping internal conditions stable?", "Homeostasis"),
        Flashcard("What is the largest organ in the human body?", "Skin")
    ),
    mutableListOf(
        Flashcard("What does “Hola” mean?", "Hello"),
        Flashcard("How do you say “Thank you” in Spanish?", "Gracias"),
        Flashcard("What is the Spanish word for “Water”?", "Agua"),
        Flashcard("How do you say “Good morning” in Spanish?", "Buenos días"),
        Flashcard("What does “Adiós” mean?", "Goodbye")
    ),
    mutableListOf(
        Flashcard("What is 5 + 3?", "8"),
        Flashcard("What is 9 − 4?", "5"),
        Flashcard("What is 6 × 2?", "12"),
        Flashcard("What is 15 ÷ 3?", "5"),
        Flashcard("What is the square of 4?", "16")
    ),
    mutableListOf(
        Flashcard("Who was the first President of the United States?", "George Washington"),
        Flashcard("In which country did the pyramids of Giza originate?", "Egypt"),
        Flashcard("What major event began in 1939?", "World War II"),
        Flashcard("When did the French Revolution begin?", "1789"),
        Flashcard("What wall fell in 1989, symbolizing the end of the Cold War?", "Berlin Wall")
    )
)
class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private val homeFragment = HomeFragment()
    private val newDeckFragment = NewDeckFragment()
    private val addCardFragment = AddCardFragment()
    private val studyFragment = StudyFragment()
    private val calendarFragment = CalendarFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
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
        supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, WelcomeFragment())
            .commit()
    }

    fun onGetStarted() {
        bottomNavigationView.visibility = View.VISIBLE
        bottomNavigationView.selectedItemId = R.id.homeTab
    }

    fun openDeckDetail() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame_layout, DeckDetailFragment()).addToBackStack(null).commit()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, fragment).commit()
    }
}
