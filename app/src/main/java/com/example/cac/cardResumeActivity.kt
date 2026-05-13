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
import android.view.Gravity
import androidx.core.content.res.ResourcesCompat
class cardResumeActivity : AppCompatActivity() {
    private fun appFont(fontRes: Int) = ResourcesCompat.getFont(this, fontRes)
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

    private lateinit var cardRadar: View
    private lateinit var cardCompetency: View
    private lateinit var competencyList: LinearLayout
    private lateinit var radarChart: com.github.mikephil.charting.charts.RadarChart
    private lateinit var txtResult: TextView

    private lateinit var txtResumeOnlyScore: TextView
    private lateinit var txtResumeComment: TextView
    private lateinit var txtResumeSummary: TextView

    private lateinit var chipGroupMissing: ChipGroup
    private lateinit var qualificationList: LinearLayout

    private lateinit var cardResumeScore: View
    private lateinit var cardMissing: View
    private lateinit var cardQualification: View

    private lateinit var cardCoverScore: View
    private lateinit var cardCoverFeedback: View
    private lateinit var cardRewrite: View

    private lateinit var txtCoverOnlyScore: TextView
    private lateinit var txtCoverComment: TextView

    private lateinit var coverFeedbackList: LinearLayout
    private lateinit var rewriteList: LinearLayout

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


        //양식버튼 클릭

        val btnFormDownload = findViewById<android.view.View>(R.id.btnformdownload)


        btnFormDownload.setOnClickListener {

            val url = "http://54.180.123.65:8000/static/index.html"

            // 인터넷 브라우저 열기
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        }




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


        //종합
        cardRadar = findViewById(R.id.cardRadar)
        cardCompetency = findViewById(R.id.cardCompetency)
        competencyList = findViewById(R.id.competencyList)
        radarChart = findViewById(R.id.radarChart)





        cardCoverScore = findViewById(R.id.cardCoverScore)
        cardCoverFeedback = findViewById(R.id.cardCoverFeedback)
        cardRewrite = findViewById(R.id.cardRewrite)

        txtCoverOnlyScore = findViewById(R.id.txtCoverOnlyScore)
        txtCoverComment = findViewById(R.id.txtCoverComment)

        coverFeedbackList = findViewById(R.id.coverFeedbackList)
        rewriteList = findViewById(R.id.rewriteList)


        cardResumeScore = findViewById(R.id.cardResumeScore)
        cardMissing = findViewById(R.id.cardMissing)
        cardQualification = findViewById(R.id.cardQualification)

//이력서 결과
        txtResumeOnlyScore = findViewById(R.id.txtResumeOnlyScore)
        txtResumeComment = findViewById(R.id.txtResumeComment)
        txtResumeSummary = findViewById(R.id.txtResumeSummary)

        chipGroupMissing = findViewById(R.id.chipGroupMissing)
        qualificationList = findViewById(R.id.qualificationList)


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
            btnAnalyzeCircle.isEnabled = false

            val uri = selectedUri
            if (uri == null) {
                Toast.makeText(this, "파일 먼저 선택", Toast.LENGTH_SHORT).show()
                btnAnalyzeCircle.isEnabled = true
                return@setOnClickListener
            }


            val token = SessionManager.getToken(this)

            if (token.isNullOrBlank()) {
                Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
                btnAnalyzeCircle.isEnabled = true
                return@setOnClickListener
            }

            val bearer = "Bearer $token"
            Log.d("UPLOAD", "사용 토큰: $bearer")

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

                        saveLatestResumeId(resumeId)

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


        competencyList.removeAllViews()
        cardRadar.visibility = View.GONE
        cardCompetency.visibility = View.GONE


        coverFeedbackList.removeAllViews()
        rewriteList.removeAllViews()

        txtCoverOnlyScore.text = ""
        txtCoverComment.text = ""

        cardCoverScore.visibility = View.GONE
        cardCoverFeedback.visibility = View.GONE
        cardRewrite.visibility = View.GONE

