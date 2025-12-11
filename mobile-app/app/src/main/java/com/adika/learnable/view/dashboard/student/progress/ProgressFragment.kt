package com.adika.learnable.view.dashboard.student.progress

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.WeeklyHistoryAdapter
import com.adika.learnable.databinding.FragmentProgressBinding
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.progress.ProgressViewModel
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.yearMonth
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@AndroidEntryPoint
class ProgressFragment : BaseFragment() {
    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProgressViewModel by viewModels()
    private lateinit var historyAdapter: WeeklyHistoryAdapter

    private val monthCalendarView: CalendarView get() = binding.exOneCalendar

    private val activityDates = mutableSetOf<LocalDate>()

    private val today = LocalDate.now()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeader()
        setupRecycler()
        observeViewModel()
        viewModel.loadTodayProgress()
        viewModel.loadRecentHistory()

        val daysOfWeek = daysOfWeek()
        bindLegend(daysOfWeek)

        setupTextScaling()

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)

        setupMonthCalendar(startMonth, endMonth, currentMonth, daysOfWeek)

        updateTitle()
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.tvSeeAll.setOnClickListener {
            findNavController().navigate(R.id.action_progressFragment_to_progressDetailFragment)
        }
        binding.btnLearnNow.setOnClickListener {
            findNavController().navigate(R.id.action_progressFragment_to_subjectListFragment)
        }

        binding.btnPrevMonth.setOnClickListener {
            monthCalendarView.findFirstVisibleMonth()?.let { month ->
                val prevMonth = month.yearMonth.minusMonths(1)
                monthCalendarView.scrollToMonth(prevMonth)
            }
        }

        binding.btnNextMonth.setOnClickListener {
            monthCalendarView.findFirstVisibleMonth()?.let { month ->
                val nextMonth = month.yearMonth.plusMonths(1)
                monthCalendarView.scrollToMonth(nextMonth)
            }
        }
    }

    private fun bindLegend(daysOfWeek: List<DayOfWeek>) {
        val labels = resources.getStringArray(R.array.days_id)
        binding.legendLayout.root.children
            .map { it as TextView }
            .forEachIndexed { index, textView ->
                val label = labels[daysOfWeek[index].value - 1]
                textView.text = label
                textView.setTextColor(requireColor(R.color.grey))
            }
    }

    private fun setupRecycler() {
        historyAdapter = WeeklyHistoryAdapter()
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
            setHasFixedSize(false) // Changed to false for dynamic content
            isNestedScrollingEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProgressViewModel.UiState.Loading -> {
                    showHistoryLoading(true)
                }

                is ProgressViewModel.UiState.TodayEmpty -> {
                    showHistoryLoading(false)
                    binding.emptyState.visibility = View.VISIBLE
                    binding.historySection.visibility = View.GONE
                }

                is ProgressViewModel.UiState.TodayHasActivity -> {
                    showHistoryLoading(false)
                    binding.emptyState.visibility = View.GONE
                    binding.historySection.visibility = View.VISIBLE
                }
            }
        }

        viewModel.history.observe(viewLifecycleOwner) { list ->
            android.util.Log.d("ProgressFragment", "History data received: ${list.size} items")
            list.forEachIndexed { index, weekGroup ->
                android.util.Log.d(
                    "ProgressFragment",
                    "Week $index: ${weekGroup.title} - ${weekGroup.items.size} items"
                )
            }
            showHistoryLoading(false)

            historyAdapter.submitList(list)
            android.util.Log.d(
                "ProgressFragment",
                "Adapter item count after submitList: ${historyAdapter.itemCount}"
            )

            if (list.isNotEmpty()) {
                android.util.Log.d(
                    "ProgressFragment",
                    "Showing history section with ${list.size} weeks"
                )
                binding.historySection.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
            } else {
                android.util.Log.d("ProgressFragment", "No history data, checking today's activity")

                val currentState = viewModel.uiState.value
                if (currentState is ProgressViewModel.UiState.TodayEmpty) {
                    android.util.Log.d("ProgressFragment", "Showing empty state")
                    binding.emptyState.visibility = View.VISIBLE
                    binding.historySection.visibility = View.GONE
                }
            }
        }

        viewModel.activityDates.observe(viewLifecycleOwner) { dates ->
            activityDates.clear()
            activityDates.addAll(dates)

            monthCalendarView.notifyCalendarChanged()
        }
    }

    private fun setupMonthCalendar(
        startMonth: YearMonth,
        endMonth: YearMonth,
        currentMonth: YearMonth,
        daysOfWeek: List<DayOfWeek>,
    ) {
        class DayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: CalendarDay
            val textView: TextView = view.findViewById(R.id.exOneDayText)
        }

        monthCalendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data

                val visibleMonth =
                    monthCalendarView.findFirstVisibleMonth()?.yearMonth ?: currentMonth
                bindDate(
                    date = data.date,
                    textView = container.textView,
                    isSelectable = data.position == DayPosition.MonthDate,
                    currentMonth = visibleMonth
                )
            }
        }

        monthCalendarView.monthScrollListener = {
            updateTitle()

            monthCalendarView.notifyCalendarChanged()
        }
        monthCalendarView.setup(startMonth, endMonth, daysOfWeek.first())
        monthCalendarView.scrollToMonth(currentMonth)
    }


    private fun bindDate(
        date: LocalDate,
        textView: TextView,
        isSelectable: Boolean,
        currentMonth: YearMonth
    ) {
        textView.text = date.dayOfMonth.toString()

        if (date.yearMonth == currentMonth) {
            textView.visibility = View.VISIBLE

            if (isSelectable) {
                when {
                    activityDates.contains(date) -> {

                        textView.setTextColor(requireColor(R.color.white))
                        textView.setBackgroundResource(R.drawable.bg_button_green)
                    }

                    today == date -> {

                        textView.setTextColor(requireColor(R.color.white))
                        textView.setBackgroundResource(R.drawable.bg_button_blue)
                    }

                    else -> {

                        textView.setTextColor(requireColor(R.color.grey))
                        textView.background = null
                    }
                }
            } else {
                textView.setTextColor(requireColor(R.color.grey))
                textView.background = null
            }
        } else {

            textView.visibility = View.INVISIBLE
        }
    }


    @SuppressLint("SetTextI18n")
    private fun updateTitle() {
        val ym = monthCalendarView.findFirstVisibleMonth()?.yearMonth ?: return

        val months = resources.getStringArray(R.array.months_id)
        val monthName = months[ym.monthValue - 1]

        binding.tvYearMonthTitle.text = getString(R.string.month_year_format, monthName, ym.year)
    }

    private fun requireColor(resId: Int): Int =
        ContextCompat.getColor(requireContext(), resId)

    private fun showHistoryLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBarHistory.visibility = View.VISIBLE
            binding.historySection.visibility = View.GONE
            binding.emptyState.visibility = View.GONE
        } else {
            binding.progressBarHistory.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}