package com.example.flashcardstudy.ui.calendar

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.R
import com.example.flashcardstudy.data.database.FlashcardDatabaseHelper
import com.example.flashcardstudy.data.database.StudyProgress
import com.example.flashcardstudy.data.repository.RepositoryProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {
    private val repository by lazy { RepositoryProvider.flashcardRepository }

    private var currentMonthCells: List<String> = emptyList()
    private var sessionCountsForMonth: Map<Int, Int> = emptyMap()
    private var weeklySessionCounts: List<Int> = List(7) { 0 }
    private val displayedMonth: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    private val monthFormatter by lazy { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    private lateinit var tvActivityMapTitle: TextView
    private lateinit var prevMonthButton: TextView
    private lateinit var nextMonthButton: TextView
    private lateinit var tvWeekTotal: TextView
    private lateinit var tvStreakCount: TextView
    private lateinit var calendarGrid: RecyclerView
    private lateinit var heatmapLegend: LinearLayout
    private lateinit var barGraph: LinearLayout
    private lateinit var dayLabels: List<TextView>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_calendar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvActivityMapTitle = view.findViewById(R.id.tv_activity_map_title)
        prevMonthButton = view.findViewById(R.id.btn_prev_month)
        nextMonthButton = view.findViewById(R.id.btn_next_month)
        tvWeekTotal = view.findViewById(R.id.tv_week_total)
        tvStreakCount = view.findViewById(R.id.tv_streak_count)
        calendarGrid = view.findViewById(R.id.rv_calendar_grid)
        heatmapLegend = view.findViewById(R.id.ll_heatmap_legend)
        barGraph = view.findViewById(R.id.ll_bar_graph)
        dayLabels = listOf(
            view.findViewById(R.id.tv_label_sun),
            view.findViewById(R.id.tv_label_mon),
            view.findViewById(R.id.tv_label_tue),
            view.findViewById(R.id.tv_label_wed),
            view.findViewById(R.id.tv_label_thu),
            view.findViewById(R.id.tv_label_fri),
            view.findViewById(R.id.tv_label_sat)
        )

        displayedMonth.timeInMillis = Calendar.getInstance().timeInMillis
        displayedMonth.set(Calendar.DAY_OF_MONTH, 1)
        tvActivityMapTitle.text = monthFormatter.format(displayedMonth.time)

        val density = resources.displayMetrics.density
        val primary = ContextCompat.getColor(requireContext(), R.color.sf_accent)
        val outlineVariant = ContextCompat.getColor(requireContext(), R.color.sf_line)

        calendarGrid.layoutManager = GridLayoutManager(requireContext(), 7)
        currentMonthCells = buildMonthCells(displayedMonth)

        prevMonthButton.setOnClickListener { shiftDisplayedMonth(-1) }
        nextMonthButton.setOnClickListener { shiftDisplayedMonth(1) }

        setupHeatmapLegend(
            heatmapLegend,
            primary,
            outlineVariant,
            density
        )

        renderCalendar(displayedMonth, density)
        setupBarGraph(barGraph, weeklySessionCounts)
        loadCalendarData(displayedMonth, density)
    }

    override fun onResume() {
        super.onResume()
        val density = resources.displayMetrics.density
        loadCalendarData(displayedMonth, density)
    }

    private fun loadCalendarData(monthAnchor: Calendar, density: Float) {
        lifecycleScope.launch {
            val selectedMonth = (monthAnchor.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }
            tvActivityMapTitle.text = monthFormatter.format(selectedMonth.time)
            currentMonthCells = buildMonthCells(selectedMonth)

            val allProgress = mutableListOf<StudyProgress>()
            repository.getDecks().forEach { deck ->
                allProgress += repository.getStudyProgressForDeck(deck.id)
            }
            val sessionProgress =
                allProgress.filter { it.status == FlashcardDatabaseHelper.STATUS_SESSION_STARTED }
            val today = Calendar.getInstance()

            sessionCountsForMonth = aggregateMonthSessionCounts(sessionProgress, selectedMonth)
            weeklySessionCounts = aggregateWeeklySessions(sessionProgress, today)
            val weekTotal = weeklySessionCounts.sum()
            val streak = calculateStreak(sessionProgress, today)

            tvWeekTotal.text = "$weekTotal times studied"
            tvStreakCount.text = streak.toString()

            renderCalendar(selectedMonth, density)
            setupBarGraph(barGraph, weeklySessionCounts)
        }
    }

    private fun renderCalendar(monthAnchor: Calendar, density: Float) {
        val primary = ContextCompat.getColor(requireContext(), R.color.sf_accent)
        val onSurface = ContextCompat.getColor(requireContext(), R.color.sf_ink)
        val outlineVariant = ContextCompat.getColor(requireContext(), R.color.sf_line)
        val onPrimary = Color.WHITE
        val today = Calendar.getInstance()
        val highlightedDay = if (
            today.get(Calendar.YEAR) == monthAnchor.get(Calendar.YEAR) &&
            today.get(Calendar.MONTH) == monthAnchor.get(Calendar.MONTH)
        ) {
            today.get(Calendar.DAY_OF_MONTH)
        } else {
            -1
        }

        calendarGrid.adapter = CalendarAdapter(
            currentMonthCells,
            highlightedDay,
            sessionCountsForMonth,
            primary,
            onPrimary,
            onSurface,
            outlineVariant,
            cornerRadius = 6 * density,
            strokeWidth = maxOf(density.toInt(), 1)
        )
    }

    private fun shiftDisplayedMonth(delta: Int) {
        displayedMonth.add(Calendar.MONTH, delta)
        displayedMonth.set(Calendar.DAY_OF_MONTH, 1)
        val density = resources.displayMetrics.density
        loadCalendarData(displayedMonth, density)
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

    private fun setupHeatmapLegend(
        container: LinearLayout,
        primary: Int,
        outline: Int,
        density: Float
    ) {
        val size = (20 * density).toInt()
        val gap = (4 * density).toInt()
        val radius = 4 * density
        container.removeAllViews()
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

    private fun setupBarGraph(container: LinearLayout, sessionCounts: List<Int>) {
        container.post {
            val maxSessions = sessionCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
            val drawablePeak = ContextCompat.getColor(requireContext(), R.color.sf_accent)
            val drawableDefault = "#38F4EFE6".toColorInt()
            val labelPeak = ContextCompat.getColor(requireContext(), R.color.sf_accent)
            val labelDefault = "#88F4EFE6".toColorInt()
            val peakIndex = sessionCounts.indexOfFirst { it == maxSessions && it > 0 }

            sessionCounts.forEachIndexed { index, sessions ->
                val item = container.getChildAt(index)
                val bar = item.findViewById<View>(R.id.v_bar)
                val count = item.findViewById<TextView>(R.id.tv_bar_count)
                val minBarHeight = (8 * resources.displayMetrics.density).toInt()
                val availableHeight =
                    container.height - count.height - (20 * resources.displayMetrics.density).toInt()
                val normalized = sessions.toFloat() / maxSessions.toFloat()
                val barHeight = (availableHeight * normalized).toInt().coerceAtLeast(minBarHeight)

                bar.layoutParams = bar.layoutParams.also { it.height = barHeight }
                count.text = sessions.toString()
                count.alpha = if (sessions == 0) 0.45f else 1f

                val isPeak = sessions == maxSessions && sessions > 0
                val shape = GradientDrawable().apply {
                    setColor(if (isPeak) drawablePeak else drawableDefault)
                    cornerRadii = floatArrayOf(8f, 8f, 8f, 8f, 2f, 2f, 2f, 2f)
                }
                bar.background = shape
            }

            dayLabels.forEachIndexed { index, label ->
                label.setTextColor(if (index == peakIndex) labelPeak else labelDefault)
            }
        }
    }

    private fun aggregateMonthSessionCounts(
        allProgress: List<StudyProgress>,
        monthAnchor: Calendar
    ): Map<Int, Int> {
        val targetYear = monthAnchor.get(Calendar.YEAR)
        val targetMonth = monthAnchor.get(Calendar.MONTH)
        val dayCounts = mutableMapOf<Int, Int>()

        allProgress.forEach { progress ->
            val cal = Calendar.getInstance().apply { timeInMillis = progress.timestamp }
            if (cal.get(Calendar.YEAR) == targetYear && cal.get(Calendar.MONTH) == targetMonth) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                dayCounts[day] = (dayCounts[day] ?: 0) + 1
            }
        }
        return dayCounts
    }

    private fun aggregateWeeklySessions(
        allProgress: List<StudyProgress>,
        today: Calendar
    ): List<Int> {
        val weekStart = (today.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                add(Calendar.DAY_OF_MONTH, -1)
            }
        }

        val dayKeys = (0..6).map { offset ->
            (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
                .let { dayKey(it) }
        }

        val counts = MutableList(7) { 0 }
        allProgress.forEach { progress ->
            val key = dayKey(Calendar.getInstance().apply { timeInMillis = progress.timestamp })
            val index = dayKeys.indexOf(key)
            if (index >= 0) {
                counts[index] = counts[index] + 1
            }
        }

        return counts
    }

    private fun calculateStreak(allProgress: List<StudyProgress>, today: Calendar): Int {
        if (allProgress.isEmpty()) return 0

        val daysWithStudy = allProgress
            .map { progress ->
                dayKey(
                    Calendar.getInstance().apply { timeInMillis = progress.timestamp })
            }
            .toSet()

        var streak = 0
        val cursor = (today.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        while (daysWithStudy.contains(dayKey(cursor))) {
            streak += 1
            cursor.add(Calendar.DAY_OF_MONTH, -1)
        }

        return streak
    }

    private fun dayKey(calendar: Calendar): Int {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        return y * 10000 + m * 100 + d
    }
}

class CalendarAdapter(
    private val cells: List<String>,
    private val highlightedDay: Int,
    private val sessionCounts: Map<Int, Int>,
    private val primaryColor: Int,
    private val onPrimaryColor: Int,
    private val onSurfaceColor: Int,
    private val outlineVariantColor: Int,
    private val cornerRadius: Float,
    private val strokeWidth: Int
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tv_day_number)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder =
        DayViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        )

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val label = cells[position]
        holder.tvDay.text = label

        if (label.isEmpty()) {
            holder.tvDay.background = null
            return
        }

        val day = label.toInt()
        val sessions = sessionCounts[day] ?: 0
        val drawable = GradientDrawable().apply { cornerRadius = this@CalendarAdapter.cornerRadius }

        when {
            highlightedDay > 0 && day == highlightedDay -> {
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

        holder.itemView.setOnClickListener {
            val msg = when (sessions) {
                0 -> "No sessions"
                1 -> "1 session"
                else -> "$sessions sessions"
            }
            val ctx = holder.itemView.context
            val popupView = LayoutInflater.from(ctx).inflate(R.layout.view_day_session, null)
            popupView.findViewById<TextView>(R.id.tv_session_count).text = msg

            val popup = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            popup.elevation = 8 * ctx.resources.displayMetrics.density
            popup.isOutsideTouchable = true

            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val xOff = (holder.itemView.width - popupView.measuredWidth) / 2
            val yOff = -(holder.itemView.height + popupView.measuredHeight)
            popup.showAsDropDown(holder.itemView, xOff, yOff)
        }
    }

    override fun getItemCount() = cells.size
}
