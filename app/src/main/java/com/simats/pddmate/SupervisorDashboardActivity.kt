package com.simats.pddmate

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import java.io.IOException
import java.text.DecimalFormat

class SupervisorDashboardActivity : AppCompatActivity() {

    //region Views
    private lateinit var totalStudentsTextView: TextView
    private lateinit var totalDevelopersTextView: TextView
    private lateinit var completionRateTextView: TextView
    private lateinit var pendingReviewsTextView: TextView
    private lateinit var laggingStudentsTextView: TextView

    private lateinit var appProductEnrollmentChart: PieChart
    private lateinit var submissionStatusChart: PieChart // NEW
    private lateinit var phasePerformanceChart: BarChart
    private lateinit var progressTrendChart: LineChart
    private lateinit var milestoneCompletionOverviewChart: BarChart // NEW
    private lateinit var projectCapacityChart: BarChart
    private lateinit var studentPerformanceChart: BarChart
    //endregion

    //region API & Data Models
    private lateinit var apiService: ApiService

    // Update the data class
    data class SupervisorDashboardResponse(
        val success: Boolean,
        @SerializedName("overall_metrics") val overallMetrics: OverallMetrics,
        @SerializedName("app_product_enrollment") val appProductEnrollment: Map<String, Int>,
        @SerializedName("phase_performance") val phasePerformance: List<PhasePerformanceData>,
        @SerializedName("progress_trend") val progressTrend: ProgressTrend,
        @SerializedName("project_capacity") val projectCapacity: List<ProjectCapacityData>,
        @SerializedName("student_performance_data") val studentPerformanceData: List<LaggingStudentData>,
        @SerializedName("submission_status_breakdown") val submissionStatusBreakdown: Map<String, Int>, // NEW
        @SerializedName("milestone_completion_overview") val milestoneCompletionOverview: List<MilestoneCompletionOverviewData> // NEW
    )

    data class OverallMetrics(
        @SerializedName("total_students") val totalStudents: Int,
        @SerializedName("total_developers") val totalDevelopers: Int,
        @SerializedName("completion_rate") val completionRate: Float,
        @SerializedName("pending_reviews") val pendingReviews: Int,
        @SerializedName("lagging_students") val laggingStudents: Int
    )

    data class PhasePerformanceData(
        @SerializedName("milestone_index") val milestoneIndex: Int,
        val phase: String,
        val count: Int
    )

    data class ProgressTrend(
        @SerializedName("App") val app: Map<String, Int>,
        @SerializedName("Product") val product: Map<String, Int>
    )

    data class ProjectCapacityData(
        @SerializedName("project_title") val projectTitle: String,
        val capacity: Int,
        @SerializedName("enrolled_count") val enrolledCount: Int
    )

    data class LaggingStudentData(
        @SerializedName("student_name") val studentName: String,
        @SerializedName("user_id") val userId: String,
        @SerializedName("progress_percent") val progressPercent: Float
    )

    // New data classes for the new charts
    data class MilestoneCompletionOverviewData(
        @SerializedName("project_title") val projectTitle: String,
        val milestones: List<MilestoneStatusData>
    )

    data class MilestoneStatusData(
        val accepted: Int,
        val pending: Int,
        val rejected: Int,
        @SerializedName("not_submitted") val notSubmitted: Int
    )

    interface ApiService {
        @POST("get_supervisor_dashboard_data.php")
        fun getSupervisorDashboardData(): Call<SupervisorDashboardResponse>
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor_dashboard)

        val cardTotalStudents = findViewById<View>(R.id.card_total_students)
        val cardTotalDevelopers = findViewById<View>(R.id.card_total_developers)
        val cardCompletionRate = findViewById<View>(R.id.card_completion_rate)
        val cardPendingReviews = findViewById<View>(R.id.card_pending_reviews)
        val cardLaggingStudents = findViewById<View>(R.id.card_lagging_students)