        chipGroupMissing.removeAllViews()
        qualificationList.removeAllViews()

        txtResumeOnlyScore.text = ""
        txtResumeComment.text = ""
        txtResumeSummary.text = ""

        cardResumeScore.visibility = View.GONE
        cardMissing.visibility = View.GONE
        cardQualification.visibility = View.GONE

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

    //최근이력서 저장
    private fun saveLatestResumeId(resumeId: Int) {
        val sharedPref = getSharedPreferences("ResumePrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("last_resume_id", resumeId)
            apply() // 비동기로 안전하게 저장
        }
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

        RetrofitClient.api.analyzeResumeById(bearer, resumeId)
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

    private fun addChips(
        group: ChipGroup,
        items: List<String>,
        colorCode: String
    ) {
        group.removeAllViews()

        fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

        for (t in items) {

            val chip = com.google.android.material.chip.Chip(this).apply {
                text = t
                typeface = appFont(R.font.pretendard_light)
                textSize = 13f
                chipMinHeight = dpToPx(42).toFloat()
                setEnsureMinTouchTargetSize(false)

                chipCornerRadius = dpToPx(10).toFloat() // 라운드 조절
                chipStrokeWidth = 0f
                chipStartPadding = dpToPx(12).toFloat()
                chipEndPadding = dpToPx(12).toFloat()


                chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.parseColor(colorCode))
                setTextColor(Color.WHITE) // 글씨는 모두 흰색
            }

            val lp = ChipGroup.LayoutParams(
                ChipGroup.LayoutParams.WRAP_CONTENT,
                ChipGroup.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dpToPx(1)
            lp.bottomMargin = dpToPx(8) // 행 간격 조절
            chip.layoutParams = lp

            group.addView(chip)
        }
    }

    private fun tryApplyResultJson(raw: String): Boolean {
        return try {
            val root = JSONObject(raw)


            val resultObj = root.optJSONObject("result")
            val resumeObj = resultObj?.optJSONObject("resume")

//종합
            val jobFit = resultObj?.optJSONObject("score_breakdown")?.optInt("job_fit", 0) ?: 0
            val specificity = resultObj?.optJSONObject("score_breakdown")?.optInt("specificity", 0) ?: 0
            val growthPotential = resultObj?.optJSONObject("score_breakdown")?.optInt("growth_potential", 0) ?: 0
            val persuasiveness = resultObj?.optJSONObject("score_breakdown")?.optInt("cover_persuasiveness", 0) ?: 0
            val completeness = resultObj?.optJSONObject("score_breakdown")?.optInt("completeness", 0) ?: 0


// 자소서
            val coverObj = resultObj?.optJSONObject("cover_letter")

            val coverScore = coverObj?.optInt("score", 0) ?: 0

            val motivationList = coverObj?.optJSONArray("motivation_feedback")?.let { arr ->
                List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
            } ?: emptyList()

            val experienceList = coverObj?.optJSONArray("experience_feedback")?.let { arr ->
                List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
            } ?: emptyList()

            val collaborationList = coverObj?.optJSONArray("collaboration_feedback")?.let { arr ->
                List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
            } ?: emptyList()

            val aspirationList = coverObj?.optJSONArray("aspiration_feedback")?.let { arr ->
                List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
            } ?: emptyList()

            val rewriteArr = coverObj?.optJSONArray("rewrite_suggestions")

            val coverFeedbackCards = mutableListOf<Pair<String, String>>()

            if (motivationList.isNotEmpty()) {
                coverFeedbackCards.add(
                    "지원 동기" to motivationList.joinToString("\n\n")
                )
            }

            if (experienceList.isNotEmpty()) {
                coverFeedbackCards.add(
                    "경험 서술" to experienceList.joinToString("\n\n")
                )
            }

            if (collaborationList.isNotEmpty()) {
                coverFeedbackCards.add(
                    "협업 경험" to collaborationList.joinToString("\n\n")
                )
            }

            if (aspirationList.isNotEmpty()) {
                coverFeedbackCards.add(
                    "입사 후 포부" to aspirationList.joinToString("\n\n")
                )
            }

            val rewriteCardItems = mutableListOf<Triple<String, String, String>>()
            if (rewriteArr != null) {
                for (i in 0 until rewriteArr.length()) {
                    val obj = rewriteArr.optJSONObject(i) ?: continue
                    val before = obj.optString("before")
                    val after = obj.optString("after")
                    val reason = obj.optString("reason")
                    rewriteCardItems.add(Triple(before, after, reason))
                }
            }

            txtCoverOnlyScore.text = coverScore.toString()

            txtCoverComment.text = when {
                coverScore >= 85 -> "전반적으로 우수한 자기소개서입니다."
                coverScore >= 70 -> "전반적으로 양호한 자기소개서입니다."
                else -> "보완이 필요한 자기소개서입니다."
            }





            //이력서 결과
            val hasResume = resultObj?.optBoolean("has_resume", false) ?: false
            val hasCoverLetter = resultObj?.optBoolean("has_cover_letter", false) ?: false

            //확인
            Log.d("CHECK_DOC_TYPE", "hasResume=$hasResume, hasCoverLetter=$hasCoverLetter")

            val resumeScore = resumeObj?.optInt("score", 0) ?: 0
            val resumeSummaryText = resumeObj?.optString("summary").orEmpty()

            val missingSkillsArr = resumeObj?.optJSONArray("missing_skills")
            val qualificationArr = resumeObj?.optJSONArray("recommended_qualifications")

            val missingList = missingSkillsArr?.let { arr ->
                List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
            } ?: emptyList()


            txtResumeOnlyScore.text = resumeScore.toString()

            txtResumeComment.text = when {
                resumeScore >= 85 -> "전반적으로 우수한 이력서입니다."
                resumeScore >= 70 -> "전반적으로 양호한 이력서입니다."
                else -> "보완이 필요한 이력서입니다."
            }

            txtResumeSummary.visibility= View.GONE

            val qualificationItems = qualificationArr?.let { arr ->
                List(arr.length()) { i ->
                    val obj = arr.optJSONObject(i)
                    obj?.optString("name").orEmpty()
                }.filter { it.isNotBlank() }
            } ?: emptyList()

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

            //차트/카드 채우기(종합)
            setupRadarChart(jobFit, specificity, growthPotential, persuasiveness, completeness)

            competencyList.removeAllViews()

            addCompetencyScoreCard(
                competencyList,
                "기술 역량",
                technicalScore,
                competencyObj?.optJSONObject("technical")?.optString("reason").orEmpty(),
                competencyObj?.optJSONObject("technical")?.optString("improvement").orEmpty()
            )

            addCompetencyScoreCard(
                competencyList,
                "열정",
                passionScore,
                competencyObj?.optJSONObject("passion")?.optString("reason").orEmpty(),
                competencyObj?.optJSONObject("passion")?.optString("improvement").orEmpty()
            )

            addCompetencyScoreCard(
                competencyList,
                "커뮤니케이션",
                communicationScore,
                competencyObj?.optJSONObject("communication")?.optString("reason").orEmpty(),
                competencyObj?.optJSONObject("communication")?.optString("improvement").orEmpty()
            )

            addCompetencyScoreCard(
                competencyList,
                "협업 능력",
                collaborationScore,
                competencyObj?.optJSONObject("collaboration")?.optString("reason").orEmpty(),
                competencyObj?.optJSONObject("collaboration")?.optString("improvement").orEmpty()
            )

            addCompetencyScoreCard(
                competencyList,
                "문제 해결 능력",
                problemSolvingScore,
                competencyObj?.optJSONObject("problem_solving")?.optString("reason").orEmpty(),
                competencyObj?.optJSONObject("problem_solving")?.optString("improvement").orEmpty()
            )

            //  UI 적용
            resultSection.visibility = View.VISIBLE

            findViewById<View>(R.id.cardScore).visibility = View.VISIBLE
            cardRadar.visibility = View.VISIBLE
            cardCompetency.visibility = View.VISIBLE

            cardResumeScore.visibility = if (hasResume) View.VISIBLE else View.GONE
            findViewById<View>(R.id.cardExtracted).visibility = if (hasResume) View.VISIBLE else View.GONE
            findViewById<View>(R.id.cardRequired).visibility = if (hasResume) View.VISIBLE else View.GONE
            findViewById<View>(R.id.cardStrength).visibility = if (hasResume) View.VISIBLE else View.GONE
            findViewById<View>(R.id.cardImprove).visibility = if (hasResume) View.VISIBLE else View.GONE
            cardMissing.visibility = if (hasResume) View.VISIBLE else View.GONE
            cardQualification.visibility = if (hasResume) View.VISIBLE else View.GONE


            txtScore.text = if (totalScore >= 0) totalScore.toString() else "-"


            // 1. 추출된 스킬 및 역량 (진한 파랑)
            addChips(
                chipGroupExtracted,
                if (extractedList.isNotEmpty()) extractedList else listOf("스킬 없음"),
                "#3950E7"
            )

// 2. 희망 직무 요구 역량 (검정)

            addChips(
                chipGroupRequired,
                if (targetList.isNotEmpty()) targetList else listOf("역량 없음"),
                "#222222"
            )

// 3. 부족한 역량 (연한 파랑)
            addChips(
                chipGroupMissing,
                if (missingList.isNotEmpty()) missingList else listOf("부족한 역량 없음"),
                "#7D8DF3"
            )


//            addChips(
//                chipGroupExtracted,
//                if (extractedList.isNotEmpty()) extractedList else listOf("스킬 없음"),
//                isBlue = true
//            )
//
//            addChips(
//                chipGroupRequired,
//                if (targetList.isNotEmpty()) targetList else listOf("역량 없음"),
//                isBlue = false
//            )

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


            addSimpleItems(
                qualificationList,
                if (qualificationItems.isNotEmpty()) qualificationItems else listOf("추천 자격증 없음")
            )

            addCoverFeedbackCards(
                coverFeedbackList,
                if (coverFeedbackCards.isNotEmpty()) coverFeedbackCards
                else listOf("자소서 피드백" to "피드백 없음")
            )

            addRewriteCards(
                rewriteList,
                rewriteCardItems
            )


            cardCoverScore.visibility = if (hasCoverLetter) View.VISIBLE else View.GONE
            cardCoverFeedback.visibility = if (hasCoverLetter) View.VISIBLE else View.GONE
            cardRewrite.visibility = if (hasCoverLetter) View.VISIBLE else View.GONE


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

    private fun addSimpleItems(parent: LinearLayout, items: List<String>) {
        parent.removeAllViews()
        fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

        for ((idx, text) in items.withIndex()) {

            val outerCard = com.google.android.material.card.MaterialCardView(this).apply {
                radius = dpToPx(16).toFloat() // 모서리
                cardElevation = 0f // 그림자
                setCardBackgroundColor(Color.WHITE)
                strokeColor = Color.parseColor("#E0E0E0") // 회색 테두리
                strokeWidth = dpToPx(1) // 테두리 두께
                useCompatPadding = true // 내부 여백 확보
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(1) // 카드 간의 간격
                }
            }


            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL // 숫자와 내용을 세로 중앙 정렬
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            }

            //동그란 숫자 배경
            val txtNum = TextView(this).apply {
                this.text = "${idx + 1}"
                textSize = 14f
                setTextColor(Color.WHITE)
                typeface = appFont(R.font.pretendard_bold)
                gravity = Gravity.CENTER // 숫자를 중앙에
                setBackgroundResource(R.drawable.bg_circle_black)

                layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(28)).apply {
                    marginEnd = dpToPx(16) // 숫자와 내용 사이의 간격
                }
            }

            // 실제 내용 텍스트
            val txtContent = TextView(this).apply {
                this.text = text
                textSize = 15f
                setTextColor(Color.parseColor("#333333"))
                typeface = appFont(R.font.pretendard_regular)
                setLineSpacing(dpToPx(4).toFloat(), 1.1f) // 줄간격을 넓혀서 가독성 확보
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }


            container.addView(txtNum)
            container.addView(txtContent)
            outerCard.addView(container)

            parent.addView(outerCard)
        }
    }

