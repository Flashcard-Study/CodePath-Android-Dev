package com.example.flashcardstudy.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardstudy.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_calendar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val today = Calendar.getInstance()
        val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(today.time)
        view.findViewById<TextView>(R.id.tv_month_title).text = monthLabel

        val cells = buildMonthCells(today)
        val todayDay = today.get(Calendar.DAY_OF_MONTH)
        view.findViewById<RecyclerView>(R.id.rv_calendar_grid).apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = CalendarAdapter(cells, todayDay)
        }

        setupBarGraph(view.findViewById(R.id.ll_bar_graph))
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

    private fun setupBarGraph(container: LinearLayout) {
        container.post {
            val maxHeight = container.height
            WEEKLY_PROGRESS.forEachIndexed { i, progress ->
                val barView = container.getChildAt(i).findViewById<View>(R.id.v_bar)
                barView.layoutParams = barView.layoutParams.also {
                    it.height = (maxHeight * progress).toInt()
                }
            }
        }
    }

    companion object {
        // Mock study percentages (0.0 = none, 1.0 = full goal)
        private val WEEKLY_PROGRESS = listOf(0.4f, 0.8f, 0.6f, 0.9f, 0.3f, 0.7f, 0.5f)
    }
}

class CalendarAdapter(
    private val cells: List<String>,
    private val todayDay: Int
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tv_day_number)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder =
        DayViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_calendar_day, parent, false)
        )

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val label = cells[position]
        holder.tvDay.text = label
        holder.tvDay.isSelected = label == todayDay.toString()
    }

    override fun getItemCount() = cells.size
}