        totalStudentsTextView = cardTotalStudents.findViewById<TextView>(R.id.metric_value_text_view)
        totalDevelopersTextView = cardTotalDevelopers.findViewById<TextView>(R.id.metric_value_text_view)
        completionRateTextView = cardCompletionRate.findViewById<TextView>(R.id.metric_value_text_view)
        pendingReviewsTextView = cardPendingReviews.findViewById<TextView>(R.id.metric_value_text_view)
        laggingStudentsTextView = cardLaggingStudents.findViewById<TextView>(R.id.metric_value_text_view)

        cardTotalStudents.findViewById<TextView>(R.id.metric_title_text_view).text = getString(R.string.total_students)
        cardTotalDevelopers.findViewById<TextView>(R.id.metric_title_text_view).text = getString(R.string.total_developers)
        cardCompletionRate.findViewById<TextView>(R.id.metric_title_text_view).text = getString(R.string.completion_rate)
        cardPendingReviews.findViewById<TextView>(R.id.metric_title_text_view).text = getString(R.string.pending_reviews)
        cardLaggingStudents.findViewById<TextView>(R.id.metric_title_text_view).text = getString(R.string.lagging_students)

        appProductEnrollmentChart = findViewById(R.id.app_product_enrollment_chart)
        submissionStatusChart = findViewById(R.id.submission_status_chart) // NEW
        phasePerformanceChart = findViewById(R.id.phase_performance_chart)
        progressTrendChart = findViewById(R.id.progress_trend_chart)
        milestoneCompletionOverviewChart = findViewById(R.id.milestone_completion_overview_chart) // NEW
        projectCapacityChart = findViewById(R.id.project_capacity_chart)
        studentPerformanceChart = findViewById(R.id.lagging_students_chart)

        // Set up Retrofit
        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://14.139.187.229:8081/pddmate/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        apiService = retrofit.create(ApiService::class.java)

