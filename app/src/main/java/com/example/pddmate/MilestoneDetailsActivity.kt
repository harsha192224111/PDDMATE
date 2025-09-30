package com.example.pddmate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File
import java.io.FileOutputStream
import java.util.*

class MilestoneDetailActivity : AppCompatActivity() {
    private lateinit var titleTextView: TextView
    private lateinit var submitButton: Button
    private lateinit var previousUploadsLayout: LinearLayout
    private lateinit var uploadSection: LinearLayout
    private lateinit var descriptionTextView: TextView
    private lateinit var backArrow: ImageView
    private lateinit var deadlineTextView: TextView
    private lateinit var clearButton: TextView

    private val selectedFiles = mutableListOf<Uri>()
    private var projectId: Int = -1
    private var stepIndex: Int = -1
    private var userId: String? = null
    private lateinit var apiService: ApiService
    private val uploadedFileNames = mutableSetOf<String>()
    private var clearedSelection = false

    interface ApiService {
        @Multipart
        @POST("upload_files.php")
        fun uploadFiles(
            @Part files: List<MultipartBody.Part>,
            @Part("project_id") projectId: RequestBody,
            @Part("milestone_index") milestoneIndex: RequestBody,
            @Part("user_id") userId: RequestBody,
            @Part("replace_existing") replaceExisting: RequestBody?
        ): Call<ApiResponse>

        @FormUrlEncoded
        @POST("get_files.php")
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

    data class ApiResponse(
        val success: Boolean,
        val message: String,
        val responses: List<FileUploadResponse>? = null
    )

    data class FileUploadResponse(
        val file: String,
        val success: Boolean,
        val message: String
    )