    private fun showDummyResult() {
        resultSection.visibility = View.VISIBLE
        txtScore.text = "85"

//        addChips(
//            chipGroupExtracted,
//            listOf("React", "TypeScript", "Node.js", "Python", "Git/GitHub", "팀 협업"),
//            isBlue = true
//        )
//
//        addChips(
//            chipGroupRequired,
//            listOf("React", "TypeScript", "Node.js", "Python", "Git/GitHub", "팀 협업"),
//            isBlue = false
//        )

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
        fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

        for ((idx, text) in items.withIndex()) {
            // 1. 최상위 카드 (회색 테두리 + 흰색 배경 + 둥근 모서리)
            val outerCard = com.google.android.material.card.MaterialCardView(this).apply {
                radius = dpToPx(16).toFloat() // 모서리를 둥글게
                cardElevation = 0f // 그림자는 없이
                setCardBackgroundColor(Color.WHITE)
                strokeColor = Color.parseColor("#E0E0E0") // 회색 테두리
                strokeWidth = dpToPx(1) // 테두리 두께
                useCompatPadding = true // 내부 여백 확보
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(1) // 카드 간의 간격
                }
            }

            // 안쪽 요소들을 묶어주는 컨테이너 (여백 넉넉하게)
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL // 숫자와 내용을 세로 중앙 정렬
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            }

