package com.example.flashcardstudy.ui.study

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.MainActivity
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.database.DatabaseVerifier
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs
import kotlin.math.sqrt

class StudyFragment : Fragment(), SensorEventListener {
    private val viewModel: StudyViewModel by viewModels()
    private var flashcards = mutableListOf<Flashcard>()
    private var currentIndex = 0
    private var showAnswer = false

    private lateinit var card: MaterialCardView
    private lateinit var cardTV: TextView
    private lateinit var cardLabel: TextView
    private lateinit var revealButton: MaterialButton
    private lateinit var gotItButton: MaterialButton
    private lateinit var stillLearningButton: MaterialButton
    private lateinit var closeButton: ImageButton

    private lateinit var totalCardsCount: TextView
    private lateinit var masteredCount: TextView
    private lateinit var learningCount: TextView
    private lateinit var progressCount: TextView
    private lateinit var progressSegments: LinearLayout

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L
    private var swipeStartX = 0f
    private var isSwiping = false
    private var isSwipeAnimating = false
    private var hasRecordedSessionForOpen = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_study_placeholder, container, false)

        card = view.findViewById(R.id.studyCard)
        cardTV = card.findViewById(R.id.cardContent)
        cardLabel = view.findViewById(R.id.cardLabel)
        revealButton = view.findViewById(R.id.revealButton)
        gotItButton = view.findViewById(R.id.gotIt)
        stillLearningButton = view.findViewById(R.id.stillLearning)
        closeButton = view.findViewById(R.id.closeStudyButton)
        progressCount = view.findViewById(R.id.progressCount)
        progressSegments = view.findViewById(R.id.progressSegments)

        totalCardsCount = view.findViewById(R.id.totalCardsCount)
        masteredCount = view.findViewById(R.id.masteredCount)
        learningCount = view.findViewById(R.id.learningCount)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        closeButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToHome()
        }

        setupObservers()
        setupClickListeners()
        setupSwipeGesture()

        arguments?.getString(ARG_DECK_ID)?.let { deckId ->
            Log.d("StudyFragment", "Loading deck from arguments: $deckId")
            viewModel.loadDeckFlashcards(deckId)
        } ?: run {
            Log.d("StudyFragment", "No deck ID in arguments")
        }

        return view
    }

    private fun setupObservers() {
        viewModel.flashcards.observe(viewLifecycleOwner) { cards ->
            flashcards = cards.toMutableList()
            if (cards.isNotEmpty()) {
                currentIndex = 0
                if (!hasRecordedSessionForOpen) {
                    viewModel.recordSession(cards.first().id)
                    hasRecordedSessionForOpen = true
                }
                displayCard()
            } else {
                cardLabel.text = getString(R.string.study_empty_title)
                cardTV.text = getString(R.string.study_empty_message)
                revealButton.isEnabled = false
            }
        }

        viewModel.studyStats.observe(viewLifecycleOwner) { stats ->
            totalCardsCount.text = stats.totalCards.toString()
            masteredCount.text = stats.masteredCards.toString()
            learningCount.text = stats.learningCards.toString()
            renderProgressSegments(stats.masteredCards, stats.learningCards, stats.totalCards)
        }
    }

    private fun setupClickListeners() {
        fun revealOrHideCard() {
            if (flashcards.isEmpty()) return
            showAnswer = !showAnswer
            displayCard()
        }

        revealButton.setOnClickListener { revealOrHideCard() }

        gotItButton.setOnClickListener {
            gradeCurrentCard("got_it")
        }

        stillLearningButton.setOnClickListener {
            gradeCurrentCard("still_learning")
        }
    }

    private fun setupSwipeGesture() {
        val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop

        card.setOnClickListener {
            if (flashcards.isEmpty() || isSwipeAnimating) return@setOnClickListener
            showAnswer = !showAnswer
            displayCard()
        }

        card.setOnTouchListener { touchedView, event ->
            if (isSwipeAnimating || flashcards.isEmpty()) return@setOnTouchListener true

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    swipeStartX = event.rawX
                    isSwiping = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - swipeStartX
                    if (!isSwiping && abs(dx) > touchSlop) {
                        isSwiping = true
                    }
                    if (isSwiping) {
                        card.translationX = dx
                        card.rotation = (dx / card.width.coerceAtLeast(1).toFloat()) * 10f
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dx = event.rawX - swipeStartX
                    if (abs(dx) >= SWIPE_THRESHOLD) {
                        val direction = if (dx >= 0f) 1 else -1
                        val status = if (direction > 0) "got_it" else "still_learning"
                        gradeCurrentCard(status, animateOut = true, direction = direction)
                    } else {
                        val wasSwipe = isSwiping
                        card.animate()
                            .translationX(0f)
                            .rotation(0f)
                            .setDuration(120)
                            .withEndAction {
                                if (!wasSwipe && event.actionMasked == MotionEvent.ACTION_UP) {
                                    touchedView.performClick()
                                }
                            }
                            .start()
                    }
                    isSwiping = false
                    true
                }

                else -> false
            }
        }
    }

    private fun gradeCurrentCard(status: String, animateOut: Boolean = false, direction: Int = 1) {
        if (flashcards.isEmpty()) return

        val currentCard = flashcards[currentIndex]
        Log.d("StudyFragment", "Grade=$status for card: ${currentCard.id}")
        viewModel.recordGrade(currentCard.id, status)
        DatabaseVerifier.verifyStudyProgress(requireContext())

        if (animateOut) {
            isSwipeAnimating = true
            card.animate()
                .translationX(direction * card.width.coerceAtLeast(1) * 1.2f)
                .rotation(direction * 14f)
                .alpha(0f)
                .setDuration(220)
                .withEndAction {
                    card.translationX = 0f
                    card.rotation = 0f
                    card.alpha = 1f
                    isSwipeAnimating = false
                    advanceToNextCard()
                }
                .start()
        } else {
            advanceToNextCard()
        }
    }

    private fun advanceToNextCard() {
        currentIndex = (currentIndex + 1) % flashcards.size
        showAnswer = false
        displayCard()
    }

    private fun displayCard() {
        if (flashcards.isNotEmpty()) {
            if (showAnswer) {
                cardLabel.text = getString(R.string.study_answer_label)
                cardTV.text = flashcards[currentIndex].answer
                revealButton.text = getString(R.string.study_show_question)
            } else {
                cardLabel.text = getString(R.string.study_question_label)
                cardTV.text = flashcards[currentIndex].question
                revealButton.text = getString(R.string.study_reveal_answer)
            }
        }
    }

    private fun renderProgressSegments(mastered: Int, learning: Int, total: Int) {
        val safeTotal = total.coerceAtLeast(1)
        progressCount.text = "${mastered + learning}/$safeTotal"

        progressSegments.removeAllViews()
        val segmentMargin = (2 * resources.displayMetrics.density).toInt()
        val segmentHeight = (4 * resources.displayMetrics.density).toInt()

        repeat(safeTotal) { index ->
            val segment = View(requireContext())
            val params = LinearLayout.LayoutParams(0, segmentHeight, 1f).apply {
                marginEnd = if (index == safeTotal - 1) 0 else segmentMargin
            }
            segment.layoutParams = params
            val color = when {
                index < mastered -> requireContext().getColor(R.color.sf_green)
                index < mastered + learning -> requireContext().getColor(R.color.sf_accent)
                else -> requireContext().getColor(R.color.sf_line)
            }
            segment.setBackgroundColor(color)
            progressSegments.addView(segment)
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER || flashcards.isEmpty()) {
            return
        }

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = acceleration - SensorManager.GRAVITY_EARTH
        val now = System.currentTimeMillis()

        if (delta > SHAKE_THRESHOLD && now - lastShakeTime > SHAKE_COOLDOWN_MS) {
            lastShakeTime = now
            flashcards.shuffle()
            currentIndex = 0
            showAnswer = false
            displayCard()
            Log.d("StudyFragment", "Deck shuffled by shake")
        }
    }

    companion object {
        private const val ARG_DECK_ID = "deck_id"
        private const val SHAKE_THRESHOLD = 8f
        private const val SHAKE_COOLDOWN_MS = 1000L
        private const val SWIPE_THRESHOLD = 90f

        fun newInstance(deckId: String): StudyFragment {
            return StudyFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DECK_ID, deckId)
                }
            }
        }
    }
}
