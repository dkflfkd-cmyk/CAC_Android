package com.example.cac

import android.content.Context
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
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.cac.data.SessionRequest
import com.example.cac.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuestionSetupActivity : AppCompatActivity() {

    private var selectedJob: String? = null
    private var selectedType: String? = null
    private var selectedCount: Int? = null
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null
    private var selectedResumeId: Int? = null
    private var selectedS3Key: String? = null

    // 상단에 선언된 변수명들을 코드 전체에서 일치시킵니다.
    private lateinit var etJobInput: EditText
    private lateinit var txtType: TextView
    private lateinit var txtCount: TextView
    private lateinit var btnPickFile: MaterialButton
    private lateinit var fileRow: View
    private lateinit var txtFileName: TextView
    private lateinit var btnRemoveFile: ImageButton
    private lateinit var cbLoadExisting: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

//        val isRetry = intent.getBooleanExtra("is_retry", false)
//        val prefs = getSharedPreferences("InterviewPrefs", MODE_PRIVATE)
//        val lastSessionId = prefs.getInt("last_session_id", -1)
//
//        // 자동 이동 로직
//        if (!isRetry && lastSessionId != -1) {
//            val intent = Intent(this, QuestionListActivity::class.java).apply {
//                putExtra("session_id", lastSessionId)
//                putExtra("job", prefs.getString("last_job", ""))
//                putExtra("type", prefs.getString("last_type", ""))
//                putExtra("count", prefs.getInt("last_count", 5))
//                putExtra("is_from_setup", false)
//            }
//            startActivity(intent)
//            finish()
//            return
//        }

        setContentView(R.layout.activity_question_setup)
        findView() // 변수 초기화

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initResumeLogic()
        setupTopBottomButtons()
        setupPickers()
        setupFileButton()
        setupRemoveFileButton()
        setupStartButton()
        setupTitle()
    }

    private fun findView() {
        etJobInput = findViewById(R.id.etJobInput)
        txtType = findViewById(R.id.txtTypeHint)
        txtCount = findViewById(R.id.txtCountHint)
        btnPickFile = findViewById(R.id.btnPickFile)
        fileRow = findViewById(R.id.fileRow)
        txtFileName = findViewById(R.id.txtFileName)
        btnRemoveFile = findViewById(R.id.btnRemoveFile)
        cbLoadExisting = findViewById(R.id.cbLoadExisting)
    }

    private fun setupStartButton() {

        findViewById<View>(R.id.btnStartCircle).setOnClickListener {
            selectedJob = etJobInput.text.toString().trim()

            if (selectedJob.isNullOrEmpty() || selectedType == null || selectedCount == null) {
                Toast.makeText(this, "직무/유형/개수를 모두 선택해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    val loginUserId = sharedPref.getString("user_id", "sample04") ?: "sample04"


                    val request = SessionRequest(
                        user_id = loginUserId,
                        target_job = selectedJob!!,
                        question_count = selectedCount!!,
                        question_types = listOf(selectedType!!),
                        analysis_id = selectedResumeId,
                        pdf_s3_key = selectedS3Key
                    )

                    val response = RetrofitClient.api.createSession(request).execute()

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body() != null) {
                            val sid = response.body()!!.sessionId

                            getSharedPreferences("InterviewPrefs", MODE_PRIVATE).edit()
                                .putInt("last_session_id", sid)
                                .putString("last_job", selectedJob)
                                .putString("last_type", txtType.text.toString())
                                .putInt("last_count", selectedCount!!)
                                .apply()

                            val intent = Intent(this@QuestionSetupActivity, QuestionListActivity::class.java).apply {
                                putExtra("session_id", sid)
                                putExtra("job", selectedJob)
                                putExtra("type", txtType.text.toString())
                                putExtra("count", selectedCount)
                                putExtra("is_from_setup", true)
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@QuestionSetupActivity, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupPickers() {

        findViewById<View>(R.id.boxType).setOnClickListener {
            val items = arrayOf("기술 (Technical)", "행동 (Behavioral)", "프로젝트 (Project)", "역량 (Competency)", "산업 (Industry)")
            val serverValues = arrayOf("technical", "behavioral", "project", "competency", "industry")
            AlertDialog.Builder(this).setTitle("질문 유형").setItems(items) { _, which ->
                selectedType = serverValues[which]
                txtType.text = items[which]
                txtType.setTextColor(Color.parseColor("#111111"))
            }.show()
        }
        findViewById<View>(R.id.boxCount).setOnClickListener {
            val items = arrayOf("3", "5", "10")
            AlertDialog.Builder(this).setTitle("질문 개수").setItems(items) { _, which ->
                selectedCount = items[which].toInt()
                txtCount.text = "${selectedCount}개"
                txtCount.setTextColor(Color.parseColor("#111111"))
            }.show()
        }
    }


    private fun initResumeLogic() {
        cbLoadExisting.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val sharedPref = getSharedPreferences("ResumePrefs", Context.MODE_PRIVATE)
                val lastId = sharedPref.getInt("last_resume_id", -1)
                if (lastId != -1) {
                    selectedResumeId = lastId
                    btnPickFile.isEnabled = false
                    btnPickFile.alpha = 0.5f
                } else {
                    cbLoadExisting.isChecked = false
                    Toast.makeText(this, "기존 기록이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                selectedResumeId = null
                btnPickFile.isEnabled = true
                btnPickFile.alpha = 1.0f
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        selectedFileUri = uri
        selectedFileName = queryDisplayName(uri) ?: "이력서 파일"
        txtFileName.text = selectedFileName
        fileRow.visibility = View.VISIBLE
        btnPickFile.visibility = View.GONE
        cbLoadExisting.isEnabled = false
    }

    private fun setupFileButton() { btnPickFile.setOnClickListener { filePickerLauncher.launch(arrayOf("application/pdf")) } }

    private fun setupRemoveFileButton() {
        btnRemoveFile.setOnClickListener {
            selectedFileUri = null
            txtFileName.text = ""
            fileRow.visibility = View.GONE
            btnPickFile.visibility = View.VISIBLE
            cbLoadExisting.isEnabled = true
        }
    }

    private fun setupTopBottomButtons() {
        findViewById<TextView>(R.id.btnNoticev2).setOnClickListener { startActivity(Intent(this, MYActivity::class.java)) }
        findViewById<TextView>(R.id.btnInterview).setOnClickListener { startActivity(Intent(this, InterviewActivity::class.java)) }
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        findViewById<TextView>(R.id.btnNoticev).setOnClickListener { startActivity(Intent(this, DashboardActivity::class.java)) }
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
            if (nameIndex != -1 && cursor.moveToFirst()) return cursor.getString(nameIndex)
        }
        return null
    }
}