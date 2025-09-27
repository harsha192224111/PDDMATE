package com.example.pddmate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class FileUploadsActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var uploadSection: LinearLayout
    private lateinit var feedbackEditText: EditText
    private lateinit var previousUploadsLayout: LinearLayout
    private lateinit var submitButton: Button
    private lateinit var backArrow: ImageView

    private val PICK_FILE_REQUEST_CODE = 1001
    private val uploadedFiles = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_uploads)

        titleTextView = findViewById(R.id.title)
        subtitleTextView = findViewById(R.id.subtitle)
        uploadSection = findViewById(R.id.upload_section)
        previousUploadsLayout = findViewById(R.id.previous_uploads)
        submitButton = findViewById(R.id.button_submit)
        backArrow = findViewById(R.id.backArrow)

        backArrow.setOnClickListener {
            finish()
        }

        uploadSection.setOnClickListener {
            openFileChooser()
        }

        submitButton.setOnClickListener {
            if (uploadedFiles.isEmpty()) {
                Toast.makeText(this, "Please upload files before submitting", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "Files submitted for review", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "application/msword", "video/*"))
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { uri ->
                uploadedFiles.add(uri)
                addFileToPreviousUploads(uri)
            }
        }
    }

    private fun addFileToPreviousUploads(fileUri: Uri) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 12, 16, 12)
            setBackgroundResource(R.drawable.file_card_bg)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 100)
            layoutParams = params
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val icon = ImageView(this).apply {
            val ext = contentResolver.getType(fileUri)?.substringAfterLast("/")
            val iconRes = when (ext) {
                "pdf" -> R.drawable.ic_pdf
                "msword", "vnd.openxmlformats-officedocument.wordprocessingml.document" -> R.drawable.ic_doc
                "mp4", "x-msvideo", "x-matroska" -> R.drawable.ic_mp4
                else -> R.drawable.ic_file
            }
            setImageResource(iconRes)
            layoutParams = LinearLayout.LayoutParams(80, 80).apply { marginEnd = 32 }
            contentDescription = "File Icon"
        }

        val fileNameText = TextView(this).apply {
            text = fileUri.lastPathSegment ?: "Uploaded File"
            textSize = 16f
        }

        layout.addView(icon)
        layout.addView(fileNameText)
        previousUploadsLayout.addView(layout)
    }
}