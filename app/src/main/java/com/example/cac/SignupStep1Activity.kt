package com.example.cac

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignupStep1Activity : AppCompatActivity() {

    private var idChecked = false
    private var emailChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_step1)

        val edtId = findViewById<EditText>(R.id.edtId2)
        val edtPw = findViewById<EditText>(R.id.edtPw2)
        val edtPwCheck = findViewById<EditText>(R.id.edtPwCheck2)
        val edtEmail = findViewById<EditText>(R.id.edtEmail2)

        val btnCheckId = findViewById<TextView>(R.id.btnCheckId)
        val btnCheckEmail = findViewById<TextView>(R.id.btnCheckEmail)
        val btnSubmit = findViewById<TextView>(R.id.btnSubmit)

        val txtIdError = findViewById<TextView>(R.id.txtIdError)
        val txtPwError = findViewById<TextView>(R.id.txtPwError)
        val txtEmailError = findViewById<TextView>(R.id.txtEmailError)

        //입력 바뀌면 중복확인 무효
        edtId.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                idChecked = false
                txtIdError.text = ""
            }
        })

        edtEmail.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                emailChecked = false
                txtEmailError.text = ""
            }
        })

        //아이디 중복 확인
        btnCheckId.setOnClickListener {
            val id = edtId.text.toString().trim()

            if (id.isEmpty()) {
                txtIdError.text = "아이디를 입력해 주세요."
                idChecked = false
                return@setOnClickListener
            }

            // 임시 중복 로직
            if (id == "test" || id == "admin") {
                txtIdError.text = "이미 사용 중인 아이디입니다."
                idChecked = false
            } else {
                txtIdError.text = ""
                idChecked = true
                Toast.makeText(this, "사용 가능한 아이디입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        //이메일 중복 확인
        btnCheckEmail.setOnClickListener {
            val email = edtEmail.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                txtEmailError.text = "올바른 이메일이 아닙니다."
                emailChecked = false
            } else {
                txtEmailError.text = ""
                emailChecked = true
                Toast.makeText(this, "사용 가능한 이메일입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        //다음 단계
        btnSubmit.setOnClickListener {
            val id = edtId.text.toString().trim()
            val pw = edtPw.text.toString()
            val pwCheck = edtPwCheck.text.toString()
            val email = edtEmail.text.toString().trim()

            if (id.isEmpty()) {
                txtIdError.text = "아이디를 입력해 주세요."
                return@setOnClickListener
            }

            if (!idChecked) {
                txtIdError.text = "아이디 중복확인을 해주세요."
                return@setOnClickListener
            }

            if (pw.length < 8) {
                txtPwError.text = "비밀번호는 8자 이상이어야 합니다."
                return@setOnClickListener
            }

            if (pw != pwCheck) {
                txtPwError.text = "비밀번호가 일치하지 않습니다."
                return@setOnClickListener
            } else {
                txtPwError.text = ""
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                txtEmailError.text = "올바른 이메일이 아닙니다."
                return@setOnClickListener
            }

            if (!emailChecked) {
                txtEmailError.text = "이메일 중복확인을 해주세요."
                return@setOnClickListener
            }

            val intent = Intent(this, SignupStep2Activity::class.java)
            intent.putExtra("id", id)
            intent.putExtra("pw", pw)
            intent.putExtra("email", email)
            startActivity(intent)
        }
    }
}