    data class FilesResponse(
        val success: Boolean,
        val message: String,
        val files: List<Map<String, String>> = emptyList()
    )

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val clipData = result.data?.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    val fileName = getFileNameFromUri(uri)
                    if (fileName != null && !selectedFiles.any { getFileNameFromUri(it) == fileName }) {
                        selectedFiles.add(uri)
                    }
                }
            } else {
                result.data?.data?.let { uri ->
                    val fileName = getFileNameFromUri(uri)
                    if (fileName != null && !selectedFiles.any { getFileNameFromUri(it) == fileName }) {
                        selectedFiles.add(uri)
                    }
                }
            }
            updatePreviousUploadsUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_uploads)
        val milestoneType = intent.getStringExtra("MILESTONE_TYPE")
        stepIndex = intent.getIntExtra("STEP_INDEX", -1)
        projectId = intent.getIntExtra("PROJECT_ID", -1)
        userId = intent.getStringExtra("USER_ID")

        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.31.109/pdd_dashboard/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        apiService = retrofit.create(ApiService::class.java)

        backArrow = findViewById(R.id.backArrow)
        titleTextView = findViewById(R.id.title)
        submitButton = findViewById(R.id.button_submit)
        previousUploadsLayout = findViewById(R.id.previous_uploads)
        uploadSection = findViewById(R.id.upload_section)
        descriptionTextView = findViewById(R.id.subtitle)
        deadlineTextView = findViewById(R.id.deadline)
        clearButton = findViewById(R.id.clear_button)

        backArrow.setOnClickListener { finish() }
        setTitleAndDescriptionByType(milestoneType, stepIndex)
        uploadSection.setOnClickListener { openFileChooser() }

        submitButton.setOnClickListener {
            if (selectedFiles.isNotEmpty() || clearedSelection) {
                if (!userId.isNullOrEmpty()) {
                    uploadFiles(projectId, stepIndex, userId!!, selectedFiles)
                } else {
                    Toast.makeText(this, "User ID not found.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.select_files_to_upload), Toast.LENGTH_SHORT).show()
            }
        }

        clearButton.setOnClickListener {
            clearedSelection = true
            selectedFiles.clear()
            previousUploadsLayout.removeAllViews()
            updatePreviousUploadsUI()
            Toast.makeText(this, getString(R.string.cleared_files_from_selection), Toast.LENGTH_SHORT).show()
        }

        if (projectId != -1 && stepIndex != -1 && !userId.isNullOrEmpty()) {
            fetchUploadedFiles(projectId, stepIndex, userId!!)
        }
    }

    private fun updatePreviousUploadsUI() {
        previousUploadsLayout.removeAllViews()
        uploadedFileNames.forEach { addFileToPreviousUploads(it, isNewUpload = false) }
        selectedFiles.forEach { uri ->
            val fileName = getFileNameFromUri(uri)
            if (fileName != null && !uploadedFileNames.contains(fileName)) {
                addFileToPreviousUploads(fileName, isNewUpload = true)
            }
        }
    }

    private fun setTitleAndDescriptionByType(type: String?, index: Int) {
        when (type) {
            "APP" -> when (index) {
                1 -> setTitleAndDescription(getString(R.string.uiux_title), getString(R.string.uiux_desc))
                2 -> setTitleAndDescription(getString(R.string.frontend_title), getString(R.string.frontend_desc))
                3 -> setTitleAndDescription(getString(R.string.backend_title), getString(R.string.backend_desc))
                4 -> setTitleAndDescription(getString(R.string.testing_title), getString(R.string.testing_desc))
                5 -> setTitleAndDescription(getString(R.string.documentation_title), getString(R.string.documentation_desc))
            }
            "PRODUCT" -> when (index) {
                1 -> setTitleAndDescription(getString(R.string.modelling_title), getString(R.string.modelling_desc))
                2 -> setTitleAndDescription(getString(R.string.prototype_title), getString(R.string.prototype_desc))
                3 -> setTitleAndDescription(getString(R.string.validation_title), getString(R.string.validation_desc))
                4 -> setTitleAndDescription(getString(R.string.integration_title), getString(R.string.integration_desc))
                5 -> setTitleAndDescription(getString(R.string.documentation_title), getString(R.string.documentation_desc))
            }
        }
    }

    private fun setTitleAndDescription(title: String, description: String) {
        titleTextView.text = title
        descriptionTextView.text = description
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "application/msword", "video/*"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    private fun addFileToPreviousUploads(fileName: String, isNewUpload: Boolean) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 12, 16, 12)
            setBackgroundResource(if (isNewUpload) R.drawable.file_card_bg_selected else R.drawable.file_card_bg)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 16
            }
            layoutParams = params
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(this).apply {
            val fileExtension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            setImageResource(
                when {
                    fileExtension == "pdf" -> R.drawable.ic_pdf
                    fileExtension.startsWith("doc") -> R.drawable.ic_doc
                    fileExtension in listOf("mp4", "mov", "avi", "mkv") -> R.drawable.ic_mp4
                    else -> R.drawable.ic_file
                }
            )
            layoutParams = LinearLayout.LayoutParams(80, 80).apply { marginEnd = 32 }
            contentDescription = getString(R.string.file_icon)
        }
        val fileNameText = TextView(this).apply {
            text = fileName
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
        }
        layout.addView(icon)
        layout.addView(fileNameText)
        previousUploadsLayout.addView(layout)
    }

    private fun fetchUploadedFiles(projectId: Int, stepIndex: Int, userId: String) {
        apiService.getFiles(projectId, stepIndex, userId).enqueue(object : Callback<FilesResponse> {
            override fun onResponse(call: Call<FilesResponse>, response: Response<FilesResponse>) {
                previousUploadsLayout.removeAllViews()
                selectedFiles.clear()
                uploadedFileNames.clear()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        body.files.forEach {
                            val name = it["file_name"]
                            if (name != null) uploadedFileNames.add(name)
                        }
                    } else {
                        val errorBodyString = response.errorBody()?.string()
                        Log.e("MilestoneDetailActivity", "Invalid response: $errorBodyString")
                    }
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    Log.e("MilestoneDetailActivity", "Server error: $errorBodyString")
                }
                updatePreviousUploadsUI()
            }
            override fun onFailure(call: Call<FilesResponse>, t: Throwable) {
                previousUploadsLayout.removeAllViews()
                Log.e("MilestoneDetailActivity", "Network error during fetch", t)
                Toast.makeText(this@MilestoneDetailActivity, getString(R.string.network_error, t.message), Toast.LENGTH_LONG).show()
                updatePreviousUploadsUI()
            }
        })
    }

    private fun uploadFiles(projectId: Int, stepIndex: Int, userId: String, fileUris: List<Uri>) {
        submitButton.isEnabled = false
        submitButton.text = getString(R.string.uploading)

        val filesToUpload = if (clearedSelection) selectedFiles else fileUris.filter { uri ->
            val fileName = getFileNameFromUri(uri)
            fileName != null && !uploadedFileNames.contains(fileName)
        }

        val fileParts = mutableListOf<MultipartBody.Part>()
        for (uri in filesToUpload) {
            val fileName = getFileNameFromUri(uri)
            if (fileName == null) {
                continue
            }
            val file = getFileFromUri(uri, fileName)
            if (file == null) {
                Toast.makeText(this, getString(R.string.failed_read_file), Toast.LENGTH_SHORT).show()
                continue
            }
            val mediaType = contentResolver.getType(uri)?.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
            val requestFile = file.asRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("files[]", fileName, requestFile)
            fileParts.add(part)
        }

        if (fileParts.isEmpty() && !clearedSelection) {
            Toast.makeText(this, getString(R.string.no_new_files_to_upload), Toast.LENGTH_SHORT).show()
            submitButton.isEnabled = true
            submitButton.text = getString(R.string.submit_for_review)
            return
        }

        val projectIdPart = projectId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val stepIndexPart = stepIndex.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val userIdPart = userId.toRequestBody("text/plain".toMediaTypeOrNull())

        val replacePart = if (clearedSelection) {
            "1".toRequestBody("text/plain".toMediaTypeOrNull())
        } else {
            "0".toRequestBody("text/plain".toMediaTypeOrNull())
        }

        val call = apiService.uploadFiles(fileParts, projectIdPart, stepIndexPart, userIdPart, replacePart)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                submitButton.isEnabled = true
                submitButton.text = getString(R.string.submit_for_review)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        setMilestonePhase(projectId, stepIndex, userId, "pending")
                    } else {
                        val message = body?.message ?: response.errorBody()?.string() ?: getString(R.string.unknown_error)
                        Toast.makeText(this@MilestoneDetailActivity, getString(R.string.upload_failed, message), Toast.LENGTH_LONG).show()
                        Log.e("MilestoneDetailActivity", "Upload Failed: $message")
                    }
                } else {
                    val message = response.errorBody()?.string() ?: getString(R.string.unknown_error)
                    Toast.makeText(this@MilestoneDetailActivity, getString(R.string.upload_failed, message), Toast.LENGTH_LONG).show()
                    Log.e("MilestoneDetailActivity", "Upload Failed: $message")
                }
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                submitButton.isEnabled = true
                submitButton.text = getString(R.string.submit_for_review)
                Log.e("MilestoneDetailActivity", "Network error: ${t.message}", t)
                Toast.makeText(this@MilestoneDetailActivity, getString(R.string.network_error, t.message), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setMilestonePhase(projectId: Int, stepIndex: Int, userId: String, phase: String) {
        apiService.setMilestonePhase(projectId, userId, stepIndex, phase).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@MilestoneDetailActivity, getString(R.string.files_submitted_successfully), Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Log.e("MilestoneDetailActivity", "Failed to update milestone phase.")
                    Toast.makeText(this@MilestoneDetailActivity, "Files submitted, but failed to update status.", Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("MilestoneDetailActivity", "Network error updating milestone phase: ${t.message}")
                Toast.makeText(this@MilestoneDetailActivity, "Files submitted, but failed to update status.", Toast.LENGTH_LONG).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        })
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.substringAfterLast('/')
        }
        return result
    }

    private fun getFileFromUri(uri: Uri, fileName: String): File? {
        val tempFile = File(cacheDir, fileName)
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return tempFile
        } catch (e: Exception) {
            Log.e("MilestoneDetailActivity", "Error creating temporary file from URI: ${e.message}", e)
            return null
        }
    }
}