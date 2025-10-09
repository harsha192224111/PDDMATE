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
import kotlin.collections.ArrayList
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat

class DeveloperDashboardActivity : AppCompatActivity() {

    private lateinit var developerNameTextView: TextView
    private lateinit var totalProjectsTextView: TextView
    private lateinit var totalPendingReviewsTextView: TextView
    private lateinit var pendingReviewsLayout: LinearLayout
    private lateinit var submissionsPieChart: PieChart
    private lateinit var enrollmentsBarChart: BarChart
    private lateinit var appProductProgressBarChart: BarChart // Stacked bar chart for App vs Product
    private lateinit var studentProgressBarChart: BarChart
    private lateinit var avgTimeLineChart: LineChart

    private lateinit var apiService: ApiService
    private var userId: String? = null

    // Milestone titles are used to determine total possible milestones for the stacked bar chart
    private val appMilestoneTitles = listOf(
        "Idea Selection", "UI/UX Design", "Frontend Development",
        "Backend Integration", "Testing & Debugging", "Deployment & Maintenance"
    )
    private val productMilestoneTitles = listOf(
        "Idea Selection", "Modelling", "Prototype",
        "Validation", "Integration", "Documentation"
    )
    private var projectTypes: Map<Int, String> = emptyMap()

    // Data Models (using camelCase for Kotlin and @SerializedName for PHP snake_case)
    data class DeveloperDashboardResponse(
        val success: Boolean,
        val message: String,
        val projects: List<ProjectData>,
        @SerializedName("pending_reviews") val pendingReviews: List<PendingReviewData>,
        @SerializedName("submission_counts") val submissionCounts: Map<String, Int>,
        @SerializedName("enrollment_counts") val enrollmentCounts: Map<String, Int>,
        @SerializedName("student_progress_data") val studentProgressData: List<StudentProgressData>,
        @SerializedName("app_product_progress") val appProductProgress: List<AppProductProgressData>, // New data model
        @SerializedName("avg_time_per_milestone") val avgTimePerMilestone: List<Double>
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

    // Data model for the new stacked bar chart
    data class AppProductProgressData(
        val type: String,
        @SerializedName("total_students") val totalStudents: Int,
        @SerializedName("milestones_completed") val milestonesCompleted: Int
    )

    // Retrofit Interface
    interface ApiService {
        @FormUrlEncoded
        @POST("get_developer_dashboard_data.php")
        fun getDeveloperDashboardData(@Field("user_id") userId: String): Call<DeveloperDashboardResponse>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer_dashboard)

        // Initialize views
        developerNameTextView = findViewById(R.id.developer_name_text_view)
        totalProjectsTextView = findViewById(R.id.total_projects_text_view)
        totalPendingReviewsTextView = findViewById(R.id.total_pending_reviews_text_view)
        pendingReviewsLayout = findViewById(R.id.pending_reviews_layout)
        submissionsPieChart = findViewById(R.id.submissions_pie_chart)
        enrollmentsBarChart = findViewById(R.id.enrollments_bar_chart)
        studentProgressBarChart = findViewById(R.id.student_progress_bar_chart)
        appProductProgressBarChart = findViewById(R.id.app_product_progress_bar_chart) // Initialize new chart
        avgTimeLineChart = findViewById(R.id.avg_time_line_chart)

        userId = intent.getStringExtra("USER_ID") ?: getSharedPreferences("login_session", Context.MODE_PRIVATE).getString("user_id", null)

        // Set up Retrofit
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

                        // Populate the project types map
                        projectTypes = data.projects.associate { it.projectId to it.type }

                        updatePendingReviewsUI(data.pendingReviews)
                        setupSubmissionsPieChart(data.submissionCounts)
                        setupEnrollmentsBarChart(data.enrollmentCounts)
                        setupAppProductProgressBarChart(data.appProductProgress) // Call new function
                        setupStudentProgressBarChart(data.studentProgressData)
                        setupAvgTimeLineChart(data.avgTimePerMilestone)
                    }
                } else {
                    Log.e("DevDashboard", "Failed to load: ${response.body()?.message ?: "Unknown API error"}")
                    Toast.makeText(this@DeveloperDashboardActivity, getString(R.string.failed_to_load_dashboard_data), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DeveloperDashboardResponse>, t: Throwable) {
                Log.e("DevDashboard", "Network error: ${t.message}", t)
                // A better message for the user that doesn't expose raw error
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
            this.colors = colors
            sliceSpace = 3f
            valueTextSize = 12f
            setDrawValues(true) // Ensure values are drawn
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
            this.colors = colors
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

        val leftAxis = enrollmentsBarChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        leftAxis.setDrawGridLines(false)

        enrollmentsBarChart.axisRight.isEnabled = false
        enrollmentsBarChart.invalidate()
    }

    // Function to set up the new stacked bar chart
    private fun setupAppProductProgressBarChart(progressData: List<AppProductProgressData>) {
        if (progressData.isEmpty()) {
            appProductProgressBarChart.visibility = View.GONE
            return
        }
        appProductProgressBarChart.visibility = View.VISIBLE

        val appData = progressData.find { it.type == "App" }
        val productData = progressData.find { it.type == "Product" }

        // Data for App projects
        val appStudents = appData?.totalStudents ?: 0
        val appMilestonesCompleted = appData?.milestonesCompleted?.toFloat() ?: 0f
        // Total possible milestones = Total Students * Milestones per project (6)
        val appTotalPossibleMilestones = appStudents * appMilestoneTitles.size.toFloat()
        val appMilestonesRemaining = if (appTotalPossibleMilestones > appMilestonesCompleted) appTotalPossibleMilestones - appMilestonesCompleted else 0f

        // Data for Product projects
        val productStudents = productData?.totalStudents ?: 0
        val productMilestonesCompleted = productData?.milestonesCompleted?.toFloat() ?: 0f
        // Total possible milestones = Total Students * Milestones per project (6)
        val productTotalPossibleMilestones = productStudents * productMilestoneTitles.size.toFloat()
        val productMilestonesRemaining = if (productTotalPossibleMilestones > productMilestonesCompleted) productTotalPossibleMilestones - productMilestonesCompleted else 0f

        // Create entries for the stacked bars
        // The inner array represents the stack: [completed, remaining]
        val entries = ArrayList<BarEntry>().apply {
            add(BarEntry(0f, floatArrayOf(appMilestonesCompleted, appMilestonesRemaining)))
            add(BarEntry(1f, floatArrayOf(productMilestonesCompleted, productMilestonesRemaining)))
        }

        val labels = listOf("App Projects (Total Students: $appStudents)", "Product Projects (Total Students: $productStudents)")

        val dataSet = BarDataSet(entries, "").apply {
            colors = listOf(
                ContextCompat.getColor(this@DeveloperDashboardActivity, R.color.success_green),
                ContextCompat.getColor(this@DeveloperDashboardActivity, R.color.rejected_red)
            )
            stackLabels = arrayOf("Completed Milestones", "Remaining Milestones")
            valueTextColor = Color.BLACK
            valueTextSize = 10f
            setDrawValues(true)
        }

        val dataSets = ArrayList<IBarDataSet>()
        dataSets.add(dataSet)

        val data = BarData(dataSets)
        data.barWidth = 0.8f
        appProductProgressBarChart.data = data
        appProductProgressBarChart.description.isEnabled = false
        appProductProgressBarChart.animateY(1000)
        appProductProgressBarChart.legend.isWordWrapEnabled = true

        // Customize X-Axis
        val xAxis = appProductProgressBarChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.setCenterAxisLabels(true)
        xAxis.textSize = 10f // Smaller font for longer labels

        // Customize Y-Axis
        val leftAxis = appProductProgressBarChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        leftAxis.setDrawGridLines(false)
        leftAxis.setDrawZeroLine(true)

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
            color = ContextCompat.getColor(this@DeveloperDashboardActivity, R.color.colorPrimary)
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

    // Function to set up the average time line chart with corrected data calculation
    private fun setupAvgTimeLineChart(avgTimes: List<Double>) {
        if (avgTimes.isEmpty() || avgTimes.all { it == 0.0 }) {
            avgTimeLineChart.visibility = View.GONE
            return
        }
        avgTimeLineChart.visibility = View.VISIBLE
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        // avgTimes is a numeric array; index corresponds to milestone index
        avgTimes.forEachIndexed { idx, days ->
            entries.add(Entry(idx.toFloat(), days.toFloat()))
            // Neutral label: M0, M1, M2, ...
            labels.add("M${idx}")
        }

        val dataSet = LineDataSet(entries, "Average Days to Complete Milestone").apply {
            color = ContextCompat.getColor(this@DeveloperDashboardActivity, R.color.colorPrimaryDark)
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            lineWidth = 2f
            circleRadius = 5f
            circleColors = listOf(ContextCompat.getColor(this@DeveloperDashboardActivity, R.color.colorPrimary))
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(this@DeveloperDashboardActivity, R.drawable.chart_fill_gradient)
        }

        // Format values to one decimal place (e.g., 5.3 days)
        dataSet.valueFormatter = object : ValueFormatter() {
            private val format = DecimalFormat("###,##0.0")
            override fun getFormattedValue(value: Float): String {
                return format.format(value.toDouble())
            }
        }

        val data = LineData(dataSet)
        avgTimeLineChart.data = data
        avgTimeLineChart.description.isEnabled = false
        avgTimeLineChart.animateY(1000)

        val xAxis = avgTimeLineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = 0f // Keep labels flat for M0, M1, M2

        val leftAxis = avgTimeLineChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        leftAxis.setDrawGridLines(false)
        leftAxis.setDrawZeroLine(true)

        avgTimeLineChart.axisRight.isEnabled = false
        avgTimeLineChart.invalidate()
    }
}
