package com.example.flashcardstudy.ui.calendar

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.ColorUtils
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
        view.findViewById<TextView>(R.id.tv_month_title).text =
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(today.time)

        val density = resources.displayMetrics.density
        val primary = resolveAttr(android.R.attr.colorPrimary)
        val onSurface = resolveAttr(com.google.android.material.R.attr.colorOnSurface)
        val outlineVariant = resolveAttr(com.google.android.material.R.attr.colorOutlineVariant)
        // Derive contrast color; avoids API-level constraints on colorOnPrimary
        val onPrimary =
            if (ColorUtils.calculateLuminance(primary) > 0.35) Color.BLACK else Color.WHITE

        val cells = buildMonthCells(today)
        view.findViewById<RecyclerView>(R.id.rv_calendar_grid).apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = CalendarAdapter(
                cells,
                today.get(Calendar.DAY_OF_MONTH),
                MOCK_SESSION_COUNTS,
                primary,
                onPrimary,
                onSurface,
                outlineVariant,
                cornerRadius = 6 * density,
                strokeWidth = maxOf(density.toInt(), 1)
            )
        }

        setupHeatmapLegend(
            view.findViewById(R.id.ll_heatmap_legend),
            primary,
            outlineVariant,
            density
        )
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

    private fun setupHeatmapLegend(
        container: LinearLayout,
        primary: Int,
        outline: Int,
        density: Float
    ) {
        val size = (20 * density).toInt()
        val gap = (4 * density).toInt()
        val radius = 4 * density
        listOf(0, 64, 128, 191, 255).forEach { alpha ->
            container.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).also { it.marginEnd = gap }
                background = GradientDrawable().apply {
                    cornerRadius = radius
                    if (alpha == 0) {
                        setColor(Color.TRANSPARENT); setStroke(maxOf(density.toInt(), 1), outline)
                    } else setColor(ColorUtils.setAlphaComponent(primary, alpha))
                }
            })
        }
    }

    private fun setupBarGraph(container: LinearLayout) {
        container.post {
            val maxHeight = container.height
            WEEKLY_PROGRESS.forEachIndexed { i, progress ->
                val bar = container.getChildAt(i).findViewById<View>(R.id.v_bar)
                bar.layoutParams =
                    bar.layoutParams.also { it.height = (maxHeight * progress).toInt() }
            }
        }
    }

    private fun resolveAttr(attr: Int): Int {
        val tv = TypedValue()
        requireContext().theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    companion object {
        // Mock weekly study percentages (Sun–Sat, 0.0–1.0)
        private val WEEKLY_PROGRESS = listOf(0.4f, 0.8f, 0.6f, 0.9f, 0.3f, 0.7f, 0.5f)

        // Mock session counts: day-of-month to sessions
        private val MOCK_SESSION_COUNTS: Map<Int, Int> = mapOf(
            1 to 2,
            2 to 1,
            3 to 0,
            4 to 3,
            5 to 4,
            6 to 2,
            7 to 0,
            8 to 1,
            9 to 3,
            10 to 2,
            11 to 4,
            12 to 1,
            13 to 0,
            14 to 2,
            15 to 3,
            16 to 4,
            17 to 2,
            18 to 1,
            19 to 3,
            20 to 0,
            21 to 4,
            22 to 2,
            23 to 1,
            24 to 3,
            25 to 0,
            26 to 2,
            27 to 4,
            28 to 1,
            29 to 3,
            30 to 2
        )
    }
}

class CalendarAdapter(
    private val cells: List<String>,
    private val todayDay: Int,
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
                // 1 session = 25% alpha … 4+ = 100%
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
