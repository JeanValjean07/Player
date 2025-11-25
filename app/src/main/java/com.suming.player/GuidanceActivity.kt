package com.suming.player

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class GuidanceActivity: AppCompatActivity() {

    @SuppressLint("QueryPermissionsNeeded", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContentView(R.layout.activity_guidance)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_guidance)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        //按钮：返回
        val buttonBack = findViewById<ImageButton>(R.id.buttonExit)
        buttonBack.setOnClickListener {
            finish()
        }
        //按钮：前往Issue
        val buttonGoCoolApk = findViewById<TextView>(R.id.buttonReportIssue)
        buttonGoCoolApk.setOnClickListener {
            val url = "https://space.bilibili.com/1206378184"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }
        //SvgRepo
        val buttonGoSvgRepo = findViewById<FrameLayout>(R.id.buttonGoSvgRepo)
        buttonGoSvgRepo.setOnClickListener {
            val url = "https://www.svgrepo.com/"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        //兼容性警告：Android10解码问题
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q){
            val AlertCard_Android10Decoding = findViewById<FrameLayout>(R.id.AlertCard_Android10Decoding)
            AlertCard_Android10Decoding.visibility = View.VISIBLE
            val AlertCardText_Android10Decoding = findViewById<TextView>(R.id.AlertCardText_Android10Decoding)
            AlertCardText_Android10Decoding.text = "检测到您的系统是安卓10，解码部分视频可能会出现兼容性问题。该问题出现在高版本系统所拍摄的视频中，来自网络的视频和本机拍摄的视频应当不受影响。"
            AlertCardText_Android10Decoding.setTextColor(ContextCompat.getColor(this, R.color.WarningText))
        }

        //兼容性警告：无法清理进程
        val ifCannotCleanProcess = when (Build.BRAND.lowercase()) {
            "huawei", "honor", "xiaomi", "redmi", "oppo", "realme", "oneplus", "vivo", "iqoo" -> true
            else -> false
        }
        if (!ifCannotCleanProcess){
            val AlertCard_CannotCleanProcess = findViewById<CardView>(R.id.AlertCard_CannotCleanProcess)
            AlertCard_CannotCleanProcess.visibility = View.VISIBLE
            val AlertCardText_CannotCleanProcess = findViewById<TextView>(R.id.AlertCardText_CannotCleanProcess)
            AlertCardText_CannotCleanProcess.text = "您的系统不能执行系统级进程清理，可能导致划掉后台时视频不能停止，并且持续占用内存，需要您手动强行停止本APP"
            AlertCardText_CannotCleanProcess.setTextColor(ContextCompat.getColor(this, R.color.WarningText))
        }



    }

}