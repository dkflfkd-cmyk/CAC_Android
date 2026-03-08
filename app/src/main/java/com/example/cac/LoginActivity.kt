package com.example.cac

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cac.data.TokenResponse
import com.example.cac.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        val savedToken = SessionManager.getToken(this)
        if (!savedToken.isNullOrBlank()) {
            Session.accessToken = savedToken
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }


        //  이미 로그인(토큰 있음) 상태면 메인으로
        if (SessionManager.isLoggedIn(this) && !Session.accessToken.isNullOrBlank()) {
            Session.accessToken = SessionManager.getToken(this)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val edtId = findViewById<EditText>(R.id.edtId)
        val edtPw = findViewById<EditText>(R.id.edtPw)
        val btnLogin = findViewById<TextView>(R.id.btnLogin)
        val btnSignup = findViewById<TextView>(R.id.btnSignup)
        val btnFindPw = findViewById<TextView>(R.id.btnFindPw)

        btnLogin.setOnClickListener {
            val id = edtId.text.toString().trim()
            val pw = edtPw.text.toString().trim()

            if (id.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "아이디/비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 서버 로그인: /auth/token
            RetrofitClient.api.tokenByUserId(id, pw, "password")
                .enqueue(object : Callback<TokenResponse> {

                    override fun onResponse(
                        call: Call<TokenResponse>,
                        response: Response<TokenResponse>
                    ) {
                        if (!response.isSuccessful) {
                            Toast.makeText(
                                this@LoginActivity,
                                "로그인 실패: ${response.code()}",
                                Toast.LENGTH_LONG
                            ).show()
                            return
                        }

                        val token = response.body()?.access_token
                        if (token.isNullOrBlank()) {
                            Toast.makeText(this@LoginActivity, "토큰이 비었습니다.", Toast.LENGTH_LONG).show()
                            return
                        }

                        // 토큰 저장 (앱 전체에서 쓰려고)
                        Session.accessToken = token


                        SessionManager.saveSession(this@LoginActivity, token, id)

                        Toast.makeText(this@LoginActivity, "로그인 성공", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }

                    override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                        Toast.makeText(
                            this@LoginActivity,
                            "통신 실패: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }

        btnSignup.setOnClickListener {
            startActivity(Intent(this, SignupStep1Activity::class.java))
        }

        btnFindPw.setOnClickListener {
            startActivity(Intent(this, FindPwActivity::class.java))
        }
    }
}