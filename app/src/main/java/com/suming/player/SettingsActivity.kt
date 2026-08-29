package com.suming.player

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioRepo
import com.suming.player.DataPack.DataBaseMediaStore.Video.VideoRepo
import com.suming.player.DataPack.ReleaseInfo
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.DeviceInfo
import com.suming.player.FuncionalPack.MediaRecordManager
import com.suming.player.FuncionalPack.PrivacyPermissionHelper
import com.suming.player.ViewWidget.CircleButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.system.exitProcess

@Suppress("/unused","NewApi")
@SuppressLint("InflateParams", "SetTextI18n")
@OptIn(UnstableApi::class)
class SettingsActivity: AppCompatActivity(){


    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded", "UseKtx")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //初始化
        init()


        register()

        registerSettings()

        //开启显示监听
        setupScrollContentListener()


    }

    private fun init(){
        display()


    }

    private fun register(){
        lifecycleScope.launch(Dispatchers.Main) {
            //显示版本
            lifecycleScope.launch(Dispatchers.IO) {
                delay(500)
                if (AppVersion == null){
                    AppVersion = packageManager.getPackageInfo(packageName, 0).versionName
                }
                withContext(Dispatchers.Main) {
                    val versionText = findViewById<TextView>(R.id.version)
                    versionText.text = "版本: $AppVersion"
                }
                //consoleLog("当前版本: $AppVersion")
            }

            //按钮：返回
            val ButtonBack = findViewById<CircleButton>(R.id.buttonExit)
            ButtonBack.setOnClickListener {
                finish()
            }
            //按钮：前往项目Github仓库页
            val ButtonGoGithubRepo = findViewById<TextView>(R.id.Button_GoTo_GithubRepo)
            ButtonGoGithubRepo.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)

                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("将跳转至浏览器")
                    .setMessage("是否继续?")
                    .setPositiveButton("确认") { dialog, _ ->
                        ToolVibrate().vibrate(this@SettingsActivity)

                        val url = "https://github.com/JeanValjean07/Player/"
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        startActivity(intent)

                        dialog.dismiss()
                    }
                    .setNegativeButton("取消") { dialog, _ ->
                        ToolVibrate().vibrate(this@SettingsActivity)

                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()


            }
            //按钮：前往项目Github发布页
            val ButtonGoGithubRelease = findViewById<TextView>(R.id.Button_GoTo_GithubRelease)
            ButtonGoGithubRelease.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)

                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("将跳转至浏览器")
                    .setMessage("是否继续?")
                    .setPositiveButton("确认") { dialog, _ ->
                        ToolVibrate().vibrate(this@SettingsActivity)

                        val url = "https://github.com/JeanValjean07/Player/releases"
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        startActivity(intent)

                        dialog.dismiss()
                    }
                    .setNegativeButton("取消") { dialog, _ ->
                        ToolVibrate().vibrate(this@SettingsActivity)

                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()



            }
            //按钮：自动检查更新
            val ButtonCheckUpdate = findViewById<TextView>(R.id.ButtonAutoUpdate)
            ButtonCheckUpdate.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)
                //检查更新
                checkNewVersion()
            }




            //撤回隐私政策同意
            val RevokePrivacyAgreement = findViewById<TextView>(R.id.RevokePrivacyAgreement)
            RevokePrivacyAgreement.paint.isUnderlineText = true
            RevokePrivacyAgreement.invalidate()
            RevokePrivacyAgreement.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)

                //撤回隐私政策同意
                revokePrivacyAgreementAlert()


            }


            //不再使用
            /*
            //超链接：开放源代码许可
            val openSourceLicense = findViewById<TextView>(R.id.openSourceLicense)
            openSourceLicense.paint.isUnderlineText = true
            openSourceLicense.invalidate()
            openSourceLicense.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)


            }
            //超链接：设备信息
            val DeviceInfoPage = findViewById<TextView>(R.id.DeviceInfoPage)
            DeviceInfoPage.paint.isUnderlineText = true
            DeviceInfoPage.invalidate()
            DeviceInfoPage.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)
                startActivity(Intent(this@SettingsActivity, DeviceInfoActivity::class.java))
            }

             */



        }
    }

    private fun registerSettings(){
        lifecycleScope.launch(Dispatchers.Main) {
            //媒体会话不使用封面图片
            val switch_DisableMediaArtWork = findViewById<SwitchCompat>(R.id.DisableMediaArtWork)
            switch_DisableMediaArtWork.isChecked = SettingsRequestCenter.get_PREFS_DisableMediaArtWork(this@SettingsActivity)
            switch_DisableMediaArtWork.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.set_PREFS_DisableMediaArtWork(isChecked)
            }
            //始终使用深色播放页面
            val switch_AlwaysUseDarkTheme = findViewById<SwitchCompat>(R.id.AlwaysUseDarkTheme)
            switch_AlwaysUseDarkTheme.isChecked = SettingsRequestCenter.get_PREFS_AlwaysUseDarkTheme(this@SettingsActivity)
            switch_AlwaysUseDarkTheme.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.set_PREFS_AlwaysUseDarkTheme(isChecked)
            }
            //启动时继续上次的媒体
            val switch_EnableContinuePlay = findViewById<SwitchCompat>(R.id.EnableContinuePlay)
            switch_EnableContinuePlay.isChecked = SettingsRequestCenter.get_PREFS_EnableContinuePlay(this@SettingsActivity)
            switch_EnableContinuePlay.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.set_PREFS_EnableContinuePlay(this@SettingsActivity, isChecked)
            }
            //启动续播时启动播放器引擎
            val SC_ContinuePlay_withEngin = findViewById<SwitchCompat>(R.id.SC_ContinuePlay_withEngin)
            SC_ContinuePlay_withEngin.isChecked = SettingsRequestCenter.GET_PRF_ContinuePlay_withEngin(this@SettingsActivity)
            SC_ContinuePlay_withEngin.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.SET_PRF_ContinuePlay_withEngin(this@SettingsActivity, isChecked)
            }
            //后台划卡时关闭播放器
            val switch_StopPlayerWhenTaskRemoved = findViewById<SwitchCompat>(R.id.StopPlayerWhenTaskRemoved)
            switch_StopPlayerWhenTaskRemoved.isChecked = SettingsRequestCenter.get_PREFS_StopPlayerWhenTaskRemoved(this@SettingsActivity)
            switch_StopPlayerWhenTaskRemoved.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.set_PREFS_StopPlayerWhenTaskRemoved(isChecked)
            }
            //使用高刷新率
            val switch_EnableHighRefreshRate = findViewById<SwitchCompat>(R.id.EnableHighRefreshRate)
            switch_EnableHighRefreshRate.isChecked = SettingsRequestCenter.get_PREFS_LockRefreshRate(this@SettingsActivity)
            switch_EnableHighRefreshRate.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.set_PREFS_LockRefreshRate(isChecked)
            }
            //启用首页MiniView
            val SC_EnableMiniView = findViewById<SwitchCompat>(R.id.SC_EnableMiniView)
            SC_EnableMiniView.isChecked = SettingsRequestCenter.GET_PRF_EnableMiniView(this@SettingsActivity)
            SC_EnableMiniView.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.SET_PRF_EnableMiniView(this@SettingsActivity, isChecked)
            }
            //禁用更多操作面板下滑手势
            val switch_DisableFragmentGesture = findViewById<SwitchCompat>(R.id.DisableFragmentGesture)
            switch_DisableFragmentGesture.isChecked = SettingsRequestCenter.get_PREFS_DisableFragmentGesture(this@SettingsActivity)
            switch_DisableFragmentGesture.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.set_PREFS_DisableFragmentGesture(isChecked)
            }
            //退出播放页时确保竖屏
            val SC_SwitchPortrait_whenExit = findViewById<SwitchCompat>(R.id.EnsurePortraitWhenExit)
            SC_SwitchPortrait_whenExit.isChecked = SettingsRequestCenter.GET_PRF_SwitchPortrait_whenExit(this@SettingsActivity)
            SC_SwitchPortrait_whenExit.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)

                SettingsRequestCenter.SET_PRF_SwitchPortrait_whenExit(isChecked)
            }
            //竖屏时也开启自动隐藏控件
            val SC_EnableAutoHideController_whenPortrait = findViewById<SwitchCompat>(R.id.SC_EnableAutoHideController_whenPortrait)
            SC_EnableAutoHideController_whenPortrait.isChecked = SettingsRequestCenter.GET_PRF_EnableAutoHideController_whenPortrait(this@SettingsActivity)
            SC_EnableAutoHideController_whenPortrait.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.SET_PRF_EnableAutoHideController_whenPortrait(this@SettingsActivity, isChecked)
            }


            //启用播放区域移动动画
            val switch_EnablePlayAreaMoveAnim = findViewById<SwitchCompat>(R.id.EnablePlayAreaMoveAnim)
            switch_EnablePlayAreaMoveAnim.isChecked = SettingsRequestCenter.get_PREFS_EnablePlayAreaMoveAnim(this@SettingsActivity)
            switch_EnablePlayAreaMoveAnim.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.set_PREFS_EnablePlayAreaMoveAnim(isChecked)
            }
            //进度条截取缩略图时使用关键帧
            val switch_UseSyncFrameInScroller = findViewById<SwitchCompat>(R.id.UseSyncFrameInScroller)
            switch_UseSyncFrameInScroller.isChecked = SettingsRequestCenter.get_PREFS_UseSyncFrameInScroller(this@SettingsActivity)
            switch_UseSyncFrameInScroller.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.set_PREFS_UseSyncFrameInScroller(isChecked)
            }
            //寻帧时一律使用关键帧
            val switch_UseOnlySyncFrameWhenSeek = findViewById<SwitchCompat>(R.id.UseOnlySyncFrameWhenSeek)
            switch_UseOnlySyncFrameWhenSeek.isChecked = SettingsRequestCenter.get_PREFS_UseOnlySyncFrameWhenSeek(this@SettingsActivity)
            switch_UseOnlySyncFrameWhenSeek.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.set_PREFS_UseOnlySyncFrameWhenSeek(isChecked)
            }
            //禁用主页面小播放器
            val switch_DisableMainPageSmallPlayer = findViewById<SwitchCompat>(R.id.DisableMainPageSmallPlayer)
            switch_DisableMainPageSmallPlayer.isChecked = SettingsRequestCenter.GET_PRF_AlwaysUseImageInMiniView(this@SettingsActivity)
            switch_DisableMainPageSmallPlayer.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.SET_PRF_AlwaysUseImageInMiniView(this@SettingsActivity, isChecked)
            }
            //使用超长进度条
            val switch_UseSuperLongScroller = findViewById<SwitchCompat>(R.id.UseSuperLongScroller)
            switch_UseSuperLongScroller.isChecked = SettingsRequestCenter.get_PREFS_UseSuperLongScroller(this@SettingsActivity)
            switch_UseSuperLongScroller.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.set_PREFS_UseSuperLongScroller(isChecked)
            }
            //进度条绘制使用兼容模式
            val switch_UseCompatScroller = findViewById<SwitchCompat>(R.id.UseCompatScroller)
            switch_UseCompatScroller.isChecked = SettingsRequestCenter.get_PREFS_UseCompatScroller(this@SettingsActivity)
            switch_UseCompatScroller.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.set_PREFS_UseCompatScroller(isChecked)
            }
            //后台播放时关闭视频轨道
            val switch_DisableVideoTrackOnBack = findViewById<SwitchCompat>(R.id.DisableVideoTrackOnBack)
            switch_DisableVideoTrackOnBack.isChecked = SettingsRequestCenter.get_PREFS_DisableVideoTrackOnBack(this@SettingsActivity)
            switch_DisableVideoTrackOnBack.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(this@SettingsActivity)
                SettingsRequestCenter.set_PREFS_DisableVideoTrackOnBack(isChecked)
            }

            //播放页样式
            val ButtonPlayerType = findViewById<CardView>(R.id.ButtonPlayerType)
            updatePlayPageTypeText()
            ButtonPlayerType.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)
                //使用弹出菜单选择
                val popup = PopupMenu(this@SettingsActivity, ButtonPlayerType)
                popup.menuInflater.inflate(R.menu.activity_settings_popup_player_type, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.type_oro -> {
                            choosePlayPageType(SettingsRequestCenter.PlayPageType_Oro); true
                        }
                        R.id.type_neo -> {
                            choosePlayPageType(SettingsRequestCenter.PlayPageType_Neo); true
                        }
                        R.id.type_test -> {
                            choosePlayPageType(SettingsRequestCenter.PlayPageType_Test); true
                        }
                        else -> true
                    }
                }
                popup.show()
            }

            //寻帧间隔
            val ButtonCardSeekHandlerGap = findViewById<CardView>(R.id.ButtonCardSeekHandlerGap)
            updateSeekHandlerGapText()
            ButtonCardSeekHandlerGap.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)
                //使用弹出菜单选择
                val popup = PopupMenu(this@SettingsActivity, ButtonCardSeekHandlerGap)
                popup.menuInflater.inflate(
                    R.menu.popup_menu_gap_seek_loop,
                    popup.menu
                )
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_item_NoGap -> {
                            chooseSeekHandlerGap(0); true
                        }

                        R.id.menu_item_60hz -> {
                            chooseSeekHandlerGap(16); true
                        }

                        R.id.menu_item_30hz -> {
                            chooseSeekHandlerGap(33); true
                        }

                        R.id.menu_item_15hz -> {
                            chooseSeekHandlerGap(66); true
                        }

                        R.id.menu_item_Input -> {
                            setSeekHandlerGapByInput(); true
                        }

                        else -> true
                    }
                }
                popup.show()
            }
            //时间戳刷新间隔
            val ButtonCardTimerUpdateGap = findViewById<CardView>(R.id.ButtonCardTimerUpdateGap)
            updateTimerUpdateGapText()
            ButtonCardTimerUpdateGap.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)
                //使用弹出菜单选择
                val popup = PopupMenu(this@SettingsActivity, ButtonCardTimerUpdateGap)
                popup.menuInflater.inflate(
                    R.menu.activity_settings_popup_timer_update_gap,
                    popup.menu
                )
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_item_120hz -> {
                            chooseTimeUpdateGap(8L); true
                        }

                        R.id.menu_item_90hz -> {
                            chooseTimeUpdateGap(12L); true
                        }

                        R.id.menu_item_60hz -> {
                            chooseTimeUpdateGap(16L); true
                        }

                        R.id.menu_item_30hz -> {
                            chooseTimeUpdateGap(33L); true
                        }

                        R.id.menu_item_15hz -> {
                            chooseTimeUpdateGap(66L); true
                        }

                        R.id.menu_item_Input -> {
                            setTimerUpdateGapByInput(); true
                        }

                        else -> true
                    }
                }
                popup.show()

            }
            //进度条刷新间隔
            val ButtonCardScrollerUpdateGap = findViewById<CardView>(R.id.ButtonCard_scrollerUpdateGap)
            updateScrollerUpdateGapText()
            ButtonCardScrollerUpdateGap.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)
                chooseScrollerUpdateGap(ButtonCardScrollerUpdateGap)
            }
            //振动模式
            val ButtonCardVibrateMode = findViewById<CardView>(R.id.ButtonCardVibrateMode)
            updateVibrateModeText()
            ButtonCardVibrateMode.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)
                //使用弹出菜单选择
                val popup = PopupMenu(this@SettingsActivity, ButtonCardVibrateMode)
                popup.menuInflater.inflate(R.menu.popup_menu_vibrate_mode, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.item_NoVibrate -> {
                            chooseVibrateMode(0); true
                        }

                        R.id.item_EFFECT_CLICK -> {
                            chooseVibrateMode(1); true
                        }

                        R.id.item_EFFECT_TICK -> {
                            chooseVibrateMode(2); true
                        }

                        R.id.item_EFFECT_DOUBLE_CLICK -> {
                            chooseVibrateMode(3); true
                        }

                        R.id.item_EFFECT_HEAVY_CLICK -> {
                            chooseVibrateMode(4); true
                        }

                        R.id.item_100Ms -> {
                            chooseVibrateMode(5); true
                        }

                        else -> true
                    }
                }
                popup.show()
            }

            //SeekBar刷新间隔
            val ButtonCardSeekBarUpdateGap = findViewById<CardView>(R.id.ButtonCard_seekbarUpdateGap)
            updateSeekBarUpdateGapText()
            ButtonCardSeekBarUpdateGap.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)
                chooseSeekBarUpdateGap(ButtonCardSeekBarUpdateGap)
            }

            //封面缩略图管理
            val ButtonRemoveAllThumbPath = findViewById<TextView>(R.id.RemoveAllThumbPath)
            ButtonRemoveAllThumbPath.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)

                chooseDeleteFrameItem(ButtonRemoveAllThumbPath)

            }
            //数据库缓存管理
            val ButtonManageDB = findViewById<TextView>(R.id.TextButton_DBManage)
            ButtonManageDB.setOnClickListener {
                ToolVibrate().vibrate(this@SettingsActivity)

                chooseDeleteDB(ButtonManageDB)
            }

        }
    }




    //检查更新(依托github release api)
    private var AppVersion: String? = null
    private fun checkNewVersion(){
        //读取当前版本(用于比对)
        if (AppVersion == null){
            AppVersion = packageManager.getPackageInfo(packageName, 0).versionName
            if (AppVersion == null){
                showCustomToast("无法检查当前App版本")
                //consoleLog("无法检查当前版本")
            }
        }

        //检查最新版本
        lifecycleScope.launch(Dispatchers.IO) {
            val latestRelease = FindLatestRelease()
            if (latestRelease != null) {
                consoleLog("检查更新：当前最新版本为: ${latestRelease.version}, 下载链接: ${latestRelease.downloadUrl}")
                withContext(Dispatchers.Main) {
                    showCustomToast("检查更新成功: 最新发布版本为 ${latestRelease.version}, 下载链接: ${latestRelease.downloadUrl}")
                }
            }else{
                consoleLog("检查更新：失败")
                withContext(Dispatchers.Main) {
                    showCustomToast("检查更新失败")
                }
            }
        }

    }


    //检查更新
    suspend fun FindLatestRelease(): ReleaseInfo? {
        return withContext(Dispatchers.IO) {
            try {

                //github release api
                val url = "https://api.github.com/repos/${NetworkClient.githubRepository}/releases/latest"

                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = NetworkClient.request(request)

                //失败
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        showCustomToast("检查更新失败: ${response.code}")
                    }
                    consoleLog("检查更新：请求失败: ${response.code}")
                    return@withContext null
                }

                //解析
                val jsonString = response.body.string()
                val json = JSONObject(jsonString)
                val tagName = json.getString("tag_name")
                val version = tagName.removePrefix("v")
                //consoleLog("检查更新：当前最新版本为: $version  tagName: $tagName")
                val assets = json.getJSONArray("assets")
                if (assets.length() == 0) {
                    //withContext(Dispatchers.Main) { showCustomToast("检查更新失败: 仓库居然是空的") }
                    //consoleLog("检查更新：错误：该仓库的release什么都没有")
                    return@withContext null
                }
                val firstAsset = assets.getJSONObject(0)
                var downloadUrl = firstAsset.getString("browser_download_url")
                //找到第一个apk
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")

                    //检查后缀
                    if (name.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.getString("browser_download_url")
                        //consoleLog("检查更新：找到一个apk: $name")
                        break
                    }
                }

                //withContext(Dispatchers.Main) { showCustomToast("检查更新成功: 最新发布版本为 $version") }
                //consoleLog("检查更新：最新版本下载地址为: $downloadUrl")

                ReleaseInfo(version, downloadUrl)

            }catch(e: Exception){
                //consoleLog("从 github api 检查更新 - 网络请求出错: ${e.message}")

                withContext(Dispatchers.Main) { showCustomToast("检查更新失败: ${e.message}") }

                null
            }
        }
    }


    //取消隐私政策同意
    private fun revokePrivacyAgreementAlert(){
        AlertDialog.Builder(this@SettingsActivity)
            .setTitle("确定撤回同意吗?")
            .setMessage("若确认，App将自动退出")
            .setPositiveButton("确认") { dialog, _ ->
                ToolVibrate().vibrate(this)

                revokePrivacyAgreementCore()

                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                ToolVibrate().vibrate(this)

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()

    }
    private fun revokePrivacyAgreementCore(){
        //关闭正在播放的媒体
        PlayerSingleton.clearMediaItem()
        PlayerSingleton.stopPlayEngine()

        //清除媒体记录
        val MediaRecordManager = MediaRecordManager()
        MediaRecordManager.clear_MediaInfo(this)

        //写入隐私政策同意状态
        val PrivacyPermissionHelper = PrivacyPermissionHelper()
        PrivacyPermissionHelper.setPrivacyAgreed(this@SettingsActivity,false)

        //延时自动退出
        Handler(Looper.getMainLooper()).postDelayed({
            //退出
            finishAffinity()
            //结束进程
            exitProcess(0)
        }, 500)
    }


    //播放页样式
    private fun choosePlayPageType(playPageType: Int){
        when(playPageType){
            SettingsRequestCenter.PlayPageType_Oro -> {
                SettingsRequestCenter.SET_PRF_PlayPageType(this,playPageType)
                showCustomToast("成功设置播放页样式为经典版本", 3)
                updatePlayPageTypeText()
            }
            SettingsRequestCenter.PlayPageType_Neo -> {
                SettingsRequestCenter.SET_PRF_PlayPageType(this,playPageType)
                showCustomToast("成功设置播放页样式为新晋版本", 3)
                updatePlayPageTypeText()
            }
            SettingsRequestCenter.PlayPageType_Test -> {
                SettingsRequestCenter.SET_PRF_PlayPageType(this,playPageType)
                showCustomToast("当前包中未包含测试版界面", 3)
            }
        }
    }
    private fun updatePlayPageTypeText(){
        val ButtonPlayerTypeText = findViewById<TextView>(R.id.ButtonPlayerTypeText)
        val PlayPageType = SettingsRequestCenter.GET_PRF_PlayPageType(this)
        when(PlayPageType){
            SettingsRequestCenter.PlayPageType_Oro -> ButtonPlayerTypeText.text = "经典"
            SettingsRequestCenter.PlayPageType_Neo -> ButtonPlayerTypeText.text = "新晋"
            SettingsRequestCenter.PlayPageType_Test -> ButtonPlayerTypeText.text = "测试"
        }
    }
    //寻帧间隔
    private fun chooseSeekHandlerGap(gap: Long) {
        ToolVibrate().vibrate(this)
        SettingsRequestCenter.set_value_seekVideo_runnableGapMs(this,gap)
        updateSeekHandlerGapText()
    }
    private fun setSeekHandlerGapByInput(){
        ToolVibrate().vibrate(this)
        //创建对话框
        val dialog = Dialog(this).apply {
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_value, null)
        dialog.setContentView(dialogView)

        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description: TextView = dialogView.findViewById(R.id.dialog_description)
        val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
        val Button: Button = dialogView.findViewById(R.id.dialog_button)
        title.text = "自定义播放器寻帧间隔"
        Description.text = "仅控制滚动进度条时的寻帧间隔"
        EditText.hint = "以毫秒为单位"
        Button.text = "确定"

        val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        Button.setOnClickListener {
            val gapInput = EditText.text.toString().toLongOrNull()
            if (gapInput == null || gapInput == 0L) {
                showCustomToast("未输入内容", 3)
                dialog.dismiss()
                return@setOnClickListener
            }
            else if (gapInput > 1000) {
                showCustomToast("寻帧间隔不能大于1秒", 3)
                dialog.dismiss()
                return@setOnClickListener
            }
            else {
                //设置寻帧间隔
                SettingsRequestCenter.set_value_seekVideo_runnableGapMs(this,gapInput)
                //界面刷新
                updateSeekHandlerGapText()

                dialog.dismiss()
            }
        }
        dialog.show()
        //自动弹出键盘程序
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            EditText.requestFocus()
            @Suppress("DEPRECATION")
            imm.showSoftInput(EditText, InputMethodManager.SHOW_IMPLICIT)
        }

    }
    private fun updateSeekHandlerGapText(){
        val ButtonTextSeekHandlerGap = findViewById<TextView>(R.id.ButtonTextSeekHandlerGap)
        when(val seekHandlerGap = SettingsRequestCenter.get_value_seekVideo_runnableGapMs(this)){
            0L -> ButtonTextSeekHandlerGap.text = "无间隔"
            16L -> ButtonTextSeekHandlerGap.text = "60 Hz"
            12L -> ButtonTextSeekHandlerGap.text = "90 Hz"
            33L -> ButtonTextSeekHandlerGap.text = "30 Hz"
            66L -> ButtonTextSeekHandlerGap.text = "15 Hz"
            else -> ButtonTextSeekHandlerGap.text = "$seekHandlerGap 毫秒"
        }
    }
    //时间戳刷新间隔
    private fun chooseTimeUpdateGap(gap: Long) {
        ToolVibrate().vibrate(this)
        SettingsRequestCenter.set_value_timeStamp_updateGapMs(this,gap)
        updateTimerUpdateGapText()
    }
    private fun setTimerUpdateGapByInput() {
        ToolVibrate().vibrate(this)
        //创建对话框
        val dialog = Dialog(this).apply {
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_value, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description: TextView = dialogView.findViewById(R.id.dialog_description)
        val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
        val Button: Button = dialogView.findViewById(R.id.dialog_button)

        title.text = "自定义时间戳更新间隔"
        Description.text = "仅控制滚动进度条时的时间戳更新间隔"
        EditText.hint = "以毫秒为单位"
        Button.text = "确定"

        val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        Button.setOnClickListener {
            val gapInput = EditText.text.toString().toLongOrNull()
            if (gapInput == null || gapInput == 0L) {
                showCustomToast("未输入内容", 3)
                dialog.dismiss()
                return@setOnClickListener

            }
            else if (gapInput > 1000) {
                showCustomToast("时间更新间隔不能大于1秒", 3)
                dialog.dismiss()
                return@setOnClickListener
            }
            else {
                SettingsRequestCenter.set_value_timeStamp_updateGapMs(this,gapInput)
                //界面刷新
                updateTimerUpdateGapText()
                dialog.dismiss()
            }
        }
        dialog.show()
        //自动弹出键盘程序
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            EditText.requestFocus()
            @Suppress("DEPRECATION")
            imm.showSoftInput(EditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    private fun updateTimerUpdateGapText(){
        val ButtonTextTimerUpdateGap = findViewById<TextView>(R.id.ButtonTextTimerUpdateGap)
        when(val timerUpdateGap = SettingsRequestCenter.get_value_timeStamp_updateGapMs(this)){
            8L -> ButtonTextTimerUpdateGap.text = "120 Hz"
            12L -> ButtonTextTimerUpdateGap.text = "90 Hz"
            16L -> ButtonTextTimerUpdateGap.text = "60 Hz"
            33L -> ButtonTextTimerUpdateGap.text = "30 Hz"
            66L -> ButtonTextTimerUpdateGap.text = "15 Hz"
            else -> ButtonTextTimerUpdateGap.text = "$timerUpdateGap 毫秒"
        }
    }
    //进度条刷新间隔
    private fun chooseScrollerUpdateGap(anchor: CardView){
        //使用弹出菜单选择
        val popup = PopupMenu(this@SettingsActivity, anchor)
        popup.menuInflater.inflate(
            R.menu.popup_menu_scroller_update_gap,
            popup.menu
        )
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_item_120hz -> {
                    ToolVibrate().vibrate(this)
                    chooseScrollerUpdateGapCore(0L); true
                }
                R.id.menu_item_90hz -> {
                    ToolVibrate().vibrate(this)
                    chooseScrollerUpdateGapCore(12L); true
                }
                R.id.menu_item_60hz -> {
                    ToolVibrate().vibrate(this)
                    chooseScrollerUpdateGapCore(16L); true
                }
                R.id.menu_item_30hz -> {
                    ToolVibrate().vibrate(this)
                    chooseScrollerUpdateGapCore(33L); true
                }
                R.id.menu_item_15hz -> {
                    ToolVibrate().vibrate(this)
                    chooseScrollerUpdateGapCore(66L); true
                }
                R.id.menu_item_Input -> {
                    ToolVibrate().vibrate(this)
                    setScrollerUpdateGapByInput(); true
                }
                else -> true
            }
        }
        popup.show()

    }
    private fun chooseScrollerUpdateGapCore(gap: Long) {
        SettingsRequestCenter.set_value_syncScroller_runnableGapMs(this,gap)
        updateScrollerUpdateGapText()
    }
    private fun setScrollerUpdateGapByInput() {
        ToolVibrate().vibrate(this)
        //创建对话框
        val dialog = Dialog(this).apply {
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_value, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description: TextView = dialogView.findViewById(R.id.dialog_description)
        val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
        val Button: Button = dialogView.findViewById(R.id.dialog_button)

        title.text = "自定义进度条更新间隔"
        Description.text = "仅控制进度条自主滚动时的更新间隔"
        EditText.hint = "以毫秒为单位"
        Button.text = "确定"

        val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        Button.setOnClickListener {
            val gapInput = EditText.text.toString().toLongOrNull()
            if (gapInput == null || gapInput == 0L) {
                showCustomToast("未输入内容", 3)
                dialog.dismiss()
                return@setOnClickListener

            }
            else if (gapInput > 1000) {
                showCustomToast("时间更新间隔不能大于1秒", 3)
                dialog.dismiss()
                return@setOnClickListener
            }
            else{
                SettingsRequestCenter.set_value_syncScroller_runnableGapMs(this,gapInput)
                //界面刷新
                updateScrollerUpdateGapText()
                dialog.dismiss()
            }
        }
        dialog.show()
        //自动弹出键盘程序
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            EditText.requestFocus()
            @Suppress("DEPRECATION")
            imm.showSoftInput(EditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    private fun updateScrollerUpdateGapText(){
        val ButtonTextScrollerUpdateGap = findViewById<TextView>(R.id.ButtonText_scrollerUpdateGap)
        val scrollerUpdateGap = SettingsRequestCenter.get_value_syncScroller_runnableGapMs(this)
        //consoleLog("updateScrollerUpdateGapText: $scrollerUpdateGap")
        when(scrollerUpdateGap){
            0L -> ButtonTextScrollerUpdateGap.text = "120 Hz"
            12L -> ButtonTextScrollerUpdateGap.text = "90 Hz"
            16L -> ButtonTextScrollerUpdateGap.text = "60 Hz"
            33L -> ButtonTextScrollerUpdateGap.text = "30 Hz"
            66L -> ButtonTextScrollerUpdateGap.text = "15 Hz"
            else -> ButtonTextScrollerUpdateGap.text = "$scrollerUpdateGap 毫秒"
        }
    }
    //SeekBar刷新间隔
    private fun chooseSeekBarUpdateGap(anchor: CardView){
        //使用弹出菜单选择
        val popup = PopupMenu(this@SettingsActivity, anchor)
        popup.menuInflater.inflate(
            R.menu.popup_menu_seekbar_update_gap,
            popup.menu
        )
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId){
                R.id.menu_item_100ms -> {
                    ToolVibrate().vibrate(this)
                    chooseSeekBarUpdateGapCore(100L); true
                }
                R.id.menu_item_250ms -> {
                    ToolVibrate().vibrate(this)
                    chooseSeekBarUpdateGapCore(250L); true
                }
                R.id.menu_item_500ms -> {
                    ToolVibrate().vibrate(this)
                    chooseSeekBarUpdateGapCore(500L); true
                }
                R.id.menu_item_750ms -> {
                    ToolVibrate().vibrate(this)
                    chooseSeekBarUpdateGapCore(750L); true
                }
                R.id.menu_item_1000ms -> {
                    ToolVibrate().vibrate(this)
                    chooseSeekBarUpdateGapCore(1000L); true
                }
                R.id.menu_item_Input -> {
                    ToolVibrate().vibrate(this)
                    setSeekBarUpdateGapByInput(); true
                }
                else -> true
            }
        }
        popup.show()

    }
    private fun chooseSeekBarUpdateGapCore(gap: Long) {
        SettingsRequestCenter.set_value_syncSeekbar_runnableGapMs(this,gap)
        updateSeekBarUpdateGapText()
    }
    private fun setSeekBarUpdateGapByInput(){
        ToolVibrate().vibrate(this)
        //创建对话框
        val dialog = Dialog(this).apply {
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_value, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description: TextView = dialogView.findViewById(R.id.dialog_description)
        val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
        val Button: Button = dialogView.findViewById(R.id.dialog_button)

        title.text = "自定义进度条更新间隔"
        Description.text = "仅控制进度条自主滚动时的更新间隔"
        EditText.hint = "以毫秒为单位"
        Button.text = "确定"

        val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        Button.setOnClickListener {
            val gapInput = EditText.text.toString().toLongOrNull()
            if (gapInput == null) {
                showCustomToast("未输入内容", 3)
                dialog.dismiss()
                return@setOnClickListener
            }
            if (gapInput < 0L) {
                showCustomToast("时间更新间隔不能小于0", 3)
                dialog.dismiss()
                return@setOnClickListener
            }
            if (gapInput > 3000){
                showCustomToast("时间更新间隔不能大于3秒", 3)
                dialog.dismiss()
                return@setOnClickListener
            }

            //输入检查完成
            SettingsRequestCenter.set_value_syncSeekbar_runnableGapMs(this,gapInput)
            //界面刷新
            updateSeekBarUpdateGapText()
            dialog.dismiss()

        }
        dialog.show()
        //自动弹出键盘程序
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            EditText.requestFocus()
            @Suppress("DEPRECATION")
            imm.showSoftInput(EditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    private fun updateSeekBarUpdateGapText(){
        val ButtonTextSeekBarUpdateGap = findViewById<TextView>(R.id.ButtonText_seekbarUpdateGap)
        val seekBarUpdateGap = SettingsRequestCenter.get_value_syncSeekbar_runnableGapMs(this)
        //consoleLog("updateSeekBarUpdateGapText: $seekBarUpdateGap")
        when(seekBarUpdateGap){
            0L -> ButtonTextSeekBarUpdateGap.text = "无间隔(推荐设为更高值)"
            in 1L..50L -> ButtonTextSeekBarUpdateGap.text = "$seekBarUpdateGap 毫秒 (推荐设为更高值)"
            in 50L..999L -> ButtonTextSeekBarUpdateGap.text = "$seekBarUpdateGap 毫秒"
            in 1000L..3000L -> ButtonTextSeekBarUpdateGap.text = "${seekBarUpdateGap/1000f}秒"
            else -> ButtonTextSeekBarUpdateGap.text = "$seekBarUpdateGap 毫秒"
        }
    }
    //振动模式
    private fun chooseVibrateMode(mode: Int) {
        //振动模式表
        // 0 = No Vibrate
        // 1 = VibrationEffect.EFFECT_CLICK
        // 2 = VibrationEffect.EFFECT_TICK
        // 3 = VibrationEffect.EFFECT_DOUBLE_CLICK
        // 4 = VibrationEffect.EFFECT_HEAVY_CLICK


        ToolVibrate().setVibrateMode(this, mode)

        ToolVibrate().vibrate(this)

        updateVibrateModeText()

    }
    private fun updateVibrateModeText() {
        val ButtonTextVibrateMode = findViewById<TextView>(R.id.ButtonTextVibrateMode)
        val vibrateMode = ToolVibrate().getVibrateMode(this)
        when(vibrateMode){
            0 -> ButtonTextVibrateMode.text = "无振动"
            1 -> ButtonTextVibrateMode.text = "EFFECT_CLICK"
            2 -> ButtonTextVibrateMode.text = "EFFECT_TICK"
            3 -> ButtonTextVibrateMode.text = "EFFECT_DOUBLE_CLICK"
            4 -> ButtonTextVibrateMode.text = "EFFECT_HEAVY_CLICK"
            5 -> ButtonTextVibrateMode.text = "100Ms 默认振动 (OPPO专用)"
        }

    }
    //删除缩略图缓存
    private fun chooseDeleteFrameItem(anchor: View){
        val popup = PopupMenu(this@SettingsActivity, anchor)
        popup.menuInflater.inflate(R.menu.popup_menu_setting_delete_frame, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.delete_all -> {
                    ToolVibrate().vibrate(this)

                    deleteArtworkFrameCache(
                        context = this,
                        deleteVideo = true,
                        deleteAudio = true
                    )

                    true
                }
                R.id.delete_video -> {
                    ToolVibrate().vibrate(this)

                    deleteArtworkFrameCache(
                        context = this,
                        deleteVideo = true,
                        deleteAudio = false
                    )

                    true
                }
                R.id.delete_audio -> {
                    ToolVibrate().vibrate(this)

                    deleteArtworkFrameCache(
                        context = this,
                        deleteVideo = false,
                        deleteAudio = true
                    )

                    true
                }
                R.id.delete_video_custom -> {
                    ToolVibrate().vibrate(this)

                    deleteCustomFrameCache(
                        context = this,
                        deleteVideo = true,
                        deleteAudio = false
                    )

                    true
                }
                R.id.delete_audio_custom -> {
                    ToolVibrate().vibrate(this)

                    deleteCustomFrameCache(
                        context = this,
                        deleteVideo = false,
                        deleteAudio = true
                    )

                    true
                }
                else -> true
            }
        }
        popup.show()
    }
    private fun deleteArtworkFrameCache(context: Context, deleteVideo:Boolean = false, deleteAudio:Boolean = false){
        AlertDialog.Builder(this@SettingsActivity)
            .setTitle("确定删除选中的默认封面吗?")
            .setMessage("仅删除自动生成的封面，保留自定义封面")
            .setPositiveButton("确认") { dialog, _ ->
                ToolVibrate().vibrate(this)


                lifecycleScope.launch(Dispatchers.IO){
                    val success = ArtworkFrameManager.delete_artwork(
                        context = context,
                        deleteVideo = deleteVideo,
                        deleteAudio = deleteAudio
                    )

                    withContext(Dispatchers.Main){
                        if (success){
                            showCustomToast("删除成功", 3)
                        }else{
                            showCustomToast("删除失败", 3)
                        }
                    }
                }

                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                ToolVibrate().vibrate(this)

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
    private fun deleteCustomFrameCache(context: Context, deleteVideo:Boolean = false, deleteAudio:Boolean = false){
        AlertDialog.Builder(this@SettingsActivity)
            .setTitle("确定删除选中的自定义封面吗?")
            .setMessage("更建议您在播放页删除单个媒体的自定义封面")
            .setPositiveButton("确认") { dialog, _ ->
                ToolVibrate().vibrate(this)

                lifecycleScope.launch(Dispatchers.IO){
                    val success = ArtworkFrameManager.delete_artwork_custom(
                        context = context,
                        deleteVideo = deleteVideo,
                        deleteAudio = deleteAudio
                    )

                    withContext(Dispatchers.Main){
                        if (success){
                            showCustomToast("删除成功", 3)
                        }else{
                            showCustomToast("删除失败", 3)
                        }
                    }
                }

                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                ToolVibrate().vibrate(this)

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
    //删除媒体数据缓存
    private fun chooseDeleteDB(anchor: View){
        val popup = PopupMenu(this@SettingsActivity, anchor)
        popup.menuInflater.inflate(R.menu.popup_menu_setting_delete_db, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.delete_video -> {
                    ToolVibrate().vibrate(this)

                    deleteDB(
                        context = this,
                        deleteVideo = true,
                        deleteAudio = false
                    )

                    true
                }
                R.id.delete_audio -> {
                    ToolVibrate().vibrate(this)

                    deleteDB(
                        context = this,
                        deleteVideo = false,
                        deleteAudio = true
                    )

                    true
                }
                else -> true
            }
        }
        popup.show()
    }
    private fun deleteDB(context: Context, deleteVideo:Boolean = false, deleteAudio:Boolean = false){
        AlertDialog.Builder(this@SettingsActivity)
            .setTitle(if (deleteVideo )"确定删除视频数据缓存吗?" else "确定删除音频数据缓存吗?")
            .setMessage("回到主页后会触发再次读取,仅作为清除异常数据使用")
            .setPositiveButton("确认") { dialog, _ ->
                ToolVibrate().vibrate(this)

                lifecycleScope.launch(Dispatchers.IO){
                    deleteDB_Core(context,deleteVideo,deleteAudio)
                }

                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                ToolVibrate().vibrate(this)

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
    suspend fun deleteDB_Core(context: Context, deleteVideo:Boolean = false, deleteAudio:Boolean = false){
        if (deleteVideo){
            //链接数据库仓库
            val mediaStoreRepo = VideoRepo.get(context)
            val deleteCount = mediaStoreRepo.clearAll()
            if (deleteCount > 0){
                withContext(Dispatchers.Main){
                    showCustomToast("成功删除${deleteCount}条视频数据", 3)
                }
            }else{
                withContext(Dispatchers.Main){
                    showCustomToast("未删除任何数据", 3)
                }
            }
        }
        if (deleteAudio){
            //链接数据库仓库
            val mediaStoreRepo = AudioRepo.get(context)
            val deleteCount = mediaStoreRepo.clearAll()
            if (deleteCount > 0){
                withContext(Dispatchers.Main){
                    showCustomToast("成功删除${deleteCount}条音频数据", 3)
                }
            }else{
                withContext(Dispatchers.Main){
                    showCustomToast("未删除任何数据", 3)
                }
            }
        }
    }


    //顶栏效果
    private var isTopBarTitleVisible = true
    private fun topBarEffect_Out_Title(){
        if (!isTopBarTitleVisible) return
        isTopBarTitleVisible = false

        AppBarTitle.animate()
            .alpha(0f)
            .setDuration(animDuration)
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
            .setDuration(animDuration)
            .start()
    }
    private var isTopBarBrushVisible = true
    private fun topBarEffect_Out_Brush(){
        if (!isTopBarBrushVisible) return
        isTopBarBrushVisible = false

        AppBarGradientMask.alpha = 1f
        AppBarGradientMask.animate()
            .alpha(0f)
            .setDuration(animDuration)
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
            .setDuration(animDuration)
            .start()
    }
    private var animDuration: Long = 250
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


    //界面配置
    private lateinit var AppBarGradientMask: View
    private lateinit var AppBarCore: LinearLayout
    private lateinit var AppBarSpacer: Space
    private lateinit var AppBarTitle: TextView
    private fun display(){
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        //初始化顶部栏
        AppBarGradientMask = findViewById(R.id.AppBarGradientMask)
        AppBarCore = findViewById(R.id.AppBarCore)
        AppBarSpacer = findViewById(R.id.AppBarSpacer)
        AppBarTitle = findViewById(R.id.AppBarTitle)
        //获取状态栏高度
        if (DeviceInfo.statusBarHeight != 0){
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->

                DeviceInfo.statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                onStatusBarHeightGet(DeviceInfo.statusBarHeight)

                insets
            }
        }

    }
    private fun onStatusBarHeightGet(statusBarHeight: Int){
        AppBarGradientMask.layoutParams.height = statusBarHeight + dpToPx(60f).toInt()
        (AppBarCore.layoutParams as ViewGroup.MarginLayoutParams).topMargin = statusBarHeight
        AppBarSpacer.layoutParams.height = statusBarHeight + dpToPx(60f).toInt()
    }
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


    //检查应用列表
    private var packageNumber = 0
    private fun checkMicroG(): Boolean {
        packageNumber = 0
        val packageManager = packageManager
        val installedPackages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        var packageName: String
        for (packageInfo in installedPackages) {
            packageName = packageInfo.packageName
            packageNumber++
            if (packageName == "com.google.android.gms"){
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val label = appInfo.loadLabel(packageManager).toString()
                if (label.contains("microG")){
                    return true
                }
            }
        }
        return false
    }
    //测试版可用性检查
    private fun allowUseTestPlayer(): Boolean{
        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signingInfo = packageInfo.signingInfo

        if (signingInfo == null) {
            showCustomToast("签名错误", 3)
            return false
        }

        if (signingInfo.hasMultipleSigners()) {
            showCustomToast("签名错误", 3)
            return false
        }

        val signatures = signingInfo.signingCertificateHistory
        for (sig in signatures) {
            val cert = sig.toByteArray()
            val input = ByteArrayInputStream(cert)
            val cf = CertificateFactory.getInstance("X509")
            val c = cf.generateCertificate(input) as X509Certificate
            val name = c.subjectDN.name
            if (name.contains("Android Debug")) {
                showCustomToast("当前程序可使用测试版界面", 3)
                return true
            }else{
                showCustomToast("非Debug版本不能使用测试版页面", 3)
                return false
            }
        }
        return true
    }

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "SettingsActivity: $msg")
        }
    }

}