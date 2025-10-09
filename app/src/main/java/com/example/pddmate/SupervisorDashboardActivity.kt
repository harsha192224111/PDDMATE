package com.example.pddmate

import android.content.Context
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
import retrofit2.http.POST // Correct import for @POST annotation
import java.text.DecimalFormat

class SupervisorDashboardActivity : AppCompatActivity() {

    //region Views
    private lateinit var totalStudentsTextView: TextView
    private lateinit var totalDevelopersTextView: TextView
    private lateinit var completionRateTextView: TextView
    private lateinit var pendingReviewsTextView: TextView
    private lateinit var laggingStudentsTextView: TextView
    private lateinit var enrollmentTrendsChart: BarChart
    private lateinit var appProductEnrollmentChart: PieChart
    private lateinit var milestonePerformanceChart: BarChart
    private lateinit var successRateChart: PieChart
    private lateinit var appProductCompletionChart: BarChart
    private lateinit var appProductApprovalChart: BarChart
    private lateinit var progressTrendChart: LineChart
    //endregion

    //region API & Data Models
    private lateinit var apiService: ApiService

    data class SupervisorDashboardResponse(
        val success: Boolean,
        @SerializedName("overall_metrics") val overallMetrics: OverallMetrics,
        @SerializedName("enrollment_trends") val enrollmentTrends: EnrollmentTrends,
        @SerializedName("app_product_enrollment") val appProductEnrollment: Map<String, Int>,
        @SerializedName("milestone_performance") val milestonePerformance: List<MilestonePerformanceData>,
        @SerializedName("success_rate_distribution") val successRateDistribution: Map<String, Int>,
        @SerializedName("avg_completion_time") val avgCompletionTime: Map<String, Double>,
        @SerializedName("app_product_completion_rate") val appProductCompletionRate: Map<String, Float>,
        @SerializedName("app_product_rates") val appProductRates: Map<String, AppProductRates>,
        @SerializedName("progress_trend") val progressTrend: ProgressTrend
    )

    data class OverallMetrics(
        @SerializedName("total_students") val totalStudents: Int,
        @SerializedName("total_developers") val totalDevelopers: Int,
        @SerializedName("completion_rate") val completionRate: Float,
        @SerializedName("pending_reviews") val pendingReviews: Int,
        @SerializedName("lagging_students") val laggingStudents: Int
    )

    data class EnrollmentTrends(
        @SerializedName("App") val app: Map<String, Int>,
        @SerializedName("Product") val product: Map<String, Int>
    )

    data class ProgressTrend(
        @SerializedName("App") val app: Map<String, Int>,
        @SerializedName("Product") val product: Map<String, Int>
    )

    data class MilestonePerformanceData(
        @SerializedName("milestone_index") val milestoneIndex: Int,
        @SerializedName("completion_count") val completionCount: Int
    )

    data class AppProductRates(
        val accepted: Int,
        val rejected: Int
    )

    interface ApiService {
        @POST("get_supervisor_dashboard_data.php")
        fun getSupervisorDashboardData(): Call<SupervisorDashboardResponse>
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supervisor_dashboard)

        // Initialize card includes once and reuse to avoid duplicate lookups & inference errors
        val cardTotalStudents = findViewById<View>(R.id.card_total_students)
        val cardTotalDevelopers = findViewById<View>(R.id.card_total_developers)
        val cardCompletionRate = findViewById<View>(R.id.card_completion_rate)
        val cardPendingReviews = findViewById<View>(R.id.card_pending_reviews)
        val cardLaggingStudents = findViewById<View>(R.id.card_lagging_students)

        // Use typed findViewById on the included card view to avoid inference problems
        totalStudentsTextView = cardTotalStudents.findViewById<TextView>(R.id.metric_value_text_view)
        totalDevelopersTextView = cardTotalDevelopers.findViewById<TextView>(R.id.metric_value_text_view)
        completionRateTextView = cardCompletionRate.findViewById<TextView>(R.id.metric_value_text_view)
        pendingReviewsTextView = cardPendingReviews.findViewById<TextView>(R.id.metric_value_text_view)
        laggingStudentsTextView = cardLaggingStudents.findViewById<TextView>(R.id.metric_value_text_view)

        // Set card titles using the correct view hierarchy
        cardTotalStudents.findViewById<TextView>(R.id.metric_title_text_view).text = getString(R.string.total_students)
        cardTotalDevelopers.findViewById<TextView>(R.id.metric_title_text_view).text = getString(R.string.total_developers)
        cardCompletionRate.findViewById<TextView>(R.id.metric_title_text_view).text = getString(R.string.completion_rate)
        cardPendingReviews.findViewById<TextView>(R.id.metric_title_text_view).text = getString(R.string.pending_reviews)
        cardLaggingStudents.findViewById<TextView>(R.id.metric_title_text_view).text = getString(R.string.lagging_students)

