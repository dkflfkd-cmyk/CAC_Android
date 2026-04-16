package com.example.cac

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class QuestionSetupActivity : AppCompatActivity() {

    private var selectedJob: String? = null
    private var selectedType: String? = null
    private var selectedCount: Int? = null
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null

    private lateinit var txtJob: TextView
    private lateinit var txtType: TextView
    private lateinit var txtCount: TextView

    private lateinit var fileRow: View
    private lateinit var txtFileName: TextView
    private lateinit var btnRemoveFile: ImageButton
    private lateinit var btnPickFile: MaterialButton

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult

            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            selectedFileUri = uri
            selectedFileName = queryDisplayName(uri) ?: "이력서 파일"
            txtFileName.text = selectedFileName

            fileRow.visibility = View.VISIBLE
            btnPickFile.visibility = View.GONE
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question_setup)

        findView()

        fileRow.visibility = View.GONE
        btnPickFile.visibility = View.VISIBLE

        setupTopBottomButtons()
        setupPickers()
        setupFileButton()
        setupRemoveFileButton()
        setupStartButton()
        setupTitle()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun findView() {
        txtJob = findViewById(R.id.txtJobHint)
        txtType = findViewById(R.id.txtTypeHint)
        txtCount = findViewById(R.id.txtCountHint)

        btnPickFile = findViewById(R.id.btnPickFile)

        fileRow = findViewById(R.id.fileRow)
        txtFileName = findViewById(R.id.txtFileName)
        btnRemoveFile = findViewById(R.id.btnRemoveFile)
    }

    private fun setupTopBottomButtons() {
        findViewById<TextView>(R.id.btnNoticev2).setOnClickListener {
            startActivity(Intent(this, MYActivity::class.java))
        }

        findViewById<TextView>(R.id.btnInterview).setOnClickListener {
            startActivity(Intent(this, InterviewActivity::class.java))
        }

        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }


        findViewById<TextView>(R.id.btnNoticev).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
    }

    private fun setupPickers() {
        val boxJob = findViewById<View>(R.id.boxJob)
        val boxType = findViewById<View>(R.id.boxType)
        val boxCount = findViewById<View>(R.id.boxCount)

        boxJob.setOnClickListener {
            val items = arrayOf("백엔드", "프론트엔드", "모바일", "데이터/AI", "UI/UX")
            AlertDialog.Builder(this)
                .setTitle("직무 선택")
                .setItems(items) { _, which ->
                    selectedJob = items[which]
                    txtJob.text = selectedJob
                    txtJob.setTextColor(Color.parseColor("#111111"))
                }
                .show()
        }

        boxType.setOnClickListener {
            val items = arrayOf("기술", "인성", "프로젝트", "CS")
            AlertDialog.Builder(this)
                .setTitle("질문 유형")
                .setItems(items) { _, which ->
                    selectedType = items[which]
                    txtType.text = selectedType
                    txtType.setTextColor(Color.parseColor("#111111"))
                }
                .show()
        }

        boxCount.setOnClickListener {
            val items = arrayOf("3", "5", "10")
            AlertDialog.Builder(this)
                .setTitle("질문 개수")
                .setItems(items) { _, which ->
                    selectedCount = items[which].toInt()
                    txtCount.text = "${selectedCount}개"
                    txtCount.setTextColor(Color.parseColor("#111111"))
                }
                .show()
        }
    }

    private fun setupFileButton() {
        btnPickFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("application/pdf"))
        }
    }

    private fun setupRemoveFileButton() {
        btnRemoveFile.setOnClickListener {
            selectedFileUri = null
            selectedFileName = null
            txtFileName.text = ""
            fileRow.visibility = View.GONE
            btnPickFile.visibility = View.VISIBLE
        }
    }

    private fun setupStartButton() {
        val btnStartCircle = findViewById<View>(R.id.btnStartCircle)

        btnStartCircle.setOnClickListener {
            if (selectedJob == null || selectedType == null || selectedCount == null) {
                Toast.makeText(this, "직무/유형/개수를 모두 선택해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedFileUri == null) {
                Toast.makeText(this, "이력서 파일을 선택해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, QuestionListActivity::class.java)
            intent.putExtra("job", selectedJob)
            intent.putExtra("type", selectedType)
            intent.putExtra("count", selectedCount)
            intent.putExtra("resumeName", selectedFileName)
            startActivity(intent)
        }
    }

    private fun setupTitle() {
        val title = findViewById<TextView>(R.id.txtTitle)

        val text = "Career AI Coach"
        val spannable = SpannableString(text)

        val blue = Color.parseColor("#3950E7")
        val gray = Color.parseColor("#8A8A8A")

        spannable.setSpan(ForegroundColorSpan(blue), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 1, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 6, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(blue), 7, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 8, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 9, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(blue), 10, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 11, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        title.text = spannable
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex == -1) return null
            cursor.moveToFirst()
            return cursor.getString(nameIndex)
        }
        return null
    }
}