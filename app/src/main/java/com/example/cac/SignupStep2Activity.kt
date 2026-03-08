package com.example.cac

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import android.widget.Toast
import com.example.cac.network.RetrofitClient
import com.example.cac.data.SignupRequest

class SignupStep2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_step2)

        val edtName = findViewById<EditText>(R.id.edtName)
        val edtMajor = findViewById<EditText>(R.id.edtMajor)
        val ddEdu = findViewById<MaterialAutoCompleteTextView>(R.id.ddEdu)
        val ddCareer = findViewById<MaterialAutoCompleteTextView>(R.id.ddCareer)
        val edtWish = findViewById<EditText>(R.id.edtWishJob)
        val btnNext = findViewById<TextView>(R.id.btnNext1)

        ddEdu.setSimpleItems(arrayOf("재학 중", "휴학 중", "졸업 예정", "졸업", "해당 없음"))
        ddCareer.setSimpleItems(arrayOf("신입", "인턴/계약직 경험", "1~3년", "3년 이상"))

        btnNext.setOnClickListener {
            val id = intent.getStringExtra("id") ?: ""
            val pw = intent.getStringExtra("pw") ?: ""
            val email = intent.getStringExtra("email") ?: ""

            val name = edtName.text.toString().trim()
            val major = edtMajor.text.toString().trim()
            val edu = ddEdu.text.toString().trim()
            val career = ddCareer.text.toString().trim()
            val wish = edtWish.text.toString().trim()




            // (선택) 간단 유효성 체크
            if (id.isBlank() || pw.isBlank() || email.isBlank()) {
                Toast.makeText(this, "Step1 정보가 비었음", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (name.isBlank()) {
                Toast.makeText(this, "이름을 입력해 주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 서버에 보낼 회원가입 body
            val req = SignupRequest(
                user_id = id,
                email = email,
                password = pw,
                username = name,
                major = major,
                grade = edu,
                target_job = wish,
                experience_level = career
            )

            RetrofitClient.api.signup(req).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                override fun onResponse(
                    call: retrofit2.Call<Map<String, Any>>,
                    response: retrofit2.Response<Map<String, Any>>
                ) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@SignupStep2Activity, "회원가입 실패: ${response.code()} ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                        return
                    }

                    Toast.makeText(this@SignupStep2Activity, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignupStep2Activity, LoginActivity::class.java))
                    finish()
                }

                override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                    Toast.makeText(this@SignupStep2Activity, "통신 실패: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }

    }
}
