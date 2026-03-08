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
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.example.cac.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Context
import org.json.JSONArray

class cardResumeActivity : AppCompatActivity() {
    private lateinit var txtAnalyze: TextView
    private var pollingTry = 0
    private val pollingMax = 20
    private val pollingDelayMs = 2000L

    private var selectedUri: Uri? = null
    private lateinit var fileRow: View
    private lateinit var txtFileName: TextView
    private lateinit var btnRemoveFile: ImageButton
    private lateinit var btnPickFile: MaterialButton
    private lateinit var btnAnalyzeCircle: View

    private lateinit var scrollContent: NestedScrollView
    private lateinit var resultSection: View
    private lateinit var txtScore: TextView

    private lateinit var chipGroupExtracted: ChipGroup
    private lateinit var chipGroupRequired: ChipGroup
    private lateinit var strengthList: LinearLayout
    private lateinit var improveList: LinearLayout

    private lateinit var txtResult: TextView

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedUri = uri
            showPickedState(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_card_resume)


        //버튼 클릭시
        val btnRoadmap = findViewById<MaterialButton>(R.id.btnRoadmap)
        btnRoadmap.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        //분석중
        txtAnalyze = findViewById(R.id.btnAnalyze)



        findViewById<TextView>(R.id.btnNoticev2).setOnClickListener {
            startActivity(Intent(this, MYActivity::class.java))
        }
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<TextView>(R.id.btnNoticev).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

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

        fileRow = findViewById(R.id.fileRow)
        txtFileName = findViewById(R.id.txtFileName)
        btnRemoveFile = findViewById(R.id.btnRemoveFile)
        btnPickFile = findViewById(R.id.btnPickFile)
        btnAnalyzeCircle = findViewById(R.id.btnAnalyzeCircle)

        scrollContent = findViewById(R.id.scrollContent)
        resultSection = findViewById(R.id.resultSection)
        txtScore = findViewById(R.id.txtScore)
        chipGroupExtracted = findViewById(R.id.chipGroupExtracted)
        chipGroupRequired = findViewById(R.id.chipGroupRequired)
        strengthList = findViewById(R.id.strengthList)
        improveList = findViewById(R.id.improveList)

        txtResult = findViewById(R.id.txtResult)

        showEmptyState()
        hideResultSection()

        btnPickFile.setOnClickListener {
            pickFileLauncher.launch(arrayOf("application/pdf"))
        }

        btnRemoveFile.setOnClickListener {
            selectedUri = null
            showEmptyState()
            hideResultSection()
        }