            // 2. 동그란 숫자 배경
            val txtNum = TextView(this).apply {
                this.text = "${idx + 1}"
                textSize = 14f
                setTextColor(Color.WHITE)
                typeface = appFont(R.font.pretendard_bold)
                gravity = Gravity.CENTER // 숫자를 중앙에

                // 파란색 또는 검은색 원형 배경 설정 (기존 isBlue 활용)
                if (isBlue) {
                    setBackgroundResource(R.drawable.bg_circle_blue)
                } else {
                    setBackgroundResource(R.drawable.bg_circle_black)
                }

                layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(28)).apply {
                    marginEnd = dpToPx(16) // 숫자와 내용 사이의 간격
                }
            }

            // 3. 실제 내용 텍스트
            val txtContent = TextView(this).apply {
                this.text = text
                textSize = 15f
                setTextColor(Color.parseColor("#333333"))
                typeface = appFont(R.font.pretendard_regular)
                setLineSpacing(dpToPx(4).toFloat(), 1.1f) // 줄간격을 넓혀서 가독성 확보
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // 조립 (컨테이너에 넣고 카드에 담기)
            container.addView(txtNum)
            container.addView(txtContent)
            outerCard.addView(container)

            parent.addView(outerCard)
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



    private fun addCoverFeedbackCards(
        parent: LinearLayout,
        items: List<Pair<String, String>>
    ) {
        parent.removeAllViews()

        // 화면 크기에 맞게 자동으로 여백/크기를 조절해주는 함수
        fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

        for ((title, content) in items) {
            //최상위 카드
            val outerCard = com.google.android.material.card.MaterialCardView(this).apply {
                radius = dpToPx(20).toFloat()
                cardElevation = dpToPx(4).toFloat()
                setCardBackgroundColor(Color.WHITE)
                strokeWidth = 0
                useCompatPadding = true // 그림자가 잘리지 않도록 여백 확보
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(8)
                }
            }

            // 안쪽 요소들을 묶어주는 넉넉한 여백의 컨테이너
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24))
            }

            // 항목 제목 ("지원 동기", "경험 서술" 등)
            val titleText = TextView(this).apply {
                text = title
                textSize = 18f
                setTextColor(Color.parseColor("#111111"))
                typeface = appFont(R.font.pretendard_bold)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // 피드백 소제목 라벨
            val feedbackLabel = TextView(this).apply {
                text = "• AI 피드백"
                textSize = 13f
                setTextColor(Color.parseColor("#647AF6")) // 메인 블루 컬러
                typeface = appFont(R.font.pretendard_bold)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(20)
                    bottomMargin = dpToPx(8)
                }
            }


            val feedbackCard = com.google.android.material.card.MaterialCardView(this).apply {
                radius = dpToPx(12).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(Color.parseColor("#F0F4FF")) // 아주 연한 파란색 바탕
                strokeWidth = 0
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            val feedbackTextContent = TextView(this).apply {
                text = content
                textSize = 14f
                setTextColor(Color.parseColor("#333333"))
                typeface = appFont(R.font.pretendard_regular)
                setLineSpacing(dpToPx(6).toFloat(), 1.2f) // 줄간격을 넓혀서 가독성 확보
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            }
            feedbackCard.addView(feedbackTextContent)


            container.addView(titleText)
            container.addView(feedbackLabel)
            container.addView(feedbackCard)

            outerCard.addView(container)
            parent.addView(outerCard)
        }
    }

    private fun addRewriteCards(
        parent: LinearLayout,
        items: List<Triple<String, String, String>>
    ) {
        parent.removeAllViews()

        for ((before, after, reason) in items) {
            val outerCard = com.google.android.material.card.MaterialCardView(this)
            outerCard.radius = 18f
            outerCard.cardElevation = 0f
            outerCard.strokeWidth = 0
            outerCard.setCardBackgroundColor(Color.parseColor("#F2F2F2"))




            val container = LinearLayout(this)
            container.orientation = LinearLayout.VERTICAL
            container.setPadding(24, 24, 24, 24)

            val beforeCard = com.google.android.material.card.MaterialCardView(this)
            beforeCard.radius = 20f
            beforeCard.cardElevation = 0f
            beforeCard.strokeWidth = 0
            beforeCard.setCardBackgroundColor(Color.WHITE)

            val beforeWrap = LinearLayout(this)
            beforeWrap.orientation = LinearLayout.VERTICAL
            beforeWrap.setPadding(45, 30, 45, 38)

            val beforeTitle = TextView(this)
            beforeTitle.text = "Before"
            beforeTitle.textSize = 15f
            beforeTitle.setTextColor(Color.parseColor("#333333"))
            beforeTitle.typeface = appFont(R.font.pretendard_bold)

            val beforeContent = TextView(this)
            beforeContent.text = before
            beforeContent.textSize = 15f
            beforeContent.setTextColor(Color.parseColor("#333333"))
            beforeContent.setLineSpacing(8f, 1.1f)
            beforeContent.setPadding(0, 8, 0, 0)
            beforeContent.typeface = appFont(R.font.pretendard_light)

            beforeWrap.addView(beforeTitle)
            beforeWrap.addView(beforeContent)
            beforeCard.addView(beforeWrap)

            val arrowText = TextView(this)
            arrowText.text = "↓"
            arrowText.textSize = 34f
            arrowText.setTextColor(Color.parseColor("#111111"))
            arrowText.gravity = android.view.Gravity.CENTER

            val arrowParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            arrowParams.topMargin = 18
            arrowParams.bottomMargin = 18
            arrowText.layoutParams = arrowParams

            val afterCard = com.google.android.material.card.MaterialCardView(this)
            afterCard.radius = 20f
            afterCard.cardElevation = 0f
            afterCard.strokeWidth = 0
            afterCard.setCardBackgroundColor(Color.parseColor("#4A5BFF"))

            val afterWrap = LinearLayout(this)
            afterWrap.orientation = LinearLayout.VERTICAL
            afterWrap.setPadding(45, 30, 45, 38)

            val afterTitle = TextView(this)
            afterTitle.text = "After"
            afterTitle.textSize = 15f
            afterTitle.setTextColor(Color.WHITE)
            afterTitle.typeface = appFont(R.font.pretendard_bold)

            val afterContent = TextView(this)
            afterContent.text = after
            afterContent.textSize = 15f
            afterContent.setTextColor(Color.WHITE)
            afterContent.setLineSpacing(8f, 1.1f)
            afterContent.setPadding(0, 8, 0, 0)
            afterContent.typeface = appFont(R.font.pretendard_light)

            afterWrap.addView(afterTitle)
            afterWrap.addView(afterContent)
            afterCard.addView(afterWrap)

            val reasonText = TextView(this)
            reasonText.text = reason
            reasonText.textSize = 14f
            reasonText.setTextColor(Color.parseColor("#666666"))
            reasonText.gravity = android.view.Gravity.CENTER
            reasonText.setLineSpacing(6f, 1.1f)
            reasonText.setPadding(25, 28, 25, 20)
            reasonText.typeface = appFont(R.font.pretendard_light)

            container.addView(beforeCard)
            container.addView(arrowText)
            container.addView(afterCard)
            container.addView(reasonText)

            outerCard.addView(container)

            val outerParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            outerParams.topMargin = 16
            parent.addView(outerCard, outerParams)
        }
    }

    private fun setupRadarChart(
        jobFit: Int,
        specificity: Int,
        growthPotential: Int,
        persuasiveness: Int,
        completeness: Int
    ) {
        val entries = listOf(
            com.github.mikephil.charting.data.RadarEntry(jobFit.toFloat()),
            com.github.mikephil.charting.data.RadarEntry(specificity.toFloat()),
            com.github.mikephil.charting.data.RadarEntry(growthPotential.toFloat()),
            com.github.mikephil.charting.data.RadarEntry(persuasiveness.toFloat()),
            com.github.mikephil.charting.data.RadarEntry(completeness.toFloat())
        )

        val dataSet = com.github.mikephil.charting.data.RadarDataSet(entries, "")
        dataSet.color = Color.parseColor("#4A5BFF")
        dataSet.fillColor = Color.parseColor("#4A5BFF")
        dataSet.setDrawFilled(true)
        dataSet.fillAlpha = 90
        dataSet.lineWidth = 2f

        val data = com.github.mikephil.charting.data.RadarData(dataSet)
        data.setDrawValues(false)

        radarChart.data = data
        radarChart.description.isEnabled = false
        radarChart.legend.isEnabled = false
        radarChart.webLineWidth = 1f
        radarChart.webColor = Color.parseColor("#BDBDBD")
        radarChart.webLineWidthInner = 1f
        radarChart.webColorInner = Color.parseColor("#D9D9D9")
        radarChart.yAxis.axisMinimum = 0f
        radarChart.yAxis.axisMaximum = 100f
        radarChart.yAxis.labelCount = 5
        radarChart.yAxis.textColor = Color.parseColor("#999999")

        val labels = listOf("직무 적합도", "구체성", "성장 가능성", "설득력", "내용 완성도")
        radarChart.xAxis.valueFormatter =
            com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        radarChart.xAxis.textSize = 12f
        radarChart.xAxis.textColor = Color.parseColor("#333333")

        radarChart.invalidate()
    }

    private fun addCompetencyScoreCard(
        parent: LinearLayout,
        title: String,
        score: Int,
        reason: String,
        improvement: String
    ) {
        val outerCard = com.google.android.material.card.MaterialCardView(this)
        outerCard.radius = 18f
        outerCard.cardElevation = 0f
        outerCard.strokeWidth = 0
        outerCard.setCardBackgroundColor(Color.parseColor("#F2F2F2"))

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(40, 35, 40, 32)

        val topRow = LinearLayout(this)
        topRow.orientation = LinearLayout.HORIZONTAL
        topRow.gravity = Gravity.CENTER_VERTICAL

        val titleView = TextView(this)
        titleView.text = title
        titleView.textSize = 16f
        titleView.setTextColor(Color.parseColor("#333333"))
        titleView.typeface = appFont(R.font.pretendard_regular)
        titleView.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val scoreView = TextView(this)
        scoreView.text = "$score"
        scoreView.textSize = 22f
        scoreView.setTextColor(Color.parseColor("#4A5BFF"))
        scoreView.typeface = appFont(R.font.pretendard_bold)

        val suffixView = TextView(this)
        suffixView.text = "/100"
        suffixView.textSize = 14f
        suffixView.setTextColor(Color.parseColor("#999999"))
        suffixView.typeface = appFont(R.font.pretendard_regular)
        suffixView.setPadding(6, 6, 0, 0)

        topRow.addView(titleView)
        topRow.addView(scoreView)
        topRow.addView(suffixView)

        val progressWrap = android.widget.FrameLayout(this)
        val progressParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            20
        )
        progressParams.topMargin = 14
        progressWrap.layoutParams = progressParams

        val bgDrawable = android.graphics.drawable.GradientDrawable()
        bgDrawable.setColor(Color.parseColor("#D9D9D9"))
        bgDrawable.cornerRadius = 999f

        val progressBg = View(this)
        val bgParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            18
        )
        progressBg.layoutParams = bgParams
        progressBg.background = bgDrawable
        progressWrap.addView(progressBg)

        val fillDrawable = android.graphics.drawable.GradientDrawable()
        fillDrawable.setColor(Color.parseColor("#4A5BFF"))
        fillDrawable.cornerRadius = 999f

        val progressFill = View(this)
        val fillWidth = (score * 6).coerceAtMost(600)
        val fillParams = android.widget.FrameLayout.LayoutParams(
            fillWidth,
            18
        )
        progressFill.layoutParams = fillParams
        progressFill.background = fillDrawable
        progressWrap.addView(progressFill)

        val reasonCard = com.google.android.material.card.MaterialCardView(this)
        reasonCard.radius = 16f
        reasonCard.cardElevation = 0f
        reasonCard.strokeWidth = 0
        reasonCard.setCardBackgroundColor(Color.WHITE)

        val reasonText = TextView(this)
        reasonText.text = reason
        reasonText.textSize = 15f
        reasonText.setTextColor(Color.parseColor("#333333"))
        reasonText.typeface = appFont(R.font.pretendard_regular)
        reasonText.setLineSpacing(6f, 1.1f)
        reasonText.setPadding(24, 24, 24, 24)
        reasonCard.addView(reasonText)


        val reasonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        reasonParams.topMargin = 35
        reasonCard.layoutParams = reasonParams

        val improvementCard = com.google.android.material.card.MaterialCardView(this)
        improvementCard.radius = 16f
        improvementCard.cardElevation = 0f
        improvementCard.strokeWidth = 0
        improvementCard.setCardBackgroundColor(Color.WHITE)

        val improvementText = TextView(this)
        improvementText.text = improvement
        improvementText.textSize = 15f
        improvementText.setTextColor(Color.parseColor("#333333"))
        improvementText.typeface = appFont(R.font.pretendard_regular)
        improvementText.setLineSpacing(6f, 1.1f)
        improvementText.setPadding(24, 24, 24, 24)
        improvementCard.addView(improvementText)

        val improvementParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        improvementParams.topMargin = 14
        improvementCard.layoutParams = improvementParams

        container.addView(topRow)
        container.addView(progressWrap)
        container.addView(reasonCard)
        container.addView(improvementCard)

        outerCard.addView(container)

        val outerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        outerParams.topMargin = 18
        parent.addView(outerCard, outerParams)
    }
    }