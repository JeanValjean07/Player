package com.suming.player

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.ViewWidget.CircleButton

class GuidanceActivity: AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.Q)
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
        val ButtonExit = findViewById<CircleButton>(R.id.AppBarButton_Exit)
        ButtonExit.setOnClickListener {
            finish()
        }
        //按钮：反馈
        val ButtonReport = findViewById<TextView>(R.id.TextButton_Report)
        ButtonReport.setOnClickListener {
            ToolVibrate().vibrate(this)

            showReportMenu(ButtonReport)


        }
        //SvgRepo
        val buttonGoSvgRepo = findViewById<FrameLayout>(R.id.buttonGoSvgRepo)
        buttonGoSvgRepo.setOnClickListener {
            val url = "https://www.svgrepo.com/"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        showWarningMessageCard()

    }


    //合成警示信息
    private fun showWarningMessageCard(){
        val AlertCard = findViewById<CardView>(R.id.AlertCard)
        val AlertCardText = findViewById<TextView>(R.id.AlertCardText)
        //
        val (ifWarning, warningText) = loadWarnings()

        if (ifWarning){

            AlertCard.visibility = View.VISIBLE

            AlertCardText.text = warningText
        }else{
            AlertCard.visibility = View.GONE
        }
    }
    private fun loadWarnings():Pair<Boolean, String>{
        var ifWarning = false
        var warningText = "环境提示\n\n"

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q){
            ifWarning = true
            warningText += "您的系统是安卓10及以下版本，解码部分视频可能会出现兼容性问题。该问题出现在高版本系统设备所拍摄的视频中，来自网络的视频和本机拍摄的视频大概率不受影响。\n\n"
        }

        when (Build.BRAND.lowercase()) {
            "honor", "xiaomi", "redmi", "oppo", "realme", "oneplus", "vivo", "iqoo" -> {
                ifWarning = true
                warningText += "未测试过App在您的设备上的兼容性。"
            }
            //原生/类原生/偏原生系统
            "samsung", "google", "sony", "nokia" -> {
                ifWarning = true
                warningText += "您的系统不具备强行停止进程的机制，如果遇到莫名其妙的问题，请将本App强行停止后重试。\n\n"
            }
        }

        return Pair(ifWarning, warningText)

    }




    //弹出反馈菜单
    private fun showReportMenu(button:TextView){
        val popup = PopupMenu(this@GuidanceActivity, button)
        popup.menuInflater.inflate(R.menu.popup_menu_report_platform, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.platform_coolapk -> {
                    reportByCoolApk(); true
                }

                R.id.platform_bilibili -> {
                    reportByBilibili(); true
                }

                R.id.platform_github_issue -> {
                    reportByGithubIssue(); true
                }

                else -> true
            }
        }
        popup.show()
    }
    private fun reportByBilibili(){
        val url = "https://space.bilibili.com/1206378184"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        showCustomToast("正在跳转",3)
        startActivity(intent)
    }
    private fun reportByCoolApk(){
        val webUrl = "https://www.coolapk.com/u/3105725"
        val intent = Intent(Intent.ACTION_VIEW, webUrl.toUri())

        showCustomToast("正在跳转,请稍等",3)

        startActivity(intent)
    }
    private fun reportByGithubIssue(){
        val url = "https://github.com/JeanValjean07/Player/issues"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        showCustomToast("正在跳转",3)
        startActivity(intent)
    }


}