        fetchDashboardData()
    }

    private fun fetchDashboardData() {
        apiService.getSupervisorDashboardData().enqueue(object : Callback<SupervisorDashboardResponse> {
            override fun onResponse(call: Call<SupervisorDashboardResponse>, response: Response<SupervisorDashboardResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()
                    data?.let {
                        updateOverallMetrics(it.overallMetrics)
                        setupAppProductEnrollmentChart(it.appProductEnrollment)
                        setupSubmissionStatusChart(it.submissionStatusBreakdown) // NEW
                        setupPhasePerformanceChart(it.phasePerformance)
                        setupProgressTrendChart(it.progressTrend)
                        setupMilestoneCompletionOverviewChart(it.milestoneCompletionOverview) // NEW
                        setupProjectCapacityChart(it.projectCapacity)
                        setupStudentPerformanceChart(it.studentPerformanceData)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e("SupervisorDashboard", "Failed to load: ${response.code()} - $errorBody")
                    Toast.makeText(this@SupervisorDashboardActivity, getString(R.string.failed_to_load_dashboard_data), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SupervisorDashboardResponse>, t: Throwable) {
                Log.e("SupervisorDashboard", "Network error: ${t.message}", t)
                val displayMessage = when {
                    t is IOException -> "Network connection failed. Please check your internet and try again."
                    else -> getString(R.string.network_error, t.message ?: "Unknown")
                }
                Toast.makeText(this@SupervisorDashboardActivity, displayMessage, Toast.LENGTH_LONG).show()
            }
        })
    }

    //region Chart and Data Functions
    private fun updateOverallMetrics(metrics: OverallMetrics) {
        totalStudentsTextView.text = metrics.totalStudents.toString()
        totalDevelopersTextView.text = metrics.totalDevelopers.toString()
        completionRateTextView.text = "${metrics.completionRate}%"
        pendingReviewsTextView.text = metrics.pendingReviews.toString()
        laggingStudentsTextView.text = metrics.laggingStudents.toString()
    }

    private fun setupAppProductEnrollmentChart(enrollment: Map<String, Int>) {
        val entries = ArrayList<PieEntry>()
        enrollment.forEach { (type, count) ->
            if (count > 0) {
                entries.add(PieEntry(count.toFloat(), type))
            }
        }

        if (entries.isEmpty()) {
            appProductEnrollmentChart.visibility = View.GONE
            return
        }
        appProductEnrollmentChart.visibility = View.VISIBLE

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = listOf(
                ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.app_blue),
                ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.product_orange)
            ).filterIndexed { index, color -> index < entries.size }
            valueTextSize = 12f
            setDrawValues(true)
            valueTextColor = Color.BLACK
        }
        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter())

        appProductEnrollmentChart.data = data
        appProductEnrollmentChart.description.isEnabled = false
        appProductEnrollmentChart.isDrawHoleEnabled = true
        appProductEnrollmentChart.setCenterText(getString(R.string.app_vs_product_enrollment))
        appProductEnrollmentChart.setCenterTextSize(14f)
        appProductEnrollmentChart.animateY(800)
        appProductEnrollmentChart.invalidate()
    }

    // NEW: Function to set up Submission Status Pie Chart
    private fun setupSubmissionStatusChart(counts: Map<String, Int>) {
        val entries = ArrayList<PieEntry>()
        val colors = mutableListOf<Int>()

        if ((counts["accepted"] ?: 0) > 0) {
            entries.add(PieEntry(counts["accepted"]!!.toFloat(), "Accepted"))
            colors.add(ContextCompat.getColor(this, R.color.success_green))
        }
        if ((counts["pending"] ?: 0) > 0) {
            entries.add(PieEntry(counts["pending"]!!.toFloat(), "Pending"))
            colors.add(ContextCompat.getColor(this, R.color.pending_yellow))
        }
        if ((counts["rejected"] ?: 0) > 0) {
            entries.add(PieEntry(counts["rejected"]!!.toFloat(), "Rejected"))
            colors.add(ContextCompat.getColor(this, R.color.rejected_red))
        }

        if (entries.isEmpty()) {
            submissionStatusChart.visibility = View.GONE
            return
        }
        submissionStatusChart.visibility = View.VISIBLE

        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            sliceSpace = 3f
            valueTextSize = 12f
            setDrawValues(true)
            valueTextColor = Color.BLACK
        }

        val data = PieData(dataSet)
        submissionStatusChart.data = data
        submissionStatusChart.description.isEnabled = false
        submissionStatusChart.isDrawHoleEnabled = true
        submissionStatusChart.setCenterText(getString(R.string.submissions_status))
        submissionStatusChart.setCenterTextSize(14f)
        submissionStatusChart.animateY(1000)
        submissionStatusChart.invalidate()
    }

    private fun setupPhasePerformanceChart(performance: List<PhasePerformanceData>) {
        val milestones = performance.map { it.milestoneIndex }.distinct().sorted()
        val labels = milestones.map { "M$it" }
        val entries = ArrayList<BarEntry>()

        milestones.forEachIndexed { index, milestoneIndex ->
            val milestoneData = performance.filter { it.milestoneIndex == milestoneIndex }
            val accepted = milestoneData.find { it.phase == "accepted" }?.count?.toFloat() ?: 0f
            val pending = milestoneData.find { it.phase == "pending" }?.count?.toFloat() ?: 0f
            val rejected = milestoneData.find { it.phase == "rejected" }?.count?.toFloat() ?: 0f
            entries.add(BarEntry(index.toFloat(), floatArrayOf(accepted, pending, rejected)))
        }

        if (entries.isEmpty() || labels.isEmpty()) {
            phasePerformanceChart.visibility = View.GONE
            return
        }
        phasePerformanceChart.visibility = View.VISIBLE

        val dataSet = BarDataSet(entries, "").apply {
            stackLabels = arrayOf(getString(R.string.accepted), getString(R.string.pending), getString(R.string.rejected))
            colors = listOf(
                ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.success_green),
                ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.pending_yellow),
                ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.rejected_red)
            )
            valueTextColor = Color.BLACK
            valueTextSize = 10f
            setDrawValues(true)
        }

        val dataSets = ArrayList<IBarDataSet>().apply { add(dataSet) }
        val data = BarData(dataSets)

        phasePerformanceChart.data = data
        phasePerformanceChart.description.isEnabled = false
        phasePerformanceChart.animateY(800)
        phasePerformanceChart.legend.isWordWrapEnabled = true
        phasePerformanceChart.setFitBars(true)

        val xAxis = phasePerformanceChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f

        phasePerformanceChart.axisLeft.axisMinimum = 0f
        phasePerformanceChart.axisRight.isEnabled = false
        phasePerformanceChart.invalidate()
    }

    private fun setupProgressTrendChart(trends: ProgressTrend) {
        val allDates = (trends.app.keys + trends.product.keys).toMutableSet().toList().sorted()
        val appEntries = ArrayList<Entry>()
        val productEntries = ArrayList<Entry>()

        allDates.forEachIndexed { index, date ->
            appEntries.add(Entry(index.toFloat(), trends.app[date]?.toFloat() ?: 0f))
            productEntries.add(Entry(index.toFloat(), trends.product[date]?.toFloat() ?: 0f))
        }

        if (allDates.isEmpty()) {
            progressTrendChart.visibility = View.GONE
            return
        }
        progressTrendChart.visibility = View.VISIBLE

        val appDataSet = LineDataSet(appEntries, getString(R.string.app_projects)).apply {
            color = ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.app_blue)
            lineWidth = 2f
            circleRadius = 4f
            circleColors = listOf(ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.app_blue))
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val productDataSet = LineDataSet(productEntries, getString(R.string.product_projects)).apply {
            color = ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.product_orange)
            lineWidth = 2f
            circleRadius = 4f
            circleColors = listOf(ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.product_orange))
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val dataSets = ArrayList<ILineDataSet>().apply {
            add(appDataSet)
            add(productDataSet)
        }

        val data = LineData(dataSets)
        progressTrendChart.data = data
        progressTrendChart.description.isEnabled = false
        progressTrendChart.animateX(800)
        progressTrendChart.legend.isWordWrapEnabled = true

        val xAxis = progressTrendChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(allDates)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.setLabelCount(allDates.size, false)
        xAxis.setDrawLabels(true)
        xAxis.isGranularityEnabled = true
        xAxis.setAvoidFirstLastClipping(true)

        val leftAxis = progressTrendChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        progressTrendChart.axisRight.isEnabled = false
        progressTrendChart.invalidate()
    }

    // NEW: Function to set up Milestone Completion Overview
    private fun setupMilestoneCompletionOverviewChart(data: List<MilestoneCompletionOverviewData>) {
        if (data.isEmpty()) {
            milestoneCompletionOverviewChart.visibility = View.GONE
            return
        }
        milestoneCompletionOverviewChart.visibility = View.VISIBLE

        val projectTitles = data.map { it.projectTitle }
        val entries = ArrayList<BarEntry>()

        data.forEachIndexed { projectIndex, projectData ->
            val milestoneStatuses = projectData.milestones
            val acceptedCount = milestoneStatuses.sumOf { it.accepted }.toFloat()
            val pendingCount = milestoneStatuses.sumOf { it.pending }.toFloat()
            val rejectedCount = milestoneStatuses.sumOf { it.rejected }.toFloat()
            val notSubmittedCount = milestoneStatuses.sumOf { it.notSubmitted }.toFloat()

            entries.add(BarEntry(projectIndex.toFloat(), floatArrayOf(acceptedCount, pendingCount, rejectedCount, notSubmittedCount)))
        }

        val dataSet = BarDataSet(entries, "Milestone Status").apply {
            setColors(listOf(
                ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.success_green),
                ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.pending_yellow),
                ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.rejected_red),
                ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.text_light)
            ))
            stackLabels = arrayOf("Accepted", "Pending", "Rejected", "Not Submitted")
            valueTextColor = Color.BLACK
            setDrawValues(true)
        }

        val barData = BarData(dataSet)
        milestoneCompletionOverviewChart.data = barData
        milestoneCompletionOverviewChart.description.isEnabled = false
        milestoneCompletionOverviewChart.animateY(1000)
        milestoneCompletionOverviewChart.legend.isWordWrapEnabled = true
        milestoneCompletionOverviewChart.setFitBars(true)

        val xAxis = milestoneCompletionOverviewChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(projectTitles)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        xAxis.setDrawLabels(true)

        val leftAxis = milestoneCompletionOverviewChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        leftAxis.setDrawGridLines(false)

        milestoneCompletionOverviewChart.axisRight.isEnabled = false
        milestoneCompletionOverviewChart.invalidate()
    }

    private fun setupProjectCapacityChart(capacityData: List<ProjectCapacityData>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        capacityData.forEachIndexed { index, data ->
            val percentage = if (data.capacity > 0) (data.enrolledCount.toFloat() / data.capacity.toFloat()) * 100 else 0f
            entries.add(BarEntry(index.toFloat(), percentage))
            labels.add(data.projectTitle)
        }

        if (entries.isEmpty() || labels.isEmpty()) {
            projectCapacityChart.visibility = View.GONE
            return
        }
        projectCapacityChart.visibility = View.VISIBLE

        val dataSet = BarDataSet(entries, getString(R.string.capacity_utilization_percent)).apply {
            color = ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.project_utilization_bar)
            valueTextSize = 12f
            setDrawValues(true)
            valueTextColor = Color.BLACK
            valueFormatter = object : ValueFormatter() {
                private val format = DecimalFormat("###.##")
                override fun getFormattedValue(value: Float): String {
                    return "${format.format(value)}%"
                }
            }
        }
        val data = BarData(dataSet)
        data.barWidth = 0.9f

        projectCapacityChart.data = data
        projectCapacityChart.description.isEnabled = false
        projectCapacityChart.animateY(800)
        projectCapacityChart.setFitBars(true)

        val xAxis = projectCapacityChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        xAxis.setDrawLabels(true)
        xAxis.setCenterAxisLabels(true)
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = labels.size.toFloat() - 0.5f

        val leftAxis = projectCapacityChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 100f
        leftAxis.granularity = 20f
        leftAxis.valueFormatter = object : ValueFormatter() {
            private val format = DecimalFormat("###")
            override fun getFormattedValue(value: Float): String {
                return "${format.format(value)}%"
            }
        }
        projectCapacityChart.axisRight.isEnabled = false
        projectCapacityChart.invalidate()
    }

    // Removed: private fun setupDeveloperPerformanceChart(...)

    private fun setupStudentPerformanceChart(studentPerformance: List<LaggingStudentData>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        studentPerformance.forEachIndexed { index, data ->
            entries.add(BarEntry(index.toFloat(), data.progressPercent))
            labels.add(data.studentName)
        }

        if (entries.isEmpty() || labels.isEmpty()) {
            studentPerformanceChart.visibility = View.GONE
            return
        }
        studentPerformanceChart.visibility = View.VISIBLE

        val dataSet = BarDataSet(entries, getString(R.string.student_progress_percent)).apply {
            color = ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.lagging_students_bar)
            valueTextSize = 12f
            setDrawValues(true)
            valueTextColor = Color.BLACK
            valueFormatter = object : ValueFormatter() {
                private val format = DecimalFormat("###.##")
                override fun getFormattedValue(value: Float): String {
                    return "${format.format(value)}%"
                }
            }
        }
        val data = BarData(dataSet)
        data.barWidth = 0.9f

        studentPerformanceChart.data = data
        studentPerformanceChart.description.isEnabled = false
        studentPerformanceChart.animateY(800)
        studentPerformanceChart.setFitBars(true)

        val xAxis = studentPerformanceChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        xAxis.setDrawLabels(true)
        xAxis.setCenterAxisLabels(true)
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = labels.size.toFloat() - 0.5f

        val leftAxis = studentPerformanceChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 100f
        leftAxis.granularity = 20f
        leftAxis.valueFormatter = object : ValueFormatter() {
            private val format = DecimalFormat("###")
            override fun getFormattedValue(value: Float): String {
                return "${format.format(value)}%"
            }
        }
        studentPerformanceChart.axisRight.isEnabled = false
        studentPerformanceChart.invalidate()
    }
    //endregion
}