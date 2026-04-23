package com.example.flashcardstudy.ui.calendar

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.Deck
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.database.StudyProgress
import com.example.flashcardstudy.data.repository.RepositoryProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    private lateinit var monthSpinner: Spinner
    private lateinit var deckSpinner: Spinner
    private lateinit var monthTitle: TextView
    private lateinit var calendarGrid: RecyclerView
    private lateinit var heatmapLegend: LinearLayout
    private lateinit var barGraph: LinearLayout

    private val monthOptions = mutableListOf<Calendar>()
    private var decks: List<Deck> = emptyList()
    private var selectedMonthPosition = 0
    private var selectedDeckId: String? = null
    private var restoringFilters = true

    private val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dayFormatter = SimpleDateFormat("MMMM d", Locale.getDefault())

    private val filterPrefs by lazy {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_calendar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        monthSpinner = view.findViewById(R.id.spinner_month_filter)
        deckSpinner = view.findViewById(R.id.spinner_deck_filter)
        monthTitle = view.findViewById(R.id.tv_month_title)
        calendarGrid = view.findViewById(R.id.rv_calendar_grid)
        heatmapLegend = view.findViewById(R.id.ll_heatmap_legend)
        barGraph = view.findViewById(R.id.ll_bar_graph)

        buildMonthOptions()
        setupStaticUi()
        restoreAndLoadFilters(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (::monthSpinner.isInitialized && !restoringFilters) {
            renderCalendar()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_SELECTED_MONTH, monthOptions[selectedMonthPosition].timeInMillis)
        outState.putString(KEY_SELECTED_DECK_ID, selectedDeckId)
    }

    private fun setupStaticUi() {
        val density = resources.displayMetrics.density
        val primary = resolveAttr(android.R.attr.colorPrimary)
        val outlineVariant = resolveAttr(com.google.android.material.R.attr.colorOutlineVariant)

        calendarGrid.layoutManager = GridLayoutManager(requireContext(), 7)
        setupHeatmapLegend(heatmapLegend, primary, outlineVariant, density)
    }

    private fun restoreAndLoadFilters(savedInstanceState: Bundle?) {
        val savedMonthMillis = when {
            savedInstanceState?.containsKey(KEY_SELECTED_MONTH) == true ->
                savedInstanceState.getLong(KEY_SELECTED_MONTH)
            else -> filterPrefs.getLong(KEY_SELECTED_MONTH, monthOptions.first().timeInMillis)
        }

        val savedDeckId = when {
            savedInstanceState?.containsKey(KEY_SELECTED_DECK_ID) == true ->
                savedInstanceState.getString(KEY_SELECTED_DECK_ID)
            else -> filterPrefs.getString(KEY_SELECTED_DECK_ID, null)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            decks = RepositoryProvider.flashcardRepository.getDecks()
            setupMonthSpinner(savedMonthMillis)
            setupDeckSpinner(savedDeckId)
            restoringFilters = false
            renderCalendar()
        }
    }

    private fun setupMonthSpinner(savedMonthMillis: Long) {
        val labels = monthOptions.map { monthFormatter.format(it.time) }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = adapter

        val savedMonth = calendarFromMillis(savedMonthMillis)
        val index = monthOptions.indexOfFirst { sameMonth(it, savedMonth) }
        selectedMonthPosition = if (index >= 0) index else 0

        monthSpinner.setSelection(selectedMonthPosition, false)
        monthTitle.text = labels[selectedMonthPosition]

        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonthPosition = position
                monthTitle.text = labels[position]
                if (!restoringFilters) {
                    persistFilterState()
                    renderCalendar()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDeckSpinner(savedDeckId: String?) {
        val labels = mutableListOf(getString(R.string.calendar_all_decks))
        labels.addAll(decks.map { it.name })

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deckSpinner.adapter = adapter

        val deckIndex = decks.indexOfFirst { it.id == savedDeckId }
        val selection = if (deckIndex >= 0) deckIndex + 1 else 0
        selectedDeckId = if (selection == 0) null else decks[selection - 1].id

        deckSpinner.setSelection(selection, false)

        deckSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDeckId = if (position == 0) null else decks[position - 1].id
                if (!restoringFilters) {
                    persistFilterState()
                    renderCalendar()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun renderCalendar() {
        val selectedMonth = monthOptions[selectedMonthPosition].clone() as Calendar
        monthTitle.text = monthFormatter.format(selectedMonth.time)

        val density = resources.displayMetrics.density
        val primary = resolveAttr(android.R.attr.colorPrimary)
        val onSurface = resolveAttr(com.google.android.material.R.attr.colorOnSurface)
        val outlineVariant = resolveAttr(com.google.android.material.R.attr.colorOutlineVariant)
        val onPrimary =
            if (ColorUtils.calculateLuminance(primary) > 0.35) Color.BLACK else Color.WHITE

        viewLifecycleOwner.lifecycleScope.launch {
            val progressEntries = loadFilteredProgress()
            val sessionCounts = buildMonthSessionCounts(progressEntries, selectedMonth)
            val weekdayCounts = buildWeekdayCounts(progressEntries, selectedMonth)
            val maxWeekdayCount = weekdayCounts.maxOrNull() ?: 0
            val weeklyProgress = if (maxWeekdayCount == 0) {
                List(7) { 0f }
            } else {
                weekdayCounts.map { it.toFloat() / maxWeekdayCount.toFloat() }
            }

            val today = Calendar.getInstance()
            val todayDay = if (sameMonth(selectedMonth, today)) {
                today.get(Calendar.DAY_OF_MONTH)
            } else {
                -1
            }

            calendarGrid.adapter = CalendarAdapter(
                cells = buildMonthCells(selectedMonth),
                displayMonth = selectedMonth,
                todayDay = todayDay,
                sessionCounts = sessionCounts,
                primaryColor = primary,
                onPrimaryColor = onPrimary,
                onSurfaceColor = onSurface,
                outlineVariantColor = outlineVariant,
                cornerRadius = 6 * density,
                strokeWidth = maxOf(density.toInt(), 1),
                dayFormatter = dayFormatter
            )

            setupBarGraph(barGraph, weeklyProgress, weekdayCounts)
        }
    }

    private suspend fun loadFilteredProgress(): List<StudyProgress> {
        val deckIds = if (selectedDeckId == null) {
            decks.map { it.id }
        } else {
            listOfNotNull(selectedDeckId)
        }

        val progressEntries = mutableListOf<StudyProgress>()
        for (deckId in deckIds) {
            progressEntries += RepositoryProvider.flashcardRepository.getStudyProgressForDeck(deckId)
        }
        return progressEntries
    }

    private fun buildMonthCells(calendar: Calendar): List<String> {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val leadingBlanks = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        return buildList {
            repeat(leadingBlanks) { add("") }
            for (day in 1..daysInMonth) add(day.toString())
            while (size % 7 != 0) add("")
        }
    }

    private fun buildMonthSessionCounts(
        progressEntries: List<StudyProgress>,
        selectedMonth: Calendar
    ): Map<Int, Int> {
        val counts = mutableMapOf<Int, Int>()

        progressEntries.forEach { progress ->
            val cal = Calendar.getInstance().apply { timeInMillis = progress.timestamp }
            if (sameMonth(cal, selectedMonth)) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                counts[day] = (counts[day] ?: 0) + 1
            }
        }

        return counts
    }

    private fun buildWeekdayCounts(
        progressEntries: List<StudyProgress>,
        selectedMonth: Calendar
    ): List<Int> {
        val counts = MutableList(7) { 0 }

        progressEntries.forEach { progress ->
            val cal = Calendar.getInstance().apply { timeInMillis = progress.timestamp }
            if (sameMonth(cal, selectedMonth)) {
                val index = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
                counts[index] = counts[index] + 1
            }
        }

        return counts
    }

    private fun setupHeatmapLegend(
        container: LinearLayout,
        primary: Int,
        outline: Int,
        density: Float
    ) {
        container.removeAllViews()

        val size = (20 * density).toInt()
        val gap = (4 * density).toInt()
        val radius = 4 * density

        listOf(0, 64, 128, 191, 255).forEach { alpha ->
            container.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).also { it.marginEnd = gap }
                background = GradientDrawable().apply {
                    cornerRadius = radius
                    if (alpha == 0) {
                        setColor(Color.TRANSPARENT)
                        setStroke(maxOf(density.toInt(), 1), outline)
                    } else {
                        setColor(ColorUtils.setAlphaComponent(primary, alpha))
                    }
                }
            })
        }
    }

    private fun setupBarGraph(
        container: LinearLayout,
        progressValues: List<Float>,
        rawCounts: List<Int>
    ) {
        val weekdayLabels = listOf(
            getString(R.string.day_sun),
            getString(R.string.day_mon),
            getString(R.string.day_tue),
            getString(R.string.day_wed),
            getString(R.string.day_thu),
            getString(R.string.day_fri),
            getString(R.string.day_sat)
        )

        container.post {
            val maxHeight = container.height
            val minVisibleHeight = (4 * resources.displayMetrics.density).toInt()

            progressValues.forEachIndexed { index, progress ->
                val barContainer = container.getChildAt(index)
                val bar = barContainer.findViewById<View>(R.id.v_bar)

                val barHeight = if (progress <= 0f) {
                    0
                } else {
                    maxOf((maxHeight * progress).toInt(), minVisibleHeight)
                }

                bar.layoutParams = bar.layoutParams.also { it.height = barHeight }
                barContainer.contentDescription = getString(
                    R.string.calendar_weekday_sessions,
                    weekdayLabels[index],
                    rawCounts[index]
                )
            }
        }
    }

    private fun buildMonthOptions() {
        monthOptions.clear()

        val current = startOfMonth(Calendar.getInstance())
        repeat(12) {
            monthOptions.add(current.clone() as Calendar)
            current.add(Calendar.MONTH, -1)
        }

        monthOptions.sortByDescending { it.timeInMillis }
    }

    private fun startOfMonth(calendar: Calendar): Calendar {
        return (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun calendarFromMillis(millis: Long): Calendar {
        return Calendar.getInstance().apply { timeInMillis = millis }
    }

    private fun sameMonth(first: Calendar, second: Calendar): Boolean {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
                first.get(Calendar.MONTH) == second.get(Calendar.MONTH)
    }

    private fun persistFilterState() {
        filterPrefs.edit()
            .putLong(KEY_SELECTED_MONTH, monthOptions[selectedMonthPosition].timeInMillis)
            .putString(KEY_SELECTED_DECK_ID, selectedDeckId)
            .apply()
    }

    private fun resolveAttr(attr: Int): Int {
        val tv = TypedValue()
        requireContext().theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    companion object {
        private const val PREFS_NAME = "calendar_filters"
        private const val KEY_SELECTED_MONTH = "selected_month"
        private const val KEY_SELECTED_DECK_ID = "selected_deck_id"
    }
}

class CalendarAdapter(
    private val cells: List<String>,
    private val displayMonth: Calendar,
    private val todayDay: Int,
    private val sessionCounts: Map<Int, Int>,
    private val primaryColor: Int,
    private val onPrimaryColor: Int,
    private val onSurfaceColor: Int,
    private val outlineVariantColor: Int,
    private val cornerRadius: Float,
    private val strokeWidth: Int,
    private val dayFormatter: SimpleDateFormat
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tv_day_number)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        return DayViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        )
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val label = cells[position]
        holder.tvDay.text = label

        if (label.isEmpty()) {
            holder.tvDay.background = null
            holder.tvDay.contentDescription = null
            holder.itemView.setOnClickListener(null)
            return
        }

        val day = label.toInt()
        val sessions = sessionCounts[day] ?: 0
        val drawable = GradientDrawable().apply { cornerRadius = this@CalendarAdapter.cornerRadius }

        when {
            day == todayDay -> {
                drawable.setColor(primaryColor)
                holder.tvDay.setTextColor(onPrimaryColor)
            }

            sessions == 0 -> {
                drawable.setColor(Color.TRANSPARENT)
                drawable.setStroke(strokeWidth, outlineVariantColor)
                holder.tvDay.setTextColor(onSurfaceColor)
            }

            else -> {
                val alpha = minOf(64 * sessions, 255)
                drawable.setColor(ColorUtils.setAlphaComponent(primaryColor, alpha))
                holder.tvDay.setTextColor(if (alpha >= 160) onPrimaryColor else onSurfaceColor)
            }
        }

        holder.tvDay.background = drawable

        val cellCalendar = (displayMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, day)
        }

        val context = holder.itemView.context
        val sessionText = when (sessions) {
            0 -> context.getString(R.string.calendar_no_sessions)
            1 -> context.getString(R.string.calendar_one_session)
            else -> context.getString(R.string.calendar_sessions, sessions)
        }

        holder.tvDay.contentDescription = context.getString(
            R.string.calendar_day_sessions,
            dayFormatter.format(cellCalendar.time),
            sessionText
        )

        holder.itemView.setOnClickListener {
            val popupView = LayoutInflater.from(context).inflate(R.layout.view_day_session, null)
            popupView.findViewById<TextView>(R.id.tv_session_count).text = sessionText

            val popup = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            popup.elevation = 8f * context.resources.displayMetrics.density
            popup.isOutsideTouchable = true

            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val xOff = (holder.itemView.width - popupView.measuredWidth) / 2
            val yOff = -(holder.itemView.height + popupView.measuredHeight)
            popup.showAsDropDown(holder.itemView, xOff, yOff)
        }
    }

    override fun getItemCount() = cells.size
}