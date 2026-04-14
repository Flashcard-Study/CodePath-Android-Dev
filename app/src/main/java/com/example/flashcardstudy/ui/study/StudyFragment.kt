package com.example.flashcardstudy.ui.study

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.flashcardstudy.Flashcard
import com.example.flashcardstudy.R
import com.google.android.material.card.MaterialCardView
import kotlin.math.sqrt

class StudyFragment : Fragment(), SensorEventListener {

    private val flashcards = mutableListOf(
        Flashcard("What is the powerhouse of the cell?", "Mitochondria"),
        Flashcard("Which molecule carries genetic information?", "DNA"),
        Flashcard("What is the main function of red blood cells?", "Carrying oxygen"),
        Flashcard("What is the term for keeping internal conditions stable?", "Homeostasis"),
        Flashcard("What is the largest organ in the human body?", "Skin")
    )

    private var showAnswer = false
    private var index = 0
    private lateinit var cardTV: TextView

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_study_placeholder, container, false)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val card = view.findViewById<MaterialCardView>(R.id.studyCard)
        cardTV = card.findViewById(R.id.cardContent)

        updateCardText()

        card.setOnClickListener {
            if (showAnswer) {
                cardTV.text = flashcards[index].question
            } else {
                cardTV.text = flashcards[index].answer
            }
            showAnswer = !showAnswer
        }

        val gotIt = view.findViewById<Button>(R.id.gotIt)
        gotIt.setOnClickListener {
            index = (index + 1) % flashcards.size
            showAnswer = false
            updateCardText()
        }

        val stillLearning = view.findViewById<Button>(R.id.stillLearning)
        stillLearning.setOnClickListener {
            index = (index + 1) % flashcards.size
            showAnswer = false
            updateCardText()
        }

        return view
    }

    private fun updateCardText() {
        cardTV.text = flashcards[index].question
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

    override fun onStop() {
        super.onStop()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = acceleration - SensorManager.GRAVITY_EARTH

        val currentTime = System.currentTimeMillis()

        if (delta > 8 && currentTime - lastShakeTime > 1000) {
            lastShakeTime = currentTime
            flashcards.shuffle()
            index = 0
            showAnswer = false
            updateCardText()
            Toast.makeText(requireContext(), "Deck shuffled", Toast.LENGTH_SHORT).show()
        }
    }
}