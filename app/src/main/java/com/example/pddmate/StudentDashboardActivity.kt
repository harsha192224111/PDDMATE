package com.example.pddmate

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var studentNameTextView: TextView
    private lateinit var projectNameTextView: TextView
    private lateinit var developerNameTextView: TextView
    private lateinit var projectTypeTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentTextView: TextView
    private lateinit var progressOverTimeLineChart: LineChart
    private lateinit var comparisonLineChart: LineChart
    private lateinit var barChart: BarChart
    private lateinit var deadlinesContainer: LinearLayout

    private lateinit var apiService: ApiService
    private var userId: String? = null
    private var projectId: Int = -1

    // Constants for Milestone Titles
    private val appMilestoneTitles = listOf(
        "Idea Selection", "UI/UX Design", "Frontend Development",
        "Backend Integration", "Testing & Debugging", "Deployment & Maintenance"
    )
    private val productMilestoneTitles = listOf(
        "Idea Selection", "Modelling", "Prototype",
        "Validation", "Integration", "Documentation"
    )

    // Data Models
    data class StudentDashboardResponse(
        val success: Boolean,
        val message: String,
        val project: ProjectData?,
        @SerializedName("completion_rate") val completionRate: Double,
        @SerializedName("milestone_timestamps") val milestoneTimestamps: Map<String, String>?,
        @SerializedName("milestone_counts") val milestoneCounts: Map<String, Int>?,
        @SerializedName("project_timeline") val projectTimeline: ProjectTimeline?,
        @SerializedName("student_milestone_timestamps") val studentMilestoneTimestamps: Map<String, String>?,
        @SerializedName("all_students_progress") val allStudentsProgress: Map<String, Int>?,
        @SerializedName("total_students") val totalStudents: Int?
    )

    data class ProjectData(
        @SerializedName("projectId") val projectId: Int,
        val title: String,
        val type: String,
        @SerializedName("developer_name") val developerName: String
    )

    data class ProjectTimeline(
        @SerializedName("start_date") val startDate: String,
        @SerializedName("end_date") val endDate: String
    )

    // Retrofit Interface
    interface ApiService {
        @FormUrlEncoded
        @POST("get_student_dashboard_data.php")
        fun getStudentDashboardData(@Field("user_id") userId: String): Call<StudentDashboardResponse>
    }

    private val milestoneDetailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (!userId.isNullOrEmpty()) {
                    fetchDashboardData(userId!!)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        // Initialize views
        studentNameTextView = findViewById(R.id.student_name_text_view)
        projectNameTextView = findViewById(R.id.project_name_text_view)
        developerNameTextView = findViewById(R.id.developer_name_text_view)
        projectTypeTextView = findViewById(R.id.project_type_text_view)
        progressBar = findViewById(R.id.progress_bar)
        progressPercentTextView = findViewById(R.id.progress_percent_text)
        progressOverTimeLineChart = findViewById(R.id.progress_over_time_line_chart)
        comparisonLineChart = findViewById(R.id.comparison_line_chart)
        barChart = findViewById(R.id.bar_chart)
        deadlinesContainer = findViewById(R.id.deadlines_container)

        userId = intent.getStringExtra("USER_ID")
            ?: getSharedPreferences("login_session", Context.MODE_PRIVATE)
                .getString("user_id", null)

        // Set up Retrofit
        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.249.231.64/pdd_dashboard/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        apiService = retrofit.create(ApiService::class.java)

        if (!userId.isNullOrEmpty()) {
            val studentName =
                getSharedPreferences("login_session", Context.MODE_PRIVATE).getString("name", getString(R.string.default_student_name))
            studentNameTextView.text = getString(R.string.welcome_student, studentName)
            fetchDashboardData(userId!!)
        } else {
            Toast.makeText(this, getString(R.string.user_id_not_found), Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!userId.isNullOrEmpty()) {
            fetchDashboardData(userId!!)
        }
    }

    private fun fetchDashboardData(userId: String) {
        apiService.getStudentDashboardData(userId).enqueue(object : Callback<StudentDashboardResponse> {
            override fun onResponse(call: Call<StudentDashboardResponse>, response: Response<StudentDashboardResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()
                    val project = data?.project
                    val completionRate = data?.completionRate ?: 0.0
                    val milestoneTimestamps = data?.milestoneTimestamps ?: emptyMap()
                    val milestoneCounts = data?.milestoneCounts ?: emptyMap()
                    val projectTimeline = data?.projectTimeline
                    val studentMilestoneTimestamps = data?.studentMilestoneTimestamps ?: emptyMap()
                    val allStudentsProgress = data?.allStudentsProgress ?: emptyMap()
                    val totalStudents = data?.totalStudents ?: 1

                    if (project != null) {
                        projectNameTextView.text = getString(R.string.project_title, project.title)
                        developerNameTextView.text = getString(R.string.developer_name, project.developerName)
                        projectTypeTextView.text = getString(R.string.project_type, project.type)
                        projectId = project.projectId
                        updateProgressBar(completionRate)
                        setupProgressOverTimeLineChart(milestoneTimestamps, projectTimeline?.startDate, project.type)
                        setupBarChart(milestoneCounts)
                        setupComparisonLineChart(studentMilestoneTimestamps, allStudentsProgress, project.type, totalStudents)
                        setupDeadlinesTable(projectTimeline, project.type)
                    } else {
                        projectNameTextView.text = getString(R.string.project_title_na)
                        developerNameTextView.text = getString(R.string.developer_name_na)
                        projectTypeTextView.text = getString(R.string.project_type_na)
                        updateProgressBar(0.0)
                        progressOverTimeLineChart.clear()
                        barChart.clear()
                        comparisonLineChart.clear()
                        deadlinesContainer.removeAllViews()
                        progressOverTimeLineChart.visibility = View.GONE
                        barChart.visibility = View.GONE
                        comparisonLineChart.visibility = View.GONE
                    }

                } else {
                    Log.e("StudentDashboard", "Failed to load: ${response.body()?.message ?: "Unknown API error"}")
                    Toast.makeText(this@StudentDashboardActivity, getString(R.string.failed_to_load_project_details), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<StudentDashboardResponse>, t: Throwable) {
                Log.e("StudentDashboard", "Network error: ${t.message}", t)
                Toast.makeText(this@StudentDashboardActivity, getString(R.string.network_error, t.message), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun updateProgressBar(rate: Double) {
        val progress = rate.toInt().coerceIn(0, 100)
        progressBar.progress = progress
        progressPercentTextView.text = getString(R.string.progress_percent, progress)
    }

    private fun setupDeadlinesTable(timeline: ProjectTimeline?, projectType: String) {
        deadlinesContainer.removeAllViews()
        if (timeline == null) return

        val inflater = LayoutInflater.from(this)
        val milestoneTitles = if (projectType == "App") appMilestoneTitles else productMilestoneTitles
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        try {
            calendar.time = dateFormat.parse(timeline.startDate)!!
        } catch (e: Exception) {
            Log.e("StudentDashboard", "Date parsing error: ${e.message}")
            return
        }

        val headerRow = inflater.inflate(R.layout.deadline_table_row, deadlinesContainer, false)
        headerRow.findViewById<TextView>(R.id.milestone_name).text = getString(R.string.milestone_column_header)
        headerRow.findViewById<TextView>(R.id.milestone_deadline).text = getString(R.string.deadline_column_header)
        headerRow.setBackgroundColor(ContextCompat.getColor(this, R.color.table_header_bg))
        deadlinesContainer.addView(headerRow)

        val milestonesCount = milestoneTitles.size
        val daysBetween =
            try {
                val start = dateFormat.parse(timeline.startDate)?.time ?: 0L
                val end = dateFormat.parse(timeline.endDate)?.time ?: start
                ((end - start) / (1000 * 60 * 60 * 24)).toInt()
            } catch (e: Exception) {
                180 // fallback: 6 months
            }

        val intervalDays = if (milestonesCount > 1) daysBetween / (milestonesCount - 1) else 30

        for (i in milestoneTitles.indices) {
            val row = inflater.inflate(R.layout.deadline_table_row, deadlinesContainer, false)
            row.findViewById<TextView>(R.id.milestone_name).text = milestoneTitles[i]

            calendar.time = dateFormat.parse(timeline.startDate)!!
            calendar.add(Calendar.DAY_OF_YEAR, intervalDays * i)
            row.findViewById<TextView>(R.id.milestone_deadline).text = dateFormat.format(calendar.time)
            deadlinesContainer.addView(row)
        }
    }

    private fun setupProgressOverTimeLineChart(timestamps: Map<String, String>, startDateString: String?, projectType: String) {
        if (timestamps.isEmpty() || startDateString == null) {
            progressOverTimeLineChart.visibility = View.GONE
            return
        }
        progressOverTimeLineChart.visibility = View.VISIBLE

        val entries = ArrayList<Entry>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val projectStartDate = try {
            startDateFormat.parse(startDateString)
        } catch (e: Exception) {
            null
        }

        val sortedTimestamps = timestamps.entries.sortedBy { it.key.toInt() }

        sortedTimestamps.forEach { entry ->
            val milestoneIndex = entry.key.toInt()
            val timestampDate = dateFormat.parse(entry.value)
            val xValue = milestoneIndex.toFloat() // X-axis is milestone index
            val yValue = if (projectStartDate != null && timestampDate != null) {
                ((timestampDate.time - projectStartDate.time) / (1000 * 60 * 60 * 24)).toFloat() // Y-axis is days
            } else {
                0f
            }
            entries.add(Entry(xValue, yValue))
        }

        val dataSet = LineDataSet(entries, "Days to Complete Milestones").apply {
            color = ContextCompat.getColor(this@StudentDashboardActivity, R.color.colorPrimary)
            valueTextColor = Color.BLACK
            lineWidth = 2f
            circleRadius = 5f
            circleColors = listOf(ContextCompat.getColor(this@StudentDashboardActivity, R.color.colorPrimaryDark))
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(this@StudentDashboardActivity, R.drawable.chart_fill_gradient)
        }

        val lineData = LineData(dataSet)
        progressOverTimeLineChart.data = lineData
        progressOverTimeLineChart.description.isEnabled = false
        progressOverTimeLineChart.setTouchEnabled(true)
        progressOverTimeLineChart.setPinchZoom(true)
        progressOverTimeLineChart.animateY(1000)

        val milestoneLabels = (if (projectType == "App") appMilestoneTitles else productMilestoneTitles)
            .map { it.split(" ").first() }
            .toMutableList()

        // Customize X-axis
        val xAxis = progressOverTimeLineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.BLACK
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.valueFormatter = IndexAxisValueFormatter(milestoneLabels)
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = (milestoneLabels.size - 0.5f).toFloat()
        xAxis.labelRotationAngle = 0f

        // Customize Y-axis
        val leftAxis = progressOverTimeLineChart.axisLeft
        leftAxis.granularity = 1f
        leftAxis.axisMinimum = 0f
        leftAxis.textColor = Color.BLACK
        leftAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()} Days"
            }
        }

        val rightAxis = progressOverTimeLineChart.axisRight
        rightAxis.isEnabled = false

        progressOverTimeLineChart.invalidate()
    }

    private fun setupComparisonLineChart(studentProgress: Map<String, String>, allStudentsProgress: Map<String, Int>, projectType: String, totalStudents: Int) {
        val milestoneTitles = if (projectType == "App") appMilestoneTitles else productMilestoneTitles
        val totalMilestones = milestoneTitles.size

        if (totalMilestones == 0) {
            comparisonLineChart.visibility = View.GONE
            return
        }

        comparisonLineChart.visibility = View.VISIBLE
        val dataSets = ArrayList<LineDataSet>()

        val studentEntries = ArrayList<Entry>()
        val sortedStudentProgress = studentProgress.entries.sortedBy { it.key.toInt() }
        var completedMilestones = 0
        sortedStudentProgress.forEach { entry ->
            val milestoneIndex = entry.key.toFloat()
            completedMilestones++
            studentEntries.add(Entry(milestoneIndex, completedMilestones.toFloat()))
        }

        val studentDataSet = LineDataSet(studentEntries, "My Progress").apply {
            color = ContextCompat.getColor(this@StudentDashboardActivity, R.color.colorPrimary)
            valueTextColor = Color.BLACK
            lineWidth = 2f
            circleRadius = 5f
            circleColors = listOf(ContextCompat.getColor(this@StudentDashboardActivity, R.color.colorPrimaryDark))
        }
        dataSets.add(studentDataSet)

        val averageEntries = ArrayList<Entry>()
        var cumulativeAccepted = 0
        for (i in 0 until totalMilestones) {
            val count = allStudentsProgress[i.toString()] ?: 0
            cumulativeAccepted += count
            val averageProgress = if (totalStudents > 0) cumulativeAccepted.toFloat() / totalStudents.toFloat() else 0f
            averageEntries.add(Entry(i.toFloat(), averageProgress))
        }

        val averageDataSet = LineDataSet(averageEntries, "Overall Average Progress").apply {
            color = ContextCompat.getColor(this@StudentDashboardActivity, R.color.rejected_red)
            valueTextColor = Color.BLACK
            lineWidth = 2f
            circleRadius = 5f
            circleColors = listOf(ContextCompat.getColor(this@StudentDashboardActivity, R.color.rejected_red))
            enableDashedLine(10f, 5f, 0f)
        }
        dataSets.add(averageDataSet)

        val lineData = LineData(dataSets.toList())
        comparisonLineChart.data = lineData
        comparisonLineChart.description.isEnabled = false
        comparisonLineChart.setTouchEnabled(true)
        comparisonLineChart.setPinchZoom(true)
        comparisonLineChart.animateY(1000)

        val labels = milestoneTitles.map { it.split(" ").first() }

        val xAxis = comparisonLineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.BLACK
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = (labels.size - 0.5f).toFloat()

        val leftAxis = comparisonLineChart.axisLeft
        leftAxis.granularity = 1f
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = totalMilestones.toFloat() + 1f
        leftAxis.textColor = Color.BLACK
        leftAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }
        leftAxis.setDrawGridLines(false)

        val rightAxis = comparisonLineChart.axisRight
        rightAxis.isEnabled = false

        val legend = comparisonLineChart.legend
        legend.isEnabled = true
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.orientation = Legend.LegendOrientation.HORIZONTAL

        comparisonLineChart.invalidate()
    }

    private fun setupBarChart(counts: Map<String, Int>) {
        if (counts.isEmpty()) {
            barChart.visibility = View.GONE
            return
        }
        barChart.visibility = View.VISIBLE

        val barEntries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val colors = ArrayList<Int>()

        val statuses = listOf("accepted", "pending", "rejected", "not_submitted")
        statuses.forEachIndexed { index, status ->
            val count = counts[status] ?: 0
            barEntries.add(BarEntry(index.toFloat(), count.toFloat()))
            labels.add(status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
            when (status) {
                "accepted" -> colors.add(ContextCompat.getColor(this, R.color.success_green))
                "pending" -> colors.add(ContextCompat.getColor(this, R.color.pending_yellow))
                "rejected" -> colors.add(ContextCompat.getColor(this, R.color.rejected_red))
                else -> colors.add(ContextCompat.getColor(this, R.color.text_light))
            }
        }

        val dataSet = BarDataSet(barEntries, "Milestone Status Distribution").apply {
            this.colors = colors
            valueTextColor = Color.BLACK
        }

        val barData = BarData(dataSet)
        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.animateY(600)
        barChart.setFitBars(true)

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.BLACK

        val leftAxis = barChart.axisLeft
        leftAxis.granularity = 1f
        leftAxis.axisMinimum = 0f
        leftAxis.textColor = Color.BLACK
        leftAxis.setDrawGridLines(false)

        barChart.axisRight.isEnabled = false

        barChart.invalidate()
    }
}