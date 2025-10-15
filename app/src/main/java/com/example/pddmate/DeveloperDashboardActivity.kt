package com.example.pddmate

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import android.widget.Button
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.view.Gravity
import com.google.gson.annotations.SerializedName
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat
import java.util.Locale

class DeveloperDashboardActivity : AppCompatActivity() {

    private lateinit var developerNameTextView: TextView
    private lateinit var totalProjectsTextView: TextView
    private lateinit var totalPendingReviewsTextView: TextView
    private lateinit var pendingReviewsLayout: LinearLayout
    private lateinit var submissionsPieChart: PieChart
    private lateinit var enrollmentsBarChart: BarChart
    private lateinit var appProductProgressBarChart: BarChart
    private lateinit var studentProgressBarChart: BarChart
    private lateinit var milestoneCompletionStackedBarChart: BarChart
    // Removed: private lateinit var avgTimeLineChart: LineChart
    // Removed: private lateinit var rejectedPhasesBarChart: BarChart

    private lateinit var apiService: ApiService
    private var userId: String? = null

    private val appMilestoneTitles = listOf(
        "Idea Selection", "UI/UX Design", "Frontend Development",
        "Backend Integration", "Testing & Debugbing", "Deployment & Maintenance"
    )
    private val productMilestoneTitles = listOf(
        "Idea Selection", "Modelling", "Prototype",
        "Validation", "Integration", "Documentation"
    )
    private var projectTypes: Map<Int, String> = emptyMap()

    // Add this new data class
    data class MilestoneCompletionOverviewData(
        @SerializedName("project_title") val projectTitle: String,
        val milestones: List<MilestoneStatusData>
    )

    // Add this new data class for the milestone status
    data class MilestoneStatusData(
        val accepted: Int,
        val pending: Int,
        val rejected: Int,
        @SerializedName("not_submitted") val notSubmitted: Int
    )

    // Update the DeveloperDashboardResponse data class
    data class DeveloperDashboardResponse(
        val success: Boolean,
        val message: String,
        val projects: List<ProjectData>,
        @SerializedName("pending_reviews") val pendingReviews: List<PendingReviewData>,
        @SerializedName("submission_counts") val submissionCounts: Map<String, Int>,
        @SerializedName("enrollment_counts") val enrollmentCounts: Map<String, Int>,
        @SerializedName("student_progress_data") val studentProgressData: List<StudentProgressData>,
        @SerializedName("app_product_progress") val appProductProgress: List<AppProductProgressData>,
        @SerializedName("avg_time_per_milestone") val avgTimePerMilestone: List<Double>,
        @SerializedName("milestone_completion_overview") val milestoneCompletionOverview: List<MilestoneCompletionOverviewData>,
        @SerializedName("rejected_phases_trend") val rejectedPhasesTrend: List<RejectedPhasesData>
    )

    data class ProjectData(
        @SerializedName("project_id") val projectId: Int,
        val title: String,
        val type: String
    )

    data class PendingReviewData(
        @SerializedName("project_id") val projectId: Int,
        @SerializedName("user_id") val userId: String,
        @SerializedName("student_name") val studentName: String,
        @SerializedName("project_title") val projectTitle: String,
        @SerializedName("milestone_index") val milestoneIndex: Int
    )

    data class StudentProgressData(
        @SerializedName("student_name") val studentName: String,
        @SerializedName("completed_milestones") val completedMilestones: Int
    )

    data class AppProductProgressData(
        val type: String,
        @SerializedName("total_students") val totalStudents: Int,
        @SerializedName("accepted_milestones") val acceptedMilestones: Int
    )

    data class RejectedPhasesData(
        @SerializedName("milestone_index") val milestoneIndex: Int,
        @SerializedName("rejection_count") val rejectionCount: Int
    )

    interface ApiService {
        @FormUrlEncoded
        @POST("get_developer_dashboard_data.php")
        fun getDeveloperDashboardData(@Field("user_id") userId: String): Call<DeveloperDashboardResponse>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer_dashboard)

