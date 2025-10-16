package com.simats.pddmate

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.io.File
import java.util.Locale

class VerifyFileUploadsActivity : AppCompatActivity() {

    private lateinit var backArrow: ImageView
    private lateinit var milestoneTitleTextView: TextView
    private lateinit var studentNameTextView: TextView
    private lateinit var studentIdTextView: TextView
    private lateinit var projectTitleValue: TextView
    private lateinit var fileListLayout: LinearLayout
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button

    private var projectId: Int = -1
    private var stepIndex: Int = -1
    private var studentUserId: String? = null
    private var studentName: String? = null
    private var milestoneTitle: String? = null
    private var projectTitle: String? = null

    private lateinit var apiService: ApiService

    data class FileItem(val fileName: String, val filePath: String)
    data class FilesResponse(
        val success: Boolean,
        val message: String,
        val files: List<Map<String, String>> = emptyList(),
        val project_title: String? = null
    )
    data class ApiResponse(val success: Boolean, val message: String)

    interface ApiService {
        @FormUrlEncoded
        @POST("verify_files.php")
        fun getFiles(
            @Field("project_id") projectId: Int,
            @Field("milestone_index") milestoneIndex: Int,
            @Field("user_id") userId: String
        ): Call<FilesResponse>

        @FormUrlEncoded
        @POST("set_milestone_phase.php")
        fun setMilestonePhase(
            @Field("project_id") projectId: Int,
            @Field("user_id") userId: String,
            @Field("milestone_index") milestoneIndex: Int,
            @Field("phase") phase: String
        ): Call<ApiResponse>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_file_uploads)

        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://14.139.187.229:8081/pddmate/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        apiService = retrofit.create(ApiService::class.java)

        projectId = intent.getIntExtra("project_id", -1)
        stepIndex = intent.getIntExtra("STEP_INDEX", -1)
        studentUserId = intent.getStringExtra("student_user_id")
        studentName = intent.getStringExtra("student_name")
        milestoneTitle = intent.getStringExtra("MILESTONE_TITLE")
        projectTitle = intent.getStringExtra("project_title")

        backArrow = findViewById(R.id.backArrow)
        milestoneTitleTextView = findViewById(R.id.title)
        studentNameTextView = findViewById(R.id.studentNameTextView)
        studentIdTextView = findViewById(R.id.studentIdTextView)
        projectTitleValue = findViewById(R.id.projectTitleValue)
        fileListLayout = findViewById(R.id.fileListLayout)
        acceptButton = findViewById(R.id.acceptButton)
        rejectButton = findViewById(R.id.rejectButton)

        backArrow.setOnClickListener { finish() }

        milestoneTitleTextView.text = milestoneTitle
        studentNameTextView.text = studentName
        studentIdTextView.text = studentUserId
        projectTitleValue.text = "Project: $projectTitle"

        fetchUploadedFiles()

        acceptButton.setOnClickListener {
            updateMilestoneStatus("accepted")
        }

        rejectButton.setOnClickListener {
            updateMilestoneStatus("rejected")
        }
    }

    private fun fetchUploadedFiles() {
        if (projectId != -1 && stepIndex != -1 && !studentUserId.isNullOrEmpty()) {
            apiService.getFiles(projectId, stepIndex, studentUserId!!).enqueue(object : Callback<FilesResponse> {
                override fun onResponse(call: Call<FilesResponse>, response: Response<FilesResponse>) {
                    fileListLayout.removeAllViews()
                    if (response.isSuccessful && response.body()?.success == true) {
                        val files = response.body()?.files ?: emptyList()
                        if (files.isNotEmpty()) {
                            files.forEach { fileMap ->
                                val fileName = fileMap["file_name"]
                                val filePath = fileMap["file_path"]
                                if (fileName != null && filePath != null) {
                                    addFileView(FileItem(fileName, filePath))
                                }
                            }
                        } else {
                            addNoFilesView()
                        }
                    } else {
                        addNoFilesView()
                        Toast.makeText(this@VerifyFileUploadsActivity, "Failed to fetch files.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<FilesResponse>, t: Throwable) {
                    addNoFilesView()
                    Log.e("VerifyFileUploads", "Error fetching files: ${t.message}")
                    Toast.makeText(this@VerifyFileUploadsActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun addFileView(fileItem: FileItem) {
        val fileView = LayoutInflater.from(this).inflate(R.layout.file_item_card, fileListLayout, false)
        val fileNameTextView = fileView.findViewById<TextView>(R.id.fileName)
        val fileIcon = fileView.findViewById<ImageView>(R.id.fileIcon)
        val downloadIcon = fileView.findViewById<ImageView>(R.id.downloadFile)

        fileNameTextView.text = fileItem.fileName
        setFileIcon(fileIcon, fileItem.fileName)

        downloadIcon.setOnClickListener {
            downloadFile(fileItem)
        }

        fileListLayout.addView(fileView)
    }

    private fun addNoFilesView() {
        val noFilesTextView = TextView(this).apply {
            text = "No files uploaded for this milestone."
            gravity = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 32, 0, 32)
        }
        fileListLayout.addView(noFilesTextView)
    }

    private fun setFileIcon(imageView: ImageView, fileName: String) {
        val fileExtension = File(fileName).extension.lowercase(Locale.getDefault())
        val iconRes = when (fileExtension) {
            "pdf" -> R.drawable.ic_pdf
            "doc", "docx" -> R.drawable.ic_doc
            "mp4", "avi", "mkv" -> R.drawable.ic_mp4
            else -> R.drawable.ic_file
        }
        imageView.setImageResource(iconRes)
    }

    private fun downloadFile(fileItem: FileItem) {
        val baseUrl = "http://14.139.187.229:8081/pddmate/"
        val fullUrl = "$baseUrl${fileItem.filePath}"

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(fullUrl)
        val request = DownloadManager.Request(uri)
            .setTitle("Downloading ${fileItem.fileName}")
            .setDescription("Downloading file from PDD Dashboard")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileItem.fileName)

        try {
            downloadManager.enqueue(request)
            Toast.makeText(this, "Downloading started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("VerifyFileUploads", "Download failed: ${e.message}")
            Toast.makeText(this, "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateMilestoneStatus(phase: String) {
        if (projectId == -1 || studentUserId.isNullOrEmpty() || stepIndex == -1) {
            Toast.makeText(this, "Cannot update status. Missing data.", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.setMilestonePhase(projectId, studentUserId!!, stepIndex, phase).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@VerifyFileUploadsActivity, "Milestone updated to $phase!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@VerifyFileUploadsActivity, "Failed to update status. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@VerifyFileUploadsActivity, "Network error updating status.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}