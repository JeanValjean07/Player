package com.suming.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Space
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
import androidx.core.widget.NestedScrollView
import androidx.media3.common.util.UnstableApi
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.FuncionalPack.Animations
import com.suming.player.FuncionalPack.DeviceInfo
import com.suming.player.ViewWidget.CircleButton

@Suppress("NewApi")
class GuidanceActivity: AppCompatActivity() {

    //context
    private val context: Context = this@GuidanceActivity

    @OptIn(UnstableApi::class)
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("QueryPermissionsNeeded", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //
        init_display()



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
        //点击顶部区域
        AppBarCore.setOnClickListener {
            if (scrollArea?.canScrollVertically(-1) == true){
                ToolVibrate().vibrate(context)
                //滚动区域回顶
                scrollArea?.stopNestedScroll()
                scrollArea?.smoothScrollTo(0, 0)
            }else{
                scrollArea?.stopNestedScroll()
            }
        }



        //
        showWarningMessageCard()


        setupScrollContentListener()

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
            warningText += "安卓12及以下版本在设置新媒体项时有概率出现MediaCodeC错误，但不是致命错误，稍等几秒即可主动跳出。\n\n"
        }

        when (Build.BRAND.lowercase()) {
            "honor",  "vivo", "iqoo" -> {
                ifWarning = true
                warningText += "未测试过App在您的设备上的兼容性。"
            }
            "xiaomi", "redmi" -> {
                ifWarning = true
                //warningText += ""
            }
            "oppo", "realme", "oneplus" -> {
                ifWarning = true
                //warningText += ""
            }
            //原生/类原生/偏原生系统
            "samsung", "google", "sony", "nokia" -> {
                ifWarning = true
                warningText += "您的系统不具备强行停止进程的机制，如果遇到未知问题，请将本App强行停止后重试。\n\n"
            }
        }

        return Pair(ifWarning, warningText)

    }


    //显示初始化
    //<editor-fold desc="////显示初始化">
    private fun init_display(){
        enableEdgeToEdge()
        setContentView(R.layout.activity_guidance)
        //初始化视图
        //顶栏区域
        AppBarGradientMask = findViewById(R.id.AppBarGradientMask)
        AppBarCore = findViewById(R.id.AppBarCore)
        AppBarSpacer = findViewById(R.id.AppBarSpacer)
        AppBarTitle = findViewById(R.id.AppBarTitle)
        //滚动区域
        scrollArea = findViewById(R.id.NestedScrollView)
        //获取状态栏高度
        if (DeviceInfo.statusBarHeight == 0){
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->

                DeviceInfo.statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                onStatusBarHeightGet(DeviceInfo.statusBarHeight)

                insets
            }
        }else{
            onStatusBarHeightGet(DeviceInfo.statusBarHeight)
        }


    }
    //顶栏规整
    private fun onStatusBarHeightGet(statusBarHeight: Int){
        AppBarGradientMask.layoutParams.height = statusBarHeight + dpToPx(60f).toInt()
        (AppBarCore.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusBarHeight
        AppBarSpacer.layoutParams.height = statusBarHeight + dpToPx(60f).toInt()
    }

    //顶栏效果
    private lateinit var AppBarGradientMask: View  //brush
    private lateinit var AppBarCore: LinearLayout
    private lateinit var AppBarSpacer: Space
    private lateinit var AppBarTitle: TextView
    private fun topBarEffect_Out_Title(){
        if (!isTopBarTitleVisible) return
        isTopBarTitleVisible = false

        AppBarTitle.animate()
            .alpha(0f)
            .setDuration(Animations.topBar_Effect_Brush_Duration)
            .withEndAction { AppBarTitle.visibility = View.GONE }
            .start()

    }
    private fun topBarEffect_In_Title(){
        if (isTopBarTitleVisible) return
        isTopBarTitleVisible = true

        AppBarTitle.visibility = View.VISIBLE
        AppBarTitle.alpha = 0f
        AppBarTitle.animate()
            .alpha(1f)
            .setDuration(Animations.topBar_Effect_Brush_Duration)
            .start()
    }
    private var isTopBarTitleVisible = true
    private fun topBarEffect_Out_Brush(){
        if (!isTopBarBrushVisible) return
        isTopBarBrushVisible = false

        AppBarGradientMask.alpha = 1f
        AppBarGradientMask.animate()
            .alpha(0f)
            .setDuration(Animations.topBar_Effect_Brush_Duration)
            .withEndAction { AppBarGradientMask.visibility = View.GONE }
            .start()
    }
    private fun topBarEffect_In_Brush(){
        if (isTopBarBrushVisible) return
        isTopBarBrushVisible = true

        AppBarGradientMask.visibility = View.VISIBLE
        AppBarGradientMask.alpha = 0f
        AppBarGradientMask.animate()
            .alpha(1f)
            .setDuration(Animations.topBar_Effect_Brush_Duration)
            .start()
    }
    private var isTopBarBrushVisible = true
    //滚动区域监听
    private var scrollArea: NestedScrollView? = null
    private fun setupScrollContentListener(){
        if (scrollArea == null){
            scrollArea = findViewById(R.id.ScrollArea)
        }
        scrollArea?.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            //未在顶部时隐藏顶部栏文字区
            if (scrollY == 0){
                topBarEffect_In_Title()
                topBarEffect_Out_Brush()
            }
            else{
                topBarEffect_Out_Title()
                topBarEffect_In_Brush()
            }
        }
    }
    //</editor-fold>


    @Suppress("unused")
    private fun dpToPx(dp: Float): Float {
        val metrics = resources.displayMetrics
        return dp * metrics.density
    }
    @Suppress("unused")
    private fun pxToDp(px: Float): Float {
        val metrics = resources.displayMetrics
        return px / metrics.density
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