        developerNameTextView = findViewById(R.id.developer_name_text_view)
        totalProjectsTextView = findViewById(R.id.total_projects_text_view)
        totalPendingReviewsTextView = findViewById(R.id.total_pending_reviews_text_view)
        pendingReviewsLayout = findViewById(R.id.pending_reviews_layout)
        submissionsPieChart = findViewById(R.id.submissions_pie_chart)
        enrollmentsBarChart = findViewById(R.id.enrollments_bar_chart)
        studentProgressBarChart = findViewById(R.id.student_progress_bar_chart)
        appProductProgressBarChart = findViewById(R.id.app_product_progress_bar_chart)
        milestoneCompletionStackedBarChart = findViewById(R.id.milestone_completion_stacked_bar_chart)
        // Removed initializations for avgTimeLineChart and rejectedPhasesBarChart

        userId = intent.getStringExtra("USER_ID") ?: getSharedPreferences("login_session", Context.MODE_PRIVATE).getString("user_id", null)

        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.249.231.64/pdd_dashboard/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        apiService = retrofit.create(ApiService::class.java)

        if (!userId.isNullOrEmpty()) {
            val devName = getSharedPreferences("login_session", Context.MODE_PRIVATE).getString("name", "Developer")
            developerNameTextView.text = getString(R.string.welcome_developer, devName)
            fetchDashboardData(userId!!)
        } else {
            Toast.makeText(this, getString(R.string.user_id_not_found), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!userId.isNullOrEmpty()) {
            fetchDashboardData(userId!!)
        }
    }

