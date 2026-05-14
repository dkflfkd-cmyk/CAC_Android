package com.example.cac // 본인의 패키지명

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity // 추가
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun ComponentActivity.setupSystemBarPadding(mainViewId: Int, headerViewId: Int, bottomViewId: Int) {

    enableEdgeToEdge()


    val main = findViewById<View>(mainViewId)
    val header = findViewById<View>(headerViewId)
    val bottom = findViewById<View>(bottomViewId)

    if (main == null) return

    ViewCompat.setOnApplyWindowInsetsListener(main) { _, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        // 상단 헤더 조정
        header?.let {
            val params = it.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = systemBars.top
            it.layoutParams = params
        }

        // 하단 탭 조정
        bottom?.let {
            it.setPadding(
                it.paddingLeft,
                it.paddingTop,
                it.paddingRight,
                systemBars.bottom
            )
        }

        insets
    }
}