package com.suming.player

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
        //按钮：前往酷安主页
        val buttonGoCoolApk = findViewById<TextView>(R.id.buttonGoCoolApk)
        buttonGoCoolApk.setOnClickListener {
            val coolapkUri = "coolmarket://u/3105725".toUri()
            val intent = Intent(Intent.ACTION_VIEW, coolapkUri)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val webUri = "https://www.coolapk.com/u/3105725".toUri()
                startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        }
        //SvgRepo
        val buttonGoSvgRepo = findViewById<FrameLayout>(R.id.buttonGoSvgRepo)
        buttonGoSvgRepo.setOnClickListener {
            val url = "https://www.svgrepo.com/"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        //兼容性警告
        val textViewOldDevice = findViewById<TextView>(R.id.textViewOldDevice)
        if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.Q){
            textViewOldDevice.text = "检测到您的系统是安卓10，解码部分视频可能会出现兼容性问题。该问题出现在高版本系统所拍摄的视频中，来自网络的视频和本机拍摄的视频应当不受影响。"
            textViewOldDevice.setTextColor(ContextCompat.getColor(this, R.color.WarningText))
        }else{
            textViewOldDevice.visibility = GONE
        }








    }
}