package com.suming.player

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.util.UnstableApi
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.ViewWidget.CircleButton

@Suppress("NewApi")
class GuidanceActivity: AppCompatActivity() {

    @OptIn(UnstableApi::class)
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
            ToolVibrate().vibrate(this)

            AlertDialog.Builder(this@GuidanceActivity)
                .setTitle("确定跳转吗?")
                .setMessage("将唤醒浏览器并打开svgrepo.com")
                .setPositiveButton("确认") { dialog, _ ->
                    ToolVibrate().vibrate(this)

                    val url = "https://www.svgrepo.com/"
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    startActivity(intent)

                    dialog.dismiss()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    ToolVibrate().vibrate(this)

                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()

        }

        //
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

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S){
            ifWarning = true
            warningText += "安卓12及以下版本在设置新媒体项时有概率出现播放器核心崩溃的问题，安卓10及以下概率较高。\n\n"
        }

        when (Build.BRAND.lowercase()) {
            "honor",  "vivo", "iqoo" -> {
                ifWarning = true
                warningText += "未测试过App在您的设备上的兼容性。"
            }
            "xiaomi", "redmi" -> {
                ifWarning = true
                warningText += "米系机型弹出面板时会卡一下，且背景颜色压暗过程无法线性显示，可影响播放区域随动功能的流畅度，若有问题可选择关闭。"
            }
            "oppo", "realme", "oneplus" -> {
                ifWarning = true
                warningText += "欧加机型似乎无法正常使用线性马达的线性震感(变成转子马达震感)，预留了一个oppo专用选项，可以试试，不保证一定有效。"
            }
            //原生/类原生/偏原生系统
            "samsung", "google", "sony", "nokia" -> {
                ifWarning = true
                warningText += "您的系统不具备强行停止进程的机制，如果遇到未知问题，请将本App强行停止后重试。\n\n"
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
                    ToolVibrate().vibrate(this)

                    reportByCoolApk()

                    true
                }

                R.id.platform_bilibili -> {
                    ToolVibrate().vibrate(this)

                    reportByBilibili()

                    true
                }

                R.id.platform_github_issue -> {
                    ToolVibrate().vibrate(this)

                    reportByGithubIssue()

                    true
                }

                else -> true
            }
        }
        popup.show()
    }
    private fun reportByBilibili(){
        AlertDialog.Builder(this@GuidanceActivity)
            .setTitle("确定跳转吗?")
            .setMessage("将唤醒哔哩哔哩App或浏览器")
            .setPositiveButton("确认") { dialog, _ ->
                ToolVibrate().vibrate(this)

                val url = "https://space.bilibili.com/1206378184"
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                showCustomToast("正在跳转",3)
                startActivity(intent)

                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                ToolVibrate().vibrate(this)

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()

    }
    private fun reportByCoolApk(){
        AlertDialog.Builder(this@GuidanceActivity)
            .setTitle("确定跳转吗?")
            .setMessage("将唤醒酷安App或浏览器")
            .setPositiveButton("确认") { dialog, which ->
                ToolVibrate().vibrate(this)

                val webUrl = "https://www.coolapk.com/u/3105725"
                val intent = Intent(Intent.ACTION_VIEW, webUrl.toUri())

                showCustomToast("正在跳转,请稍等",3)

                startActivity(intent)

                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                ToolVibrate().vibrate(this)

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()


    }
    private fun reportByGithubIssue(){
        AlertDialog.Builder(this@GuidanceActivity)
            .setTitle("确定跳转吗?")
            .setMessage("将唤醒浏览器或Github客户端")
            .setPositiveButton("确认") { dialog, _ ->
                ToolVibrate().vibrate(this)

                val url = "https://github.com/JeanValjean07/Player/issues"
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                showCustomToast("正在跳转",3)
                startActivity(intent)

                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                ToolVibrate().vibrate(this)

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()

    }

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "GuidanceActivity: $msg")
        }
    }

}