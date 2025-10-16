package com.simats.pddmate

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.simats.pddmate.databinding.ActivityDeveloperHomePageBinding
import java.util.*

class DeveloperHomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeveloperHomePageBinding
    private var selectedCalendar: Calendar = Calendar.getInstance()
    private val monthNames = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    private val weekDays = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.titleTextView.text = getString(R.string.pdd_mate)
        binding.menuImageButton.setOnClickListener { showPopupMenu(it) }
        updateMonthYearViews()
        setupWeekDaysHeader()
        setupCalendarRecyclerView()

        binding.prevMonthButton.setOnClickListener {
            selectedCalendar.add(Calendar.MONTH, -1)
            updateMonthYearViews()
            setupCalendarRecyclerView()
        }
        binding.nextMonthButton.setOnClickListener {
            selectedCalendar.add(Calendar.MONTH, 1)
            updateMonthYearViews()
            setupCalendarRecyclerView()
        }
        binding.monthTextView.setOnClickListener { showMonthDropdown() }
        binding.yearTextView.setOnClickListener { showYearDropdown() }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.developer_home_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.menu_dashboard -> {
                    openDashboard()
                    true
                }
                R.id.menu_profile -> {
                    openProfile()
                    true
                }
                R.id.menu_slots -> {
                    openSlots()
                    true
                }
                R.id.menu_verify_progress -> {
                    openVerifyProgress()
                    true
                }
                R.id.menu_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun openDashboard() {
        // Pass the user ID to the dashboard activity
        val intent = Intent(this, DeveloperDashboardActivity::class.java)
        intent.putExtra("USER_ID", intent.getStringExtra("USER_ID"))
        startActivity(intent)
    }

    private fun openProfile() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }

    private fun openSlots() {
        startActivity(Intent(this, SlotsActivity::class.java))
    }

    private fun openVerifyProgress() {
        startActivity(Intent(this, VerifyProgressActivity::class.java))
    }

    private fun logout() {
        startActivity(Intent(this, LogoutActivity::class.java))
    }

    private fun updateMonthYearViews() {
        val month = monthNames[selectedCalendar.get(Calendar.MONTH)]
        val year = selectedCalendar.get(Calendar.YEAR).toString()
        binding.monthTextView.text = month
        binding.yearTextView.text = year
    }

    private fun showMonthDropdown() {
        val popup = PopupMenu(this, binding.monthTextView)
        monthNames.forEachIndexed { i, name ->
            popup.menu.add(0, i, i, name)
        }
        popup.setOnMenuItemClickListener { item ->
            selectedCalendar.set(Calendar.MONTH, item.itemId)
            updateMonthYearViews()
            setupCalendarRecyclerView()
            true
        }
        popup.show()
    }

    private fun showYearDropdown() {
        val currentYear = selectedCalendar.get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear + 5).toList()
        val popup = PopupMenu(this, binding.yearTextView)
        years.forEach { y -> popup.menu.add(0, y, y, y.toString()) }
        popup.setOnMenuItemClickListener { item ->
            selectedCalendar.set(Calendar.YEAR, item.itemId)
            updateMonthYearViews()
            setupCalendarRecyclerView()
            true
        }
        popup.show()
    }

    private fun setupWeekDaysHeader() {
        binding.weekDaysLinearLayout.removeAllViews()
        for (day in weekDays) {
            val tv = TextView(this).apply {
                text = day
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.black, theme))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            binding.weekDaysLinearLayout.addView(tv)
        }
    }

    private fun setupCalendarRecyclerView() {
        val calendar = selectedCalendar.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeekCalendar = calendar.get(Calendar.DAY_OF_WEEK)
        val firstDayOfWeek = if (firstDayOfWeekCalendar == Calendar.SUNDAY) 7 else firstDayOfWeekCalendar - 1
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance()
        val items = mutableListOf<CalendarCell>()
        for (i in 1 until firstDayOfWeek) { items.add(CalendarCell("", false, false)) }
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val isCurrentDate =
                day == today.get(Calendar.DAY_OF_MONTH) &&
                        selectedCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        selectedCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)
            items.add(CalendarCell(day.toString(), true, isCurrentDate))
        }
        val adapter = CalendarAdapter(items)
        binding.calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        binding.calendarRecyclerView.adapter = adapter
    }
}