        enrollmentTrendsChart = findViewById(R.id.enrollment_trends_chart)
        appProductEnrollmentChart = findViewById(R.id.app_product_enrollment_chart)
        milestonePerformanceChart = findViewById(R.id.milestone_performance_chart)
        successRateChart = findViewById(R.id.success_rate_chart)
        appProductCompletionChart = findViewById(R.id.app_product_completion_chart)
        appProductApprovalChart = findViewById(R.id.app_product_approval_chart)
        progressTrendChart = findViewById(R.id.progress_trend_chart)

        // Set up Retrofit
        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.249.231.64/pdd_dashboard/")
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
                        setupEnrollmentTrendsChart(it.enrollmentTrends)
                        setupAppProductEnrollmentChart(it.appProductEnrollment)
                        setupMilestonePerformanceChart(it.milestonePerformance)
                        setupSuccessRateChart(it.successRateDistribution)
                        setupAppProductCompletionChart(it.appProductCompletionRate)
                        setupAppProductApprovalChart(it.appProductRates)
                        setupProgressTrendChart(it.progressTrend)
                    }
                } else {
                    Log.e("SupervisorDashboard", "Failed to load: ${response.body()?.let { it } ?: "Unknown API error"}")
                    Toast.makeText(this@SupervisorDashboardActivity, getString(R.string.failed_to_load_dashboard_data), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SupervisorDashboardResponse>, t: Throwable) {
                Log.e("SupervisorDashboard", "Network error: ${t.message}", t)
                val displayMessage = when {
                    t.message?.contains("EHOSTUNREACH") == true -> "Network connection failed. Please check your Wi-Fi and try again."
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
        // Show percentage with trailing % symbol
        completionRateTextView.text = "${metrics.completionRate}%"
        pendingReviewsTextView.text = metrics.pendingReviews.toString()
        laggingStudentsTextView.text = metrics.laggingStudents.toString()
    }

    private fun setupEnrollmentTrendsChart(trends: EnrollmentTrends) {
        val allDates = (trends.app.keys + trends.product.keys).toMutableSet().toList().sorted()
        val appEntries = ArrayList<BarEntry>()
        val productEntries = ArrayList<BarEntry>()

        allDates.forEachIndexed { index, date ->
            appEntries.add(BarEntry(index.toFloat(), trends.app[date]?.toFloat() ?: 0f))
            productEntries.add(BarEntry(index.toFloat(), trends.product[date]?.toFloat() ?: 0f))
        }

        val appDataSet = BarDataSet(appEntries, getString(R.string.app_projects)).apply {
            color = ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.app_blue)
        }
        val productDataSet = BarDataSet(productEntries, getString(R.string.product_projects)).apply {
            color = ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.product_orange)
        }

        val dataSets = ArrayList<IBarDataSet>().apply {
            add(appDataSet)
            add(productDataSet)
        }

        val data = BarData(dataSets)
        data.barWidth = 0.4f
        val groupSpace = 0.2f
        val barSpace = 0.05f

        enrollmentTrendsChart.data = data
        // Ensure x-axis range large enough for grouping
        enrollmentTrendsChart.xAxis.axisMinimum = 0f
        enrollmentTrendsChart.xAxis.axisMaximum = (allDates.size.toFloat())
        // Group bars if there are at least 1 group
        if (allDates.isNotEmpty()) {
            enrollmentTrendsChart.groupBars(0f, groupSpace, barSpace)
        }
        enrollmentTrendsChart.description.isEnabled = false
        enrollmentTrendsChart.animateY(800)
        enrollmentTrendsChart.legend.isWordWrapEnabled = true

        val xAxis = enrollmentTrendsChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(allDates)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.setCenterAxisLabels(true)

        val leftAxis = enrollmentTrendsChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        enrollmentTrendsChart.axisRight.isEnabled = false

        enrollmentTrendsChart.invalidate()
    }

    private fun setupAppProductEnrollmentChart(enrollment: Map<String, Int>) {
        val entries = ArrayList<PieEntry>()
        val colors = mutableListOf<Int>()
        enrollment.forEach { (type, count) ->
            entries.add(PieEntry(count.toFloat(), type))
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = listOf(
                ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.app_blue),
                ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.product_orange)
            )
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

    private fun setupMilestonePerformanceChart(performance: List<MilestonePerformanceData>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        performance.forEach { data ->
            entries.add(BarEntry(data.milestoneIndex.toFloat(), data.completionCount.toFloat()))
            labels.add("M${data.milestoneIndex}")
        }

        val dataSet = BarDataSet(entries, getString(R.string.milestone_completion_count)).apply {
            color = ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.success_green)
            valueTextSize = 12f
            setDrawValues(true)
            valueTextColor = Color.BLACK
        }
        val data = BarData(dataSet)
        milestonePerformanceChart.data = data
        milestonePerformanceChart.description.isEnabled = false
        milestonePerformanceChart.animateY(800)
        milestonePerformanceChart.setFitBars(true)

        val xAxis = milestonePerformanceChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f

        val leftAxis = milestonePerformanceChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        milestonePerformanceChart.axisRight.isEnabled = false

        milestonePerformanceChart.invalidate()
    }

    private fun setupSuccessRateChart(distribution: Map<String, Int>) {
        val entries = ArrayList<PieEntry>()
        val colors = mutableListOf<Int>()

        distribution.forEach { (phase, count) ->
            entries.add(PieEntry(count.toFloat(), phase.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }))
            when (phase) {
                "accepted" -> colors.add(ContextCompat.getColor(this, R.color.success_green))
                "pending" -> colors.add(ContextCompat.getColor(this, R.color.pending_yellow))
                "rejected" -> colors.add(ContextCompat.getColor(this, R.color.rejected_red))
                else -> colors.add(ContextCompat.getColor(this, R.color.text_dark))
            }
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 3f
            valueTextSize = 12f
            setDrawValues(true)
            valueTextColor = Color.BLACK
        }

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter())

        successRateChart.data = data
        successRateChart.description.isEnabled = false
        successRateChart.isDrawHoleEnabled = true
        successRateChart.setCenterText(getString(R.string.submission_status_title))
        successRateChart.setCenterTextSize(14f)
        successRateChart.animateY(800)
        successRateChart.invalidate()
    }

    private fun setupAppProductCompletionChart(completionRates: Map<String, Float>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val colors = ArrayList<Int>()

        val appRate = completionRates["App"] ?: 0f
        val productRate = completionRates["Product"] ?: 0f

        if (appRate > 0 || productRate > 0) {
            entries.add(BarEntry(0f, appRate))
            labels.add("App")
            colors.add(ContextCompat.getColor(this, R.color.app_blue))

            entries.add(BarEntry(1f, productRate))
            labels.add("Product")
            colors.add(ContextCompat.getColor(this, R.color.product_orange))
        } else {
            // Handle case where no data is available, for example, by showing a message
            appProductCompletionChart.setNoDataText("No completion data available.")
            appProductCompletionChart.invalidate()
            return
        }

        val dataSet = BarDataSet(entries, getString(R.string.completion_rate_percent)).apply {
            this.colors = colors
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
        appProductCompletionChart.data = data
        appProductCompletionChart.description.isEnabled = false
        appProductCompletionChart.animateY(800)
        appProductCompletionChart.setFitBars(true)

        val xAxis = appProductCompletionChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = labels.size.toFloat() - 0.5f

        val leftAxis = appProductCompletionChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 100f
        leftAxis.granularity = 20f
        leftAxis.setDrawGridLines(false)
        leftAxis.valueFormatter = object : ValueFormatter() {
            private val format = DecimalFormat("###")
            override fun getFormattedValue(value: Float): String {
                return "${format.format(value)}%"
            }
        }

        appProductCompletionChart.axisRight.isEnabled = false
        appProductCompletionChart.invalidate()
    }

    private fun setupAppProductApprovalChart(rates: Map<String, AppProductRates>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        val appAccepted = rates["App"]?.accepted?.toFloat() ?: 0f
        val appRejected = rates["App"]?.rejected?.toFloat() ?: 0f
        val productAccepted = rates["Product"]?.accepted?.toFloat() ?: 0f
        val productRejected = rates["Product"]?.rejected?.toFloat() ?: 0f

        entries.add(BarEntry(0f, floatArrayOf(appAccepted, appRejected)))
        entries.add(BarEntry(1f, floatArrayOf(productAccepted, productRejected)))

        val dataSet = BarDataSet(entries, "").apply {
            colors = listOf(
                ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.success_green),
                ContextCompat.getColor(this@SupervisorDashboardActivity, R.color.rejected_red)
            )
            stackLabels = arrayOf(getString(R.string.accepted), getString(R.string.rejected))
            valueTextColor = Color.BLACK
            valueTextSize = 10f
            setDrawValues(true)
        }

        val dataSets = ArrayList<IBarDataSet>()
        dataSets.add(dataSet)

        val data = BarData(dataSets)
        data.barWidth = 0.8f
        appProductApprovalChart.data = data
        appProductApprovalChart.description.isEnabled = false
        appProductApprovalChart.animateY(800)
        appProductApprovalChart.legend.isWordWrapEnabled = true

        val xAxis = appProductApprovalChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(listOf("App", "Product"))
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.setCenterAxisLabels(true)

        val leftAxis = appProductApprovalChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        leftAxis.setDrawGridLines(false)
        leftAxis.setDrawZeroLine(true)

        appProductApprovalChart.axisRight.isEnabled = false
        appProductApprovalChart.invalidate()
    }

    private fun setupProgressTrendChart(trends: ProgressTrend) {
        val allDates = (trends.app.keys + trends.product.keys).toMutableSet().toList().sorted()
        val appEntries = ArrayList<Entry>()
        val productEntries = ArrayList<Entry>()

        allDates.forEachIndexed { index, date ->
            appEntries.add(Entry(index.toFloat(), trends.app[date]?.toFloat() ?: 0f))
            productEntries.add(Entry(index.toFloat(), trends.product[date]?.toFloat() ?: 0f))
        }

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
    //endregion
}