        btnAnalyzeCircle.setOnClickListener {
            txtAnalyze.text = "분석중..."
            txtAnalyze.isEnabled = false

            val uri = selectedUri
            if (uri == null) {
                Toast.makeText(this, "파일 먼저 선택", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val token = Session.accessToken
            if (token.isNullOrBlank()) {
                Toast.makeText(this, "로그인 필요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideResultSection()
            txtResult.text = "업로드 중..."

            val bearer = "Bearer $token"
            val part = uriToMultipart(uri)

            RetrofitClient.api.uploadResume(bearer, part)
                .enqueue(object : Callback<Map<String, Any>> {
                    override fun onResponse(
                        call: Call<Map<String, Any>>,
                        response: Response<Map<String, Any>>
                    ) {
                        if (!response.isSuccessful) {
                            Log.d("UPLOAD", "upload fail code=${response.code()}")
                            txtResult.text = "업로드 실패: ${response.code()}"
                            txtAnalyze.text = "AI분석"
                            btnAnalyzeCircle.isEnabled = true
                            return
                        }

                        val body = response.body()
                        Log.d("UPLOAD", "body=$body")

                        val rawResumeId = body?.get("resume_id")
                        Log.d("UPLOAD", "rawResumeId=$rawResumeId, class=${rawResumeId?.javaClass?.name}")

                        val resumeId = when (rawResumeId) {
                            is Number -> rawResumeId.toInt()
                            is String -> rawResumeId.toDoubleOrNull()?.toInt()
                            else -> null
                        }

                        if (resumeId == null) {
                            Log.d("UPLOAD", "resume_id 파싱 실패")
                            txtResult.text = "resume_id 파싱 실패: $body"
                            txtAnalyze.text = "AI분석"
                            btnAnalyzeCircle.isEnabled = true
                            return
                        }

                        Log.d("UPLOAD", "resumeId parsed=$resumeId")
                        txtResult.text = "분석 요청 중..."
                        requestAnalyze(resumeId)
                    }

                    override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                        Log.d("UPLOAD", "upload failure message=${t.message}")
                        txtResult.text = "업로드 통신 실패: ${t.message}"
                        txtAnalyze.text = "AI분석"
                        btnAnalyzeCircle.isEnabled = true
                    }
                })
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun hideResultSection() {
        resultSection.visibility = View.GONE
        chipGroupExtracted.removeAllViews()
        chipGroupRequired.removeAllViews()
        strengthList.removeAllViews()
        improveList.removeAllViews()
        txtScore.text = ""

    }

    private fun showEmptyState() {
        fileRow.visibility = View.GONE
        btnPickFile.visibility = View.VISIBLE
        txtFileName.text = ""
        txtResult.text = ""
    }

    private fun showPickedState(uri: Uri) {
        txtFileName.text = queryDisplayName(uri) ?: "선택한 파일"
        fileRow.visibility = View.VISIBLE
        btnPickFile.visibility = View.GONE
    }

    private fun uriToMultipart(uri: Uri): MultipartBody.Part {
        val originalName = queryDisplayName(uri) ?: "resume.pdf"
        val safeName =
            if (originalName.endsWith(".pdf", ignoreCase = true)) originalName else "$originalName.pdf"

        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("파일을 열 수 없음")

        val requestBody = bytes.toRequestBody("application/pdf".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("file", safeName, requestBody)
    }

    private fun requestAnalyze(resumeId: Int) {
        Log.d("ANALYZE", "requestAnalyze called, resumeId=$resumeId")

        val token = Session.accessToken
        if (token.isNullOrBlank()) {
            txtResult.text = "토큰 없음"
            txtAnalyze.text = "AI분석"
            btnAnalyzeCircle.isEnabled = true
            return
        }

        val bearer = "Bearer $token"
        Log.d("ANALYZE", "calling analyze API with bearer exists=${bearer.isNotBlank()}")

        RetrofitClient.api.analyzeResumeById(bearer, resumeId.toString())
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    Log.d("ANALYZE", "response code=${response.code()}, body=${response.body()}")

                    if (!response.isSuccessful) {
                        txtResult.text = "분석 실패: ${response.code()}"
                        txtAnalyze.text = "AI분석"
                        btnAnalyzeCircle.isEnabled = true
                        return
                    }

                    txtResult.text = "결과 조회 중..."
                    pollingTry = 0
                    pollAnalysisPublic(resumeId)
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    Log.d("ANALYZE", "failure message=${t.message}")
                    txtResult.text = "분석 통신 실패: ${t.message}"
                    txtAnalyze.text = "AI분석"
                    btnAnalyzeCircle.isEnabled = true
                }
            })
    }




    private fun pollAnalysisPublic(resumeId: Int) {
        if (pollingTry >= pollingMax) {
            txtResult.text = "결과 생성 지연(시간 초과). 잠시 후 다시 시도해줘"
            return
        }

        pollingTry += 1
        txtResult.text = "결과 조회 중... (${pollingTry}/${pollingMax})"

        RetrofitClient.api.getAnalysisPublic(resumeId)
            .enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
                override fun onResponse(
                    call: retrofit2.Call<okhttp3.ResponseBody>,
                    response: retrofit2.Response<okhttp3.ResponseBody>
                ) {
                    if (!response.isSuccessful) {
                        txtResult.postDelayed({ pollAnalysisPublic(resumeId) }, pollingDelayMs)
                        return
                    }

                    val raw = response.body()?.string().orEmpty()
                    Log.d("PUBLIC", "raw=$raw")

                    val notReady =
                        raw.isBlank() ||
                                raw.contains("\"result\":null") ||
                                raw.contains("\"resume\":null")

                    if (notReady) {
                        txtResult.postDelayed({ pollAnalysisPublic(resumeId) }, pollingDelayMs)
                        return
                    }

                    applyResultFromRaw(raw)

                }

                override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                    txtResult.postDelayed({ pollAnalysisPublic(resumeId) }, pollingDelayMs)
                }
            })
    }

    private fun applyResultFromRaw(raw: String) {
        val ok = tryApplyResultJson(raw)
        if (!ok) {
            Log.d("RESULT_PARSE", "applyResultFromRaw failed, raw=$raw")
            showDummyResult()
            txtResult.text = raw
            txtAnalyze.text = "AI분석"
            btnAnalyzeCircle.isEnabled = true
            return
        }

        txtResult.text = ""
        txtAnalyze.text = "AI분석"
        btnAnalyzeCircle.isEnabled = true
    }

    private fun addChips(group: com.google.android.material.chip.ChipGroup, items: List<String>, isBlue: Boolean) {
        group.removeAllViews()
        val layout = if (isBlue) R.layout.chip_item_blue else R.layout.chip_item_black

        for (t in items) {
            val chip = layoutInflater.inflate(layout, group, false) as com.google.android.material.chip.Chip
            chip.text = t
            group.addView(chip)
        }
    }

    private fun tryApplyResultJson(raw: String): Boolean {
        return try {
            val root = JSONObject(raw)


            val resultObj = root.optJSONObject("result")
            val resumeObj = resultObj?.optJSONObject("resume")


            //역량 점수
            val competencyObj = resultObj?.optJSONObject("competency_scores")

            val technicalScore = competencyObj?.optJSONObject("technical")?.optInt("score", 0) ?: 0
            val passionScore = competencyObj?.optJSONObject("passion")?.optInt("score", 0) ?: 0
            val communicationScore = competencyObj?.optJSONObject("communication")?.optInt("score", 0) ?: 0
            val collaborationScore = competencyObj?.optJSONObject("collaboration")?.optInt("score", 0) ?: 0
            val problemSolvingScore = competencyObj?.optJSONObject("problem_solving")?.optInt("score", 0) ?: 0

            saveCompetencyScores(
                technicalScore,
                passionScore,
                communicationScore,
                collaborationScore,
                problemSolvingScore
            )

            Log.d(
                "COMPETENCY",
                "saved scores technical=$technicalScore, passion=$passionScore, communication=$communicationScore, collaboration=$collaborationScore, problemSolving=$problemSolvingScore"
            )

//학습현황
            val learningPlanObj = resumeObj?.optJSONObject("learning_plan")

            val studyPathsArr = learningPlanObj?.optJSONArray("study_paths")

            val studyPathList = studyPathsArr?.let { arr ->
                List(arr.length()) { i ->
                    val obj = arr.optJSONObject(i)
                    val type = obj?.optString("type").orEmpty()
                    val description = obj?.optString("description").orEmpty()
                    "$type - $description"
                }.filter { it.isNotBlank() }
            } ?: emptyList()

            saveLearningTodo(studyPathList)
            Log.d("LEARNING", "saved studyPathList=$studyPathList")



            val totalScore = resultObj?.optInt("total_score", -1) ?: -1

            val extractedSkillsArr = resumeObj?.optJSONArray("extracted_skills")
            val targetJobSkillsArr = resumeObj?.optJSONArray("target_job_skills")

            val strengthsArr = resumeObj?.optJSONArray("strengths")

            val missingSkillsArr = resumeObj?.optJSONArray("missing_skills")
            val skillRecArr = resumeObj?.optJSONArray("skill_recommendations")

            val extractedList = extractedSkillsArr?.let { arr ->
                List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
            } ?: emptyList()

            val targetList = targetJobSkillsArr?.let { arr ->
                List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
            } ?: emptyList()

            val strengthsList = strengthsArr?.let { arr ->
                List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
            } ?: emptyList()

            // 개선제안은 missing_skills 우선, 없으면 recommendations.action 사용
            val improveListFromMissing = missingSkillsArr?.let { arr ->
                List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
            } ?: emptyList()

            val improveListFromRec = skillRecArr?.let { arr ->
                List(arr.length()) { i ->
                    arr.optJSONObject(i)?.optString("action").orEmpty()
                }.filter { it.isNotBlank() }
            } ?: emptyList()

            val improveItems = when {
                improveListFromMissing.isNotEmpty() -> improveListFromMissing
                improveListFromRec.isNotEmpty() -> improveListFromRec
                else -> emptyList()
            }

            //  UI 적용
            resultSection.visibility = View.VISIBLE

            txtScore.text = if (totalScore >= 0) totalScore.toString() else "-"

            // 상위%가 서버에 없어서 대신 세부 점수 요약 문구로
            val breakdown = resultObj?.optJSONObject("score_breakdown")
            val jobFit = breakdown?.optInt("job_fit", -1) ?: -1
            val completeness = breakdown?.optInt("completeness", -1) ?: -1

                if (jobFit >= 0 && completeness >= 0) "직무적합 ${jobFit} · 완성도 ${completeness}"
                else ""

            addChips(
                chipGroupExtracted,
                if (extractedList.isNotEmpty()) extractedList else listOf("스킬 없음"),
                isBlue = true
            )

            addChips(
                chipGroupRequired,
                if (targetList.isNotEmpty()) targetList else listOf("역량 없음"),
                isBlue = false
            )

            addNumberItems(
                strengthList,
                if (strengthsList.isNotEmpty()) strengthsList else listOf("강점 데이터 없음"),
                isBlue = false
            )

            addNumberItems(
                improveList,
                if (improveItems.isNotEmpty()) improveItems else listOf("개선 제안 데이터 없음"),
                isBlue = true
            )


            // 버튼 텍스트 복구(넣어둔 경우)
            try {
                txtAnalyze.text = "AI분석"
                btnAnalyzeCircle.isEnabled = true
            } catch (_: Exception) {}

            scrollContent.post {
                scrollContent.smoothScrollTo(0, resultSection.top)
            }

            true
        } catch (e: Exception) {
            Log.d("RESULT_PARSE", "parse fail: ${e.message}")
            false
        }
    }

    private fun showDummyResult() {
        resultSection.visibility = View.VISIBLE
        txtScore.text = "85"

        addChips(
            chipGroupExtracted,
            listOf("React", "TypeScript", "Node.js", "Python", "Git/GitHub", "팀 협업"),
            isBlue = true
        )

        addChips(
            chipGroupRequired,
            listOf("React", "TypeScript", "Node.js", "Python", "Git/GitHub", "팀 협업"),
            isBlue = false
        )

        addNumberItems(
            strengthList,
            listOf(
                "구체적인 프로젝트 성과를 수치로 제시",
                "최신 기술 스택 (React, TypeScript) 사용 경험",
                "팀 프로젝트 경험이 풍부하고 협업 능력이 우수함"
            ),
            isBlue = false
        )

        addNumberItems(
            improveList,
            listOf(
                "백엔드 기술 스택 보강 필요 (Node.js, Database)",
                "클라우드/DevOps 경험 추가 권장 (AWS, Docker)",
                "오픈소스 기여 활동을 추가하면 경쟁력 향상"
            ),
            isBlue = true
        )

        scrollContent.post {
            scrollContent.smoothScrollTo(0, resultSection.top)
        }
    }


    private fun addNumberItems(parent: LinearLayout, items: List<String>, isBlue: Boolean) {
        parent.removeAllViews()
        for ((i, text) in items.withIndex()) {
            val card = com.google.android.material.card.MaterialCardView(this)
            card.radius = 12f
            card.cardElevation = 0f
            card.setCardBackgroundColor(Color.parseColor(if (isBlue) "#3950E7" else "#F2F2F2"))

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(18, 14, 18, 14)

            val num = TextView(this)
            num.text = "${i + 1}"
            num.textSize = 16f
            num.setTextColor(Color.parseColor(if (isBlue) "#FFFFFF" else "#111111"))
            num.setPadding(0, 0, 14, 0)

            val tv = TextView(this)
            tv.text = text
            tv.textSize = 15f
            tv.setTextColor(Color.parseColor(if (isBlue) "#FFFFFF" else "#111111"))

            row.addView(num)
            row.addView(tv)
            card.addView(row)

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 10
            parent.addView(card, lp)
        }
    }


    private fun saveLearningTodo(items: List<String>) {
        val prefs = getSharedPreferences("cac_pref", Context.MODE_PRIVATE)
        val jsonArray = JSONArray()

        for (item in items) {
            jsonArray.put(item)
        }

        prefs.edit()
            .putString("learning_todo", jsonArray.toString())
            .apply()
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

    private fun saveCompetencyScores(
        technical: Int,
        passion: Int,
        communication: Int,
        collaboration: Int,
        problemSolving: Int
    ) {
        val prefs = getSharedPreferences("cac_pref", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("score_technical", technical)
            .putInt("score_passion", passion)
            .putInt("score_communication", communication)
            .putInt("score_collaboration", collaboration)
            .putInt("score_problem_solving", problemSolving)
            .apply()
    }

}