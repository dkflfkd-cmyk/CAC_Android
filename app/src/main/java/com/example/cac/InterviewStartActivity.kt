package com.example.cac

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView

class InterviewStartActivity : AppCompatActivity() {

    private lateinit var mainScrollView: NestedScrollView
    private lateinit var scrollAnswer: NestedScrollView
    private lateinit var btnMic: ImageButton
    private lateinit var micBackground: View
    private lateinit var layoutDots: View
    private lateinit var txtAnswer: TextView
    private lateinit var btnRefresh: ImageButton
    private lateinit var layoutFeedbackResult: View
    private lateinit var tooltipLayout: View
    private lateinit var imgStar: ImageView
    private lateinit var micWrapCard: View
    private lateinit var btnNextQuestion: View
    private lateinit var btnFinalMockInterview: View

    private var isRecording = false
    private var isTypingFinished = false
    private var isSaved = false          // 별표 저장 상태 기억하는 변수
    private var isFirstQuestion = true
    private var isLastQuestion = false

    private val handler = Handler(Looper.getMainLooper())
    private var typeRunnable: Runnable? = null
    private val demoText = "네, 사용자 경험을 개선한 프로젝트 경험이 있습니다. 제가 참여했던 프로젝트는 AI 면접 코치 모바일 앱이었는데, 초기 버전에서는 사용자가 이력서를 업로드한 후 분석 결과를 확인하는 과정에서 정보가 한 화면에 많이 표시되어 가독성이 떨어지는 문제가 있었습니다. 그래서 UI/UX를 개선하기 위해 사용자가 정보를 확인하는 흐름을 다시 설계했습니다. 먼저 핵심 정보를 빠르게 볼 수 있도록 '핵심 키워드 피드백 개선 방향' 순서로 화면 구조를 재정리했고, 카드 형태 UI와 색상 강조를 사용해 중요한 정보가 눈에 잘 들어오도록 디자인을 개선했습니다.\n\n그 결과 사용자가 분석 결과를 더 빠르게 이해할 수 있게 되었고, 테스트 과정에서도 정보 파악이 쉬워졌다는 피드백을 받을 수 있었습니다.\n\n이 경험을 통해 UI 디자인뿐 아니라 사용자의 사용 흐름을 고려한 UX 설계가 중요하다는 것을 배웠습니다."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview_start)

        setupBottomButtons()
        setupTitle()
        setupDemoRecording()
    }

    private fun setupDemoRecording() {
        mainScrollView = findViewById(R.id.mainScrollView)
        scrollAnswer = findViewById(R.id.scrollAnswer)
        btnMic = findViewById(R.id.btnMic)
        micBackground = findViewById(R.id.micBackground)
        layoutDots = findViewById(R.id.layoutDots)
        txtAnswer = findViewById(R.id.txtAnswer)
        btnRefresh = findViewById(R.id.btnRefresh)
        layoutFeedbackResult = findViewById(R.id.layoutFeedbackResult)
        tooltipLayout = findViewById(R.id.tooltipLayout)
        imgStar = findViewById(R.id.imgStar)
        micWrapCard = findViewById(R.id.micWrapCard)
        btnNextQuestion = findViewById(R.id.btnNextQuestion)
        btnFinalMockInterview = findViewById(R.id.btnFinalMockInterview)

        // 처음에 화면 켜질 때 점 3개 보임
        layoutDots.visibility = View.VISIBLE
        startDotAnimation()

        // 별 모양 클릭
        imgStar.setOnClickListener {
            isSaved = !isSaved
            if (isSaved) {
                imgStar.setImageResource(R.drawable.ic_star_filled)
                imgStar.setColorFilter(Color.parseColor("#FFD700"))
            } else {
                imgStar.setImageResource(R.drawable.ic_star_outline)
                imgStar.setColorFilter(Color.WHITE)
            }
            tooltipLayout.visibility = View.GONE
        }

        // 마이크 클릭
        btnMic.setOnClickListener {
            if (!isRecording && !isTypingFinished) {
                isRecording = true
                micBackground.alpha = 0.8f

                layoutDots.visibility = View.GONE
                scrollAnswer.visibility = View.VISIBLE
                txtAnswer.text = ""
                btnRefresh.visibility = View.INVISIBLE

                var currentIndex = 0
                typeRunnable = object : Runnable {
                    override fun run() {
                        if (currentIndex < demoText.length) {
                            txtAnswer.append(demoText[currentIndex].toString())
                            currentIndex++

                            scrollAnswer.post {
                                scrollAnswer.scrollTo(0, txtAnswer.bottom)
                            }
                            handler.postDelayed(this, 30)
                        } else {
                            micBackground.alpha = 1.0f
                            btnMic.setImageResource(R.drawable.ic_check)
                            btnRefresh.visibility = View.VISIBLE
                            isTypingFinished = true
                        }
                    }
                }
                handler.post(typeRunnable!!)

            } else if (isTypingFinished) {
                // 제출 클릭 시
                btnMic.visibility = View.INVISIBLE
                layoutDots.visibility = View.VISIBLE //로딩
                btnRefresh.visibility = View.INVISIBLE

                // 2초 후 피드백 결과 노출
                handler.postDelayed({
                    layoutFeedbackResult.visibility = View.VISIBLE
                    layoutDots.visibility = View.GONE // 로딩 끝났으니 점 3개 숨김
                    btnMic.visibility = View.VISIBLE

                    if (isFirstQuestion) {
                        tooltipLayout.visibility = View.VISIBLE
                    } else {
                        tooltipLayout.visibility = View.GONE
                    }

                    mainScrollView.post {
                        mainScrollView.smoothScrollTo(0, layoutFeedbackResult.top - 100)
                    }

                    if (isLastQuestion) {
                        btnFinalMockInterview.visibility = View.VISIBLE
                    } else {
                        btnNextQuestion.visibility = View.VISIBLE
                    }
                }, 2000)
            }
        }

        // 재녹음 버튼
        btnRefresh.setOnClickListener {
            isRecording = false
            isTypingFinished = false
            typeRunnable?.let { handler.removeCallbacks(it) }

            txtAnswer.text = ""
            btnMic.setImageResource(R.drawable.ic_mic)
            layoutDots.visibility = View.VISIBLE
            scrollAnswer.visibility = View.GONE
            btnRefresh.visibility = View.INVISIBLE
        }

        // 다음 질문
        btnNextQuestion.setOnClickListener {
            isFirstQuestion = false
            isLastQuestion = true

            tooltipLayout.visibility = View.GONE

            isRecording = false
            isTypingFinished = false
            txtAnswer.text = ""
            layoutFeedbackResult.visibility = View.GONE
            layoutDots.visibility = View.VISIBLE
            scrollAnswer.visibility = View.GONE
            btnNextQuestion.visibility = View.GONE
            btnRefresh.visibility = View.INVISIBLE


            isSaved = false
            imgStar.setImageResource(R.drawable.ic_star_outline)
            imgStar.setColorFilter(Color.WHITE)

            btnMic.setImageResource(R.drawable.ic_mic)
            findViewById<ProgressBar>(R.id.progressQuestion).progress = 2
            findViewById<TextView>(R.id.txtProgress).text = "2/5"

            mainScrollView.smoothScrollTo(0, 0)
        }

        btnFinalMockInterview.setOnClickListener {
            // startActivity(Intent(this, FinalMockActivity::class.java))
        }
    }

    private fun startDotAnimation() {
        val dots = listOf(R.id.dot1, R.id.dot2, R.id.dot3).map { findViewById<View>(it) }
        dots.forEachIndexed { index, dot ->
            val animator = ObjectAnimator.ofFloat(dot, "alpha", 0.3f, 1f, 0.3f)
            animator.duration = 1000
            animator.repeatCount = ValueAnimator.INFINITE
            animator.startDelay = (index * 200).toLong()
            animator.start()
        }
    }

    private fun setupBottomButtons() {
        findViewById<TextView>(R.id.btnNoticev2).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
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
}