package com.suming.player

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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


        val deviceInfo = intent.getStringExtra("deviceInfo")
        if (deviceInfo == "old"){
            val textViewOldDevice = findViewById<TextView>(R.id.textViewOldDevice)
            textViewOldDevice.text = "检测到您的系统是安卓10，打开由安卓12及以上版本设备拍摄的视频时，" +
                    "可能出现播放暂停和无响应等兼容性问题，打开其他视频不受影响。兼容性故障导致的视频卡死基本都是假死，不影响APP自身界面" +
                    "等待足够长时间，可以自己恢复。如果在活动创建时解码器立即卡死，才会导致APP界面卡死。可点击右上角\"系统播放器打开\"按钮，这大概率会让解码器脱离卡死状态。"
            textViewOldDevice.setTextColor(ContextCompat.getColor(this, R.color.WarningText))
        }
    }
}