    private fun fetchDashboardData(userId: String) {
        apiService.getDeveloperDashboardData(userId).enqueue(object : Callback<DeveloperDashboardResponse> {
            override fun onResponse(call: Call<DeveloperDashboardResponse>, response: Response<DeveloperDashboardResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()
                    if (data != null) {
                        totalProjectsTextView.text = data.projects.size.toString()
                        totalPendingReviewsTextView.text = data.pendingReviews.size.toString()

                        projectTypes = data.projects.associate { it.projectId to it.type }

                        updatePendingReviewsUI(data.pendingReviews)
                        setupSubmissionsPieChart(data.submissionCounts)
                        setupEnrollmentsBarChart(data.enrollmentCounts)
                        setupAppProductProgressBarChart(data.appProductProgress)
                        setupStudentProgressBarChart(data.studentProgressData)
                        setupMilestoneCompletionStackedBarChart(data.milestoneCompletionOverview)
                        // Removed calls to setupAvgTimeLineChart and setupRejectedPhasesBarChart
                    }
                } else {
                    Log.e("DevDashboard", "Failed to load: ${response.body()?.message ?: "Unknown API error"}")
                    Toast.makeText(this@DeveloperDashboardActivity, getString(R.string.failed_to_load_dashboard_data), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DeveloperDashboardResponse>, t: Throwable) {
                Log.e("DevDashboard", "Network error: ${t.message}", t)
                val displayMessage = when {
                    t.message?.contains("EHOSTUNREACH") == true -> "Network connection failed. Please check your Wi-Fi and try again."
                    else -> getString(R.string.network_error, t.message)
                }
                Toast.makeText(this@DeveloperDashboardActivity, displayMessage, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun updatePendingReviewsUI(reviews: List<PendingReviewData>) {
        pendingReviewsLayout.removeAllViews()
        if (reviews.isEmpty()) {
            val noReviewsView = TextView(this).apply {
                text = getString(R.string.no_pending_reviews_msg)
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 32)
                setTextColor(ContextCompat.getColor(this@DeveloperDashboardActivity, R.color.text_light))
            }
            pendingReviewsLayout.addView(noReviewsView)
        } else {
            reviews.forEach { review ->
                val cardView = LayoutInflater.from(this).inflate(R.layout.dashboard_review_card, pendingReviewsLayout, false) as CardView
                val studentNameText = cardView.findViewById<TextView>(R.id.student_name_text)
                val projectMilestoneText = cardView.findViewById<TextView>(R.id.project_milestone_text)
                val reviewButton = cardView.findViewById<Button>(R.id.review_button)

                studentNameText.text = review.studentName
                projectMilestoneText.text = getString(R.string.milestone_title_format, review.milestoneIndex, review.projectTitle)

                val targetActivity = if (review.milestoneIndex == 0) VerifyIdeaSelectionActivity::class.java else VerifyFileUploadsActivity::class.java

                val clickAction = {
                    val intent = Intent(this, targetActivity)
                    intent.putExtra("project_id", review.projectId)
                    intent.putExtra("student_user_id", review.userId)
                    intent.putExtra("STEP_INDEX", review.milestoneIndex)
                    intent.putExtra("student_name", review.studentName)
                    intent.putExtra("project_title", review.projectTitle)
                    startActivity(intent)
                }

                cardView.setOnClickListener { clickAction() }
                reviewButton.setOnClickListener { clickAction() }

                pendingReviewsLayout.addView(cardView)
            }
        }
    }

    private fun setupSubmissionsPieChart(counts: Map<String, Int>) {
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
            submissionsPieChart.visibility = View.GONE
            return
        }
        submissionsPieChart.visibility = View.VISIBLE
        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            sliceSpace = 3f
            valueTextSize = 12f
            setDrawValues(true)
            valueTextColor = Color.BLACK
        }

        val data = PieData(dataSet)
        submissionsPieChart.data = data
        submissionsPieChart.description.isEnabled = false
        submissionsPieChart.isDrawHoleEnabled = true
        submissionsPieChart.setCenterText(getString(R.string.submissions_status))
        submissionsPieChart.setCenterTextSize(14f)
        submissionsPieChart.animateY(1000)
        submissionsPieChart.invalidate()
    }

    private fun setupEnrollmentsBarChart(counts: Map<String, Int>) {
        if (counts.isEmpty()) {
            enrollmentsBarChart.visibility = View.GONE
            return
        }
        enrollmentsBarChart.visibility = View.VISIBLE
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val colors = ArrayList<Int>()

        val statuses = listOf("Pending", "Approved", "Rejected")
        statuses.forEachIndexed { index, status ->
            val count = counts[status] ?: 0
            entries.add(BarEntry(index.toFloat(), count.toFloat()))
            labels.add(status)
            when (status) {
                "Pending" -> colors.add(ContextCompat.getColor(this, R.color.pending_yellow))
                "Approved" -> colors.add(ContextCompat.getColor(this, R.color.success_green))
                "Rejected" -> colors.add(ContextCompat.getColor(this, R.color.rejected_red))
            }
        }

        val dataSet = BarDataSet(entries, "Enrollment Status").apply {
            setColors(colors)
            valueTextSize = 12f
            setDrawValues(true)
            valueTextColor = Color.BLACK
        }
        val data = BarData(dataSet)
        enrollmentsBarChart.data = data
        enrollmentsBarChart.description.isEnabled = false
        enrollmentsBarChart.animateY(1000)
        enrollmentsBarChart.setFitBars(true)

        val xAxis = enrollmentsBarChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.setCenterAxisLabels(true)
        // Correcting alignment by setting axis min/max
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = labels.size.toFloat() - 0.5f

        val leftAxis = enrollmentsBarChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        leftAxis.setDrawGridLines(false)

        enrollmentsBarChart.axisRight.isEnabled = false
        enrollmentsBarChart.invalidate()
    }

    private fun setupAppProductProgressBarChart(progressData: List<AppProductProgressData>) {
        if (progressData.isEmpty()) {
            appProductProgressBarChart.visibility = View.GONE
            return
        }
        appProductProgressBarChart.visibility = View.VISIBLE

        val appData = progressData.find { it.type == "App" }
        val productData = progressData.find { it.type == "Product" }

        val labels = ArrayList<String>()
        val entries = ArrayList<BarEntry>()

        if (appData != null && appData.totalStudents > 0) {
            val appAvgProgress = (appData.acceptedMilestones.toFloat() / (appData.totalStudents.toFloat() * appMilestoneTitles.size.toFloat())) * 100
            entries.add(BarEntry(0f, appAvgProgress))
            labels.add("App")
        }

        if (productData != null && productData.totalStudents > 0) {
            val productAvgProgress = (productData.acceptedMilestones.toFloat() / (productData.totalStudents.toFloat() * productMilestoneTitles.size.toFloat())) * 100
            entries.add(BarEntry(1f, productAvgProgress))
            labels.add("Product")
        }

        if (entries.isEmpty()) {
            appProductProgressBarChart.visibility = View.GONE
            return
        }

        val dataSet = BarDataSet(entries, "Average Progress %").apply {
            setColor(ContextCompat.getColor(this@DeveloperDashboardActivity, R.color.colorPrimary))
            valueTextSize = 12f
            setDrawValues(true)
            valueTextColor = Color.BLACK
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format(Locale.getDefault(), "%.1f%%", value)
                }
            }
        }

        val data = BarData(dataSet)
        appProductProgressBarChart.data = data
        appProductProgressBarChart.description.isEnabled = false
        appProductProgressBarChart.animateY(1000)
        appProductProgressBarChart.setFitBars(true)

        val xAxis = appProductProgressBarChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.setCenterAxisLabels(true)
        // Correcting alignment by setting axis min/max
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = labels.size.toFloat() - 0.5f

        val leftAxis = appProductProgressBarChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 100f
        leftAxis.granularity = 10f
        leftAxis.setDrawGridLines(false)
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}%"
            }
        }

        appProductProgressBarChart.axisRight.isEnabled = false
        appProductProgressBarChart.invalidate()
    }

    private fun setupStudentProgressBarChart(progressData: List<StudentProgressData>) {
        if (progressData.isEmpty()) {
            studentProgressBarChart.visibility = View.GONE
            return
        }
        studentProgressBarChart.visibility = View.VISIBLE
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        progressData.forEachIndexed { index, data ->
            entries.add(BarEntry(index.toFloat(), data.completedMilestones.toFloat()))
            labels.add(data.studentName)
        }

        val dataSet = BarDataSet(entries, "Completed Milestones").apply {
            setColor(ContextCompat.getColor(this@DeveloperDashboardActivity, R.color.colorPrimary))
            valueTextSize = 12f
            setDrawValues(true)
            valueTextColor = Color.BLACK
        }
        val data = BarData(dataSet)
        studentProgressBarChart.data = data
        studentProgressBarChart.description.isEnabled = false
        studentProgressBarChart.animateY(1000)
        studentProgressBarChart.setFitBars(true)

        val xAxis = studentProgressBarChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = 45f

        val leftAxis = studentProgressBarChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        leftAxis.setDrawGridLines(false)

        studentProgressBarChart.axisRight.isEnabled = false
        studentProgressBarChart.invalidate()
    }

    private fun setupMilestoneCompletionStackedBarChart(data: List<MilestoneCompletionOverviewData>) {
        if (data.isEmpty()) {
            milestoneCompletionStackedBarChart.visibility = View.GONE
            return
        }
        milestoneCompletionStackedBarChart.visibility = View.VISIBLE

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
                ContextCompat.getColor(this@DeveloperDashboardActivity, R.color.success_green),
                ContextCompat.getColor(this@DeveloperDashboardActivity, R.color.pending_yellow),
                ContextCompat.getColor(this@DeveloperDashboardActivity, R.color.rejected_red),
                ContextCompat.getColor(this@DeveloperDashboardActivity, R.color.text_light)
            ))
            stackLabels = arrayOf("Accepted", "Pending", "Rejected", "Not Submitted")
            valueTextColor = Color.BLACK
            setDrawValues(true)
        }

        val barData = BarData(dataSet)
        milestoneCompletionStackedBarChart.data = barData
        milestoneCompletionStackedBarChart.description.isEnabled = false
        milestoneCompletionStackedBarChart.animateY(1000)
        milestoneCompletionStackedBarChart.legend.isWordWrapEnabled = true
        milestoneCompletionStackedBarChart.setFitBars(true)

        val xAxis = milestoneCompletionStackedBarChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(projectTitles)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = 45f

        val leftAxis = milestoneCompletionStackedBarChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        leftAxis.setDrawGridLines(false)

        milestoneCompletionStackedBarChart.axisRight.isEnabled = false
        milestoneCompletionStackedBarChart.invalidate()
    }
}
