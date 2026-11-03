package com.suming.player

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Space
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.suming.player.ActivityComponent.OSS_LICENSE.OssLicensePage
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.BASE_TOOL_BUNDLE.BASE_TOOL_BUNDLE
import com.suming.player.DataPack.DataBaseMediaStore.Audio.AudioRepo
import com.suming.player.DataPack.DataBaseMediaStore.Video.VideoRepo
import com.suming.player.DataPack.ReleaseInfo
import com.suming.player.EVENT_BUS.AppEvent
import com.suming.player.EVENT_BUS.AppEventList
import com.suming.player.EVENT_BUS.EventBus_Embedded
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.DeviceInfo
import com.suming.player.FuncionalPack.DownloadManager
import com.suming.player.FuncionalPack.MediaRecordManager
import com.suming.player.FuncionalPack.PrivacyPermissionHelper
import com.suming.player.FuncionalPack.SettingsCenter
import com.suming.player.FuncionalPack.SettingsHelper
import com.suming.player.UI_SEG_FUNCTION.UI_FUNCTION
import com.suming.player.UI_SEG_FUNCTION.ValidationResult
import com.suming.player.UI_SEG_View.CircleButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds

@Suppress("/unused","NewApi")
@SuppressLint("InflateParams", "SetTextI18n")
@OptIn(UnstableApi::class)
class SettingsActivity: AppCompatActivity() {

    //context
    private val context: Context = this@SettingsActivity
    //UI_FUNCTION
    val UI_FUNCTION = UI_FUNCTION()
    //
    val BASE_TOOL_BUNDLE = BASE_TOOL_BUNDLE()
    //振动
    @Suppress("NewApi")
    private fun vibrate() {
        ToolVibrate.vibrate()
    }





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

    override fun onDestroy() {
        super.onDestroy()
        //取消下载
        //downloadJob?.cancel()
    }

    private fun init(){
        display()

        progressLayout = findViewById(R.id.downloadProgressLayout)
        progressBar = findViewById(R.id.downloadProgressBar)
        progressText = findViewById(R.id.downloadProgressText)

    }


    //注册事件
    private fun register() {
        lifecycleScope.launch(Dispatchers.Main) {
            //显示版本
            lifecycleScope.launch(Dispatchers.IO) {
                delay(500.milliseconds)
                //获取App版本号
                if (AppVersion == null) AppVersion = BASE_TOOL_BUNDLE.getAppVersion(context)
                //获取是不是debug版
                val isDebug = BASE_TOOL_BUNDLE.isDebugVersion(context)
                //显示版本
                withContext(Dispatchers.Main) {
                    val versionText = findViewById<TextView>(R.id.version)
                    versionText.text = "版本: $AppVersion" + if (isDebug) " (Debug)" else ""
                }

            }

            //按钮：返回
            val ButtonBack = findViewById<CircleButton>(R.id.buttonExit)
            ButtonBack.setOnClickListener {
                finish()
            }
            //按钮：前往项目Github仓库页
            val ButtonGoGithubRepo = findViewById<TextView>(R.id.Button_GoTo_GithubRepo)
            ButtonGoGithubRepo.setOnClickListener {
                vibrate()

                AlertDialog.Builder(context)
                    .setTitle("将跳转至浏览器")
                    .setMessage("是否继续?")
                    .setPositiveButton("确认") { dialog, _ ->
                        vibrate()

                        val url = "https://github.com/JeanValjean07/Player/"
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        startActivity(intent)

                        dialog.dismiss()
                    }
                    .setNegativeButton("取消") { dialog, _ ->
                        vibrate()

                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()


            }
            //按钮：前往项目Github发布页
            val ButtonGoGithubRelease = findViewById<TextView>(R.id.Button_GoTo_GithubRelease)
            ButtonGoGithubRelease.setOnClickListener {
                vibrate()

                AlertDialog.Builder(context)
                    .setTitle("将跳转至浏览器")
                    .setMessage("是否继续?")
                    .setPositiveButton("确认") { dialog, _ ->
                        vibrate()

                        val url = "https://github.com/JeanValjean07/Player/releases"
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        startActivity(intent)

                        dialog.dismiss()
                    }
                    .setNegativeButton("取消") { dialog, _ ->
                        vibrate()

                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()



            }
            //按钮：自动检查更新
            val ButtonCheckUpdate = findViewById<TextView>(R.id.ButtonAutoUpdate)
            ButtonCheckUpdate.setOnClickListener {
                vibrate()
                //检查更新
                checkNewVersion()
            }
            //点击顶部区域
            AppBarCore.setOnClickListener {
                if (scrollArea?.canScrollVertically(-1) == true){
                    vibrate()
                    //滚动区域回顶
                    scrollArea?.stopNestedScroll()
                    scrollArea?.smoothScrollTo(0, 0)
                }else{
                    scrollArea?.stopNestedScroll()
                }
            }



            //撤回隐私政策同意
            val RevokePrivacyAgreement = findViewById<TextView>(R.id.RevokePrivacyAgreement)
            RevokePrivacyAgreement.paint.isUnderlineText = true
            RevokePrivacyAgreement.invalidate()
            RevokePrivacyAgreement.setOnClickListener {
                vibrate()

                //撤回隐私政策同意
                revokePrivacyAgreementAlert()


            }
            //超链接：开放源代码许可
            val openSourceLicense = findViewById<TextView>(R.id.openSourceLicense)
            openSourceLicense.paint.isUnderlineText = true
            openSourceLicense.invalidate()
            openSourceLicense.setOnClickListener {
                vibrate()
                //启动OSS页面
                startActivity(Intent(context, OssLicensePage::class.java))

            }
            //超链接：设备信息
            val DeviceInfoPage = findViewById<TextView>(R.id.DeviceInfoPage)
            DeviceInfoPage.paint.isUnderlineText = true
            DeviceInfoPage.invalidate()
            DeviceInfoPage.setOnClickListener {
                vibrate()

                startActivity(Intent(context, DeviceInfoActivity::class.java))
            }


        }
    }


    //注册设置项
    private fun registerSettings(){
        lifecycleScope.launch(Dispatchers.Main) {
            delay(100.milliseconds)

            //通用设置
            //<editor-fold desc="////通用设置">
            //启用视频跨页播放/启用首页MiniView
            val SC_EnableMiniView = findViewById<SwitchCompat>(R.id.SC_EnableMiniView)
            SC_EnableMiniView.isChecked = SettingsCenter.GET_PRF_EnableMiniView()
            SC_EnableMiniView.setOnCheckedChangeListener { _, isChecked ->
                vibrate()
                SettingsCenter.SET_PRF_EnableMiniView(isChecked)
            }
            //首页播放卡片用缩略图代替视频
            val switch_DisableMainPageSmallPlayer = findViewById<SwitchCompat>(R.id.DisableMainPageSmallPlayer)
            switch_DisableMainPageSmallPlayer.isChecked = SettingsCenter.GET_PRF_AlwaysUseImageInMiniView()
            switch_DisableMainPageSmallPlayer.setOnCheckedChangeListener { _, isChecked ->
                vibrate()
                SettingsCenter.SET_PRF_AlwaysUseImageInMiniView( isChecked)
            }
            //启动时继续上次的媒体
            val switch_EnableContinuePlay = findViewById<SwitchCompat>(R.id.EnableContinuePlay)
            switch_EnableContinuePlay.isChecked = SettingsCenter.get_PREFS_EnableContinuePlay()
            switch_EnableContinuePlay.setOnCheckedChangeListener { _, isChecked ->
                vibrate()
                SettingsCenter.set_PREFS_EnableContinuePlay( isChecked)
            }
            //启动续播时启动播放器引擎
            val SC_ContinuePlay_withEngin = findViewById<SwitchCompat>(R.id.SC_ContinuePlay_withEngin)
            SC_ContinuePlay_withEngin.isChecked = SettingsCenter.GET_PRF_ContinuePlay_withEngin()
            SC_ContinuePlay_withEngin.setOnCheckedChangeListener { _, isChecked ->
                vibrate()
                SettingsCenter.SET_PRF_ContinuePlay_withEngin(isChecked)
            }
            //后台划卡时关闭播放器
            val switch_StopPlayerWhenTaskRemoved = findViewById<SwitchCompat>(R.id.StopPlayerWhenTaskRemoved)
            switch_StopPlayerWhenTaskRemoved.isChecked = SettingsCenter.GET_PRF_stopEngine_whenTaskRemoved()
            switch_StopPlayerWhenTaskRemoved.setOnCheckedChangeListener { _, isChecked ->
                vibrate()
                SettingsCenter.SET_PRF_stopEngine_whenTaskRemoved(isChecked)
            }
            //</editor-fold>

            //播放器引擎设置
            //<editor-fold desc="////播放器引擎设置">
            //启用媒体会话艺术图
            val SC_EnableMediaSessionArtWork = findViewById<SwitchCompat>(R.id.SC_EnableMediaSessionArtWork)
            SC_EnableMediaSessionArtWork.isChecked = SettingsCenter.GET_PRF_EnableMediaSessionArtWork()
            SC_EnableMediaSessionArtWork.setOnCheckedChangeListener { _, isChecked ->
                vibrate()

                fun set_isChecked_byProcess_include_write_setting(target_isChecked: Boolean){
                    val current_isChecked = SC_EnableMediaSessionArtWork.isChecked
                    if (target_isChecked != current_isChecked){
                        set_switch_from_process = true
                        SC_EnableMediaSessionArtWork.isChecked = target_isChecked
                    }else{
                        set_switch_from_process = false
                    }
                    //写入设置
                    SettingsCenter.SET_PRF_EnableMediaSessionArtWork(target_isChecked)
                }

                if (isChecked){
                    if (set_switch_from_process){
                        set_switch_from_process = false
                        set_isChecked_byProcess_include_write_setting(true)
                    }else{
                        //检查是否可以开启
                        val result = SettingsHelper.spySupport_EnableMediaSessionArtWork()
                        when(result){
                            1 -> {
                                //允许打开
                                set_isChecked_byProcess_include_write_setting(true)
                            }
                            2 -> {
                                //禁止打开,主动置为关闭
                                set_isChecked_byProcess_include_write_setting(false)
                                //显示提示
                                AlertDialog.Builder(context)
                                    .setTitle("禁止启用此选项")
                                    .setMessage("已确认在当前设备上存在兼容性问题，不能启用此选项")
                                    .setPositiveButton("了解") { dialog, _ ->
                                        vibrate()

                                        dialog.dismiss()
                                    }
                                    .setCancelable(cancel())
                                    .show()
                            }
                            3 -> {
                                //允许打开
                                set_isChecked_byProcess_include_write_setting(true)
                                //显示提示
                                AlertDialog.Builder(context)
                                    .setTitle("未测试兼容性")
                                    .setMessage("如果后续遇到异常，请关闭此选项")
                                    .setPositiveButton("了解") { dialog, _ ->
                                        vibrate()

                                        dialog.dismiss()
                                    }
                                    .setCancelable(cancel())
                                    .show()
                            }
                            else -> {
                                //强制关闭
                                set_isChecked_byProcess_include_write_setting(false)
                                //显示提示
                                AlertDialog.Builder(context)
                                    .setTitle("无法开启此选项")
                                    .setMessage("未知错误")
                                    .setPositiveButton("了解") { dialog, _ ->
                                        vibrate()

                                        dialog.dismiss()
                                    }
                                    .setCancelable(cancel())
                                    .show()
                            }
                        }
                    }

                }else{
                    set_switch_from_process = false
                    set_isChecked_byProcess_include_write_setting(false)
                }

            }
            //后台播放时关闭视频轨道
            val switch_DisableVideoTrackOnBack = findViewById<SwitchCompat>(R.id.DisableVideoTrackOnBack)
            switch_DisableVideoTrackOnBack.isChecked = SettingsCenter.GET_PREFS_DisableVideoTrack_whenBackground()
            switch_DisableVideoTrackOnBack.setOnCheckedChangeListener { _, isChecked ->
                vibrate()

                fun set_isChecked_byProcess_include_write_setting(target_isChecked: Boolean){
                    val current_isChecked = switch_DisableVideoTrackOnBack.isChecked
                    if (target_isChecked != current_isChecked){
                        set_switch_from_process = true
                        switch_DisableVideoTrackOnBack.isChecked = target_isChecked
                    }else{
                        set_switch_from_process = false
                    }
                    //写入设置
                    SettingsCenter.SET_PREFS_DisableVideoTrack_whenBackground(target_isChecked)
                }

                if (isChecked){
                    if (set_switch_from_process){
                        set_isChecked_byProcess_include_write_setting(true)
                    }else{
                        val AndroidVersion = DeviceInfo.GET_AndroidVersion()
                        if (AndroidVersion >= Build.VERSION_CODES.TIRAMISU){
                            //先程序化关闭
                            set_isChecked_byProcess_include_write_setting(false)

                            //显示提示
                            AlertDialog.Builder(context)
                                .setTitle("提示")
                                .setMessage("开启此选项后，切换轨道时会短暂停止播放，影响播放体验。其次，即使是持续解码视频的消耗也非常低，非常不建议开启此选项。")
                                .setPositiveButton("仍要开启") { dialog, _ ->
                                    vibrate()
                                    //仍要开启
                                    set_isChecked_byProcess_include_write_setting(true)

                                    dialog.dismiss()
                                }
                                .setNegativeButton("不开启了") { dialog, _ ->
                                    vibrate()
                                    //
                                    set_isChecked_byProcess_include_write_setting(false)

                                    dialog.dismiss()
                                }
                                .setCancelable(cancel())
                                .show()
                        }else{
                            //先程序化关闭
                            set_isChecked_byProcess_include_write_setting(false)
                            //显示提示
                            AlertDialog.Builder(context)
                                .setTitle("无法启用")
                                .setMessage("受稳定性限制，安卓12及以下系统禁止开启此选项")
                                .setPositiveButton("知道了") { dialog, _ ->
                                    vibrate()

                                    dialog.dismiss()
                                }
                                .setCancelable(cancel())
                                .show()
                        }

                    }


                }else{
                    set_switch_from_process = false
                    set_isChecked_byProcess_include_write_setting(false)
                }

            }
            //媒体变更冷却时长
            val ButtonCard_onMediaChangeMillis = findViewById<CardView>(R.id.ButtonCard_onMediaChangeMillis)
            updateOnMediaChangeMillisText()
            ButtonCard_onMediaChangeMillis.setOnClickListener {
                vibrate()
                set_onMediaChangeMillis()
            }
            //</editor-fold>

            //视频播放页设置
            //<editor-fold desc="////视频播放页设置">
            //播放页样式
            val Button_Video_Screening_Type = findViewById<CardView>(R.id.ButtonPlayerType)
            update_screening_type_Text()
            Button_Video_Screening_Type.setOnClickListener {
                vibrate()

                choose_screening_type_Menu(it)
            }
            //通用视频页面设置
            //启用播放区域随面板移动
            val switch_EnablePlayAreaMoveAnim = findViewById<SwitchCompat>(R.id.EnablePlayAreaMoveAnim)
            switch_EnablePlayAreaMoveAnim.isChecked = SettingsCenter.GET_PREFS_EnablePlayAreaMoveAnim()
            switch_EnablePlayAreaMoveAnim.setOnCheckedChangeListener { _, isChecked ->
                vibrate()
                SettingsCenter.SET_PREFS_EnablePlayAreaMoveAnim(isChecked)
            }
            //禁用播放区域上下滑动手势
            val SC_DisableViewFollowing = findViewById<SwitchCompat>(R.id.SC_DisableViewMoving)
            SC_DisableViewFollowing.isChecked = SettingsCenter.GET_PREFS_Video_DisableViewFollowing()
            SC_DisableViewFollowing.setOnCheckedChangeListener { _, isChecked ->
                vibrate()
                SettingsCenter.SET_PREFS_Video_DisableViewFollowing(isChecked)
            }
            //始终使用深色播放页面
            val switch_AlwaysUseDarkTheme = findViewById<SwitchCompat>(R.id.AlwaysUseDarkTheme)
            switch_AlwaysUseDarkTheme.isChecked = SettingsCenter.GET_PREFS_AlwaysUseDarkTheme()
            switch_AlwaysUseDarkTheme.setOnCheckedChangeListener { _, isChecked ->
                vibrate()
                SettingsCenter.SET_PREFS_AlwaysUseDarkTheme(isChecked)
            }
            //使用高刷新率
            val switch_EnableHighRefreshRate = findViewById<SwitchCompat>(R.id.EnableHighRefreshRate)
            switch_EnableHighRefreshRate.isChecked = SettingsCenter.GET_PREFS_LockRefreshRate()
            switch_EnableHighRefreshRate.setOnCheckedChangeListener { _, isChecked ->
                vibrate()
                SettingsCenter.SET_PREFS_LockRefreshRate(isChecked)
            }
            //退出播放页时确保竖屏
            val SC_SwitchPortrait_whenExit = findViewById<SwitchCompat>(R.id.EnsurePortraitWhenExit)
            SC_SwitchPortrait_whenExit.isChecked = SettingsCenter.GET_PRF_SwitchPortrait_whenExit()
            SC_SwitchPortrait_whenExit.setOnCheckedChangeListener { _, isChecked ->
                vibrate()

                SettingsCenter.SET_PRF_SwitchPortrait_whenExit(isChecked)
            }
            //竖屏时也开启自动隐藏控件
            val SC_EnableAutoHideController_whenPortrait = findViewById<SwitchCompat>(R.id.SC_EnableAutoHideController_whenPortrait)
            SC_EnableAutoHideController_whenPortrait.isChecked = SettingsCenter.GET_PRF_EnableAutoHideController_whenPortrait()
            SC_EnableAutoHideController_whenPortrait.setOnCheckedChangeListener { _, isChecked ->
                vibrate()
                SettingsCenter.SET_PRF_EnableAutoHideController_whenPortrait(isChecked)
            }
            //🥙经典播放页设置
            //视频页seekBar刷新间隔
            val ButtonCard_VideoSeekBar_updateMs = findViewById<CardView>(R.id.ButtonCard_VideoSeekBar_updateMs)
            update_video_syncSeekBar_updateMS_Text()
            ButtonCard_VideoSeekBar_updateMs.setOnClickListener {
                vibrate()
                choose_video_syncSeekBar_updateMs_Menu(it)
            }
            //🥙新晋播放页设置
            //进度条绘制使用兼容模式
            val switch_UseCompatScroller = findViewById<SwitchCompat>(R.id.UseCompatScroller)
            switch_UseCompatScroller.isChecked = SettingsCenter.GET_PREFS_UseCompatScroller()
            switch_UseCompatScroller.setOnCheckedChangeListener { _, isChecked ->
                vibrate()


                fun set_isChecked_byProcess_include_write_setting(target_isChecked: Boolean){
                    val current_isChecked = switch_UseCompatScroller.isChecked
                    if (target_isChecked != current_isChecked){
                        set_switch_from_process = true
                        switch_UseCompatScroller.isChecked = target_isChecked
                    }else{
                        set_switch_from_process = false
                    }
                    //写入设置
                    SettingsCenter.SET_PREFS_UseCompatScroller(target_isChecked)
                }


                if (isChecked){
                    if (set_switch_from_process){
                        set_switch_from_process = false
                        SettingsCenter.SET_PREFS_UseCompatScroller(true)
                    }else{
                        //先程序化关闭
                        set_isChecked_byProcess_include_write_setting(false)

                        //显示确认提示
                        AlertDialog.Builder(context)
                            .setTitle("提示")
                            .setMessage("仅当横屏时的进度条两端无法与中央竖线对齐时才能开启此开关，否则将导致进度条无法正常工作，请确认后再开启")
                            .setPositiveButton("我已确认并开启") { dialog, _ ->
                                vibrate()

                                set_isChecked_byProcess_include_write_setting(true)

                                dialog.dismiss()
                            }
                            .setNegativeButton("取消") { dialog, _ ->
                                vibrate()

                                set_isChecked_byProcess_include_write_setting(false)

                                dialog.dismiss()
                            }
                            .setCancelable(cancel())
                            .show()
                    }

                }else{
                    //确保状态清空
                    if (set_switch_from_process) set_switch_from_process = false

                    set_isChecked_byProcess_include_write_setting(false)
                }
            }
            //寻帧时关键帧偏好
            val BC_SyncFramePreference = findViewById<CardView>(R.id.BC_SyncFramePreference)
            update_video_syncFramePrefs_Text()
            BC_SyncFramePreference.setOnClickListener {
                vibrate()
                //
                choose_video_syncFramePrefs_Menu(it)

            }
            //时间戳刷新间隔 value
            val ButtonCardTimerUpdateGap = findViewById<CardView>(R.id.ButtonCardTimerUpdateGap)
            update_video_timerStamp_updateMs_Text()
            ButtonCardTimerUpdateGap.setOnClickListener {
                vibrate()
                //
                choose_Video_timeStamp_updateMs_Menu(it)

            }
            //视频播放页连续寻帧间隔 value
            val ButtonCardSeekHandlerGap = findViewById<CardView>(R.id.ButtonCardSeekHandlerGap)
            update_video_generalSeek_updateMs_Text()
            ButtonCardSeekHandlerGap.setOnClickListener {
                vibrate()
                //
                choose_Video_generalSeek_updateMs_Menu(it)
            }
            //进度条刷新间隔 value
            val ButtonCardScrollerUpdateGap = findViewById<CardView>(R.id.ButtonCard_scrollerUpdateGap)
            update_video_scroller_updateMs_Text()
            ButtonCardScrollerUpdateGap.setOnClickListener {
                vibrate()
                //
                choose_Video_scroller_updateMs_Menu(it)
            }
            //</editor-fold>

            //音乐播放页设置
            //<editor-fold desc="////音乐播放页设置">
            //不使用专辑封面
            val SC_Audio_DontShowAlbumFrame = findViewById<SwitchCompat>(R.id.SC_Audio_DontShowAlbumFrame)
            SC_Audio_DontShowAlbumFrame.isChecked = SettingsCenter.GET_PRF_Audio_DontShowAlbumFrame()
            SC_Audio_DontShowAlbumFrame.setOnCheckedChangeListener { _, isChecked ->
                vibrate()

                SettingsCenter.SET_PRF_Audio_DontShowAlbumFrame(isChecked)
            }
            //使用文件名作为标题
            val SC_Audio_UseFileNameAsTitle = findViewById<SwitchCompat>(R.id.SC_Audio_UseFileNameAsTitle)
            SC_Audio_UseFileNameAsTitle.isChecked = SettingsCenter.GET_PRF_Audio_UseFileNameAsTitle()
            SC_Audio_UseFileNameAsTitle.setOnCheckedChangeListener { _, isChecked ->
                vibrate()

                SettingsCenter.SET_PRF_Audio_UseFileNameAsTitle(isChecked)
            }
            //使用专辑图手势
            val SC_Audio_UseArtworkGesture = findViewById<SwitchCompat>(R.id.SC_Audio_UseArtworkGesture)
            SC_Audio_UseArtworkGesture.isChecked = SettingsCenter.GET_PRF_Audio_UseArtworkGesture()
            SC_Audio_UseArtworkGesture.setOnCheckedChangeListener { _, isChecked ->
                vibrate()

                SettingsCenter.SET_PRF_Audio_UseArtworkGesture(isChecked)
            }
            //音乐页seekBar刷新间隔
            val ButtonCard_AudioSeekBar_updateMs = findViewById<CardView>(R.id.ButtonCard_AudioSeekBar_updateMs)
            update_audio_syncSeekBar_updateMS_Text()
            ButtonCard_AudioSeekBar_updateMs.setOnClickListener {
                vibrate()
                //
                choose_audio_syncSeekBar_updateMs_Menu(it)
            }
            //自动缩放专辑封面
            val SC_Audio_AutoZoomArtwork = findViewById<SwitchCompat>(R.id.SC_Audio_AutoZoomArtwork)
            SC_Audio_AutoZoomArtwork.isChecked = SettingsCenter.GET_PRF_Audio_AutoZoomArtwork()
            SC_Audio_AutoZoomArtwork.setOnCheckedChangeListener { _, isChecked ->
                vibrate()

                SettingsCenter.SET_PRF_Audio_AutoZoomArtwork(isChecked)
            }
            //</editor-fold>

            //交互设置
            //<editor-fold desc="////交互设置">
            //播放器振动模式
            val ButtonCardVibrateMode = findViewById<CardView>(R.id.ButtonCardVibrateMode)
            updateVibrateModeText()
            ButtonCardVibrateMode.setOnClickListener {
                vibrate()

                chooseVibrateMode_Menu(it)
            }
            //全屏Fragment
            val SC_UseFullScreenFragment = findViewById<SwitchCompat>(R.id.SC_UseFullScreenFragment)
            SC_UseFullScreenFragment.isChecked = SettingsCenter.GET_PRF_UseFullScreenFragment()
            SC_UseFullScreenFragment.setOnCheckedChangeListener { _, isChecked ->
                vibrate()

                SettingsCenter.SET_PRF_UseFullScreenFragment(isChecked)

                //如果设为开启,联动关闭 播放区域跟随移动
                if (isChecked){
                    SettingsCenter.SET_PREFS_EnablePlayAreaMoveAnim(false)
                    switch_EnablePlayAreaMoveAnim.isChecked = SettingsCenter.GET_PREFS_EnablePlayAreaMoveAnim()
                }

            }
            //禁用更多操作面板下滑手势
            val switch_DisableFragmentGesture = findViewById<SwitchCompat>(R.id.DisableFragmentGesture)
            switch_DisableFragmentGesture.isChecked = SettingsCenter.GET_PREFS_DisableFragmentGesture()
            switch_DisableFragmentGesture.setOnCheckedChangeListener { _, isChecked ->
                vibrate()
                SettingsCenter.SET_PREFS_DisableFragmentGesture(isChecked)
            }
            //设置MiniView底部抬高高度
            val TB_SetMiniViewBottomPadding = findViewById<TextView>(R.id.TextButton_SetMiniViewBottomPadding)
            TB_SetMiniViewBottomPadding.setOnClickListener {
                vibrate()
                //
                setMiniViewBottomPadding()

            }
            //</editor-fold>

            //重置设置
            //<editor-fold desc="////重置设置">
            //封面缩略图管理
            val ButtonRemoveAllThumbPath = findViewById<TextView>(R.id.RemoveAllThumbPath)
            ButtonRemoveAllThumbPath.setOnClickListener {
                vibrate()

                chooseDeleteFrameItem(ButtonRemoveAllThumbPath)

            }
            //数据库缓存管理
            val ButtonManageDB = findViewById<TextView>(R.id.TextButton_DBManage)
            ButtonManageDB.setOnClickListener {
                vibrate()

                chooseDeleteDB(ButtonManageDB)
            }
            //</editor-fold>


        }
    }


    //媒体变更冷却时长
    private fun set_onMediaChangeMillis() {
        UI_FUNCTION.show_number_input_Dialog_v1_v2 (
            context = context,
            ui_title = "设置媒体变更冷却时长",
            ui_description = "用于压力测试和检测",
            ui_hint = "以毫秒为单位",
            ui_button_text = "确定",
            validate = { v ->
                when {
                    v < 0 -> ValidationResult.Reject("时长不能为负")
                    v > 3000L -> ValidationResult.Reject("不支持超过3秒")
                    else -> ValidationResult.Ok
                }
            },
            onValid = { inputValue ->
                //执行设置
                SettingsCenter.set_value_onMediaChange_delayMillis(inputValue.toLong())
                //界面刷新
                updateOnMediaChangeMillisText()
            },
            onReject = { message ->
                showCustomToast(message, 3)
            }
        )

    }
    fun updateOnMediaChangeMillisText(){
        val ButtonText_onMediaChangeMillis = findViewById<TextView>(R.id.ButtonText_onMediaChangeMillis)
        val delayMillis = SettingsCenter.get_value_onMediaChange_delayMillis()
        ButtonText_onMediaChangeMillis.text = "${delayMillis}ms"
    }

    //视频播放页
    //<editor-fold desc="////视频播放页设置函数">
    //播放页样式 Type string  screening_type_ORO
    private fun choose_screening_type_Menu(anchor:View) {
        UI_FUNCTION.show_popup_Menu_v1_v1_base_xml(context, anchor, R.menu.activity_settings_popup_player_type) { item ->
            when (item.itemId) {
                R.id.type_oro -> {
                    vibrate()

                    choose_screening_type(SettingsCenter.screening_type_ORO)
                    true
                }
                R.id.type_neo -> {
                    vibrate()

                    choose_screening_type(SettingsCenter.screening_type_NEO)
                    true
                }
                R.id.type_test -> {
                    vibrate()

                    choose_screening_type(SettingsCenter.screening_type_TEST)
                    true
                }
                else -> true
            }
        }
    }
    private fun choose_screening_type(screening_type: Int) {
        //执行设置播放页样式
        SettingsCenter.SET_PRF_Video_Screening_Type(screening_type)
        //根据选择的播放页样式，更新显示
        when(screening_type){
            SettingsCenter.screening_type_ORO -> {
                showCustomToast("已设为经典版本", 3)
            }
            SettingsCenter.screening_type_NEO -> {
                showCustomToast("已设为新晋版本", 3)
            }
            SettingsCenter.screening_type_TEST -> {
                showCustomToast("已设为测试版本", 3)
            }
        }
        //刷新显示
        update_screening_type_Text()
    }
    private fun update_screening_type_Text() {
        val button_screening_type = findViewById<TextView>(R.id.ButtonPlayerTypeText)
        //获取当前播放页样式
        val screening_type = SettingsCenter.GET_PRF_Video_Screening_Type()
        //根据选择的播放页样式，更新显示
        when (screening_type){
            SettingsCenter.screening_type_ORO -> button_screening_type.text = "经典"
            SettingsCenter.screening_type_NEO -> button_screening_type.text = "新晋"
            SettingsCenter.screening_type_TEST -> button_screening_type.text = "测试"
        }
    }
    //寻帧时关键帧偏好
    private fun choose_video_syncFramePrefs_Menu(anchor:View) {
        UI_FUNCTION.show_popup_Menu_v1_v1_base_xml(context, anchor, R.menu.popup_menu_sync_frame_prefs) { item ->
            when (item.itemId) {
                R.id.always_use_sync -> {
                    vibrate()
                    choose_video_syncFramePrefs_Core(SettingsCenter.SYNC_FRAME_PREFS_AlwaysSync)
                    true
                }
                R.id.always_use_exact -> {
                    vibrate()
                    choose_video_syncFramePrefs_Core(SettingsCenter.SYNC_FRAME_PREFS_AlwaysExact)
                    true
                }
                R.id.use_dynamic -> {
                    vibrate()
                    choose_video_syncFramePrefs_Core(SettingsCenter.SYNC_FRAME_PREFS_Dynamic)
                    true
                }
                else -> true
            }
        }

    }
    private fun choose_video_syncFramePrefs_Core(value: String) {
        //写入设置
        SettingsCenter.SET_PREFS_Video_SyncFramePrefs(value)
        //刷新显示
        update_video_syncFramePrefs_Text()

    }
    private fun update_video_syncFramePrefs_Text() {
        val BT_SyncFramePrefsText = findViewById<TextView>(R.id.BT_SyncFramePreference)
        val syncFramePrefs = SettingsCenter.GET_PREFS_Video_SyncFramePrefs()
        //写入文字
        when(syncFramePrefs){
            SettingsCenter.SYNC_FRAME_PREFS_AlwaysSync -> BT_SyncFramePrefsText.text = "一律使用关键帧"
            SettingsCenter.SYNC_FRAME_PREFS_AlwaysExact -> BT_SyncFramePrefsText.text = "一律使用精确帧"
            SettingsCenter.SYNC_FRAME_PREFS_Dynamic -> BT_SyncFramePrefsText.text = "动态切换"
            else -> BT_SyncFramePrefsText.text = "错误"
        }
    }
    //视频播放页 连续寻帧间隔 value
    private fun choose_Video_generalSeek_updateMs_Menu(anchor:View) {
        UI_FUNCTION.show_popup_Menu_v1_v1_base_xml(context, anchor, R.menu.popup_menu_video_general_seek_millis) { item ->
            when (item.itemId) {
                R.id.menu_item_120hz -> {
                    vibrate()
                    choose_Video_generalSeek_updateMs_Core(8)
                    true
                }
                R.id.menu_item_60hz -> {
                    vibrate()
                    choose_Video_generalSeek_updateMs_Core(16)
                    true
                }
                R.id.menu_item_30hz -> {
                    vibrate()
                    choose_Video_generalSeek_updateMs_Core(33)
                    true
                }
                R.id.menu_item_15hz -> {
                    vibrate()
                    choose_Video_generalSeek_updateMs_Core(66)
                    true
                }
                R.id.menu_item_Input -> {
                    vibrate()
                    set_video_generalSeek_updateMs_input()
                    true
                }
                else -> true
            }
        }

    }
    private fun choose_Video_generalSeek_updateMs_Core(value: Long) {
        //执行函数
        fun execute(){
            //写入设置
            SettingsCenter.set_value_seekVideo_runnableGapMs(value)
            //刷新显示
            update_video_generalSeek_updateMs_Text()
        }

        //检查数值
        if (value == 0L){
            //
            AlertDialog.Builder(context)
                .setTitle("提示")
                .setMessage("零值是高危数值，可带来设备损坏风险，仅用于测试与观察。正常情况下请勿设置此值为零！如果需要高频率，建议设置为 1 Ms 以使循环压力可控")
                .setPositiveButton("仍然设为零") { dialog, _ ->
                    vibrate()

                    execute()

                    dialog.dismiss()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    vibrate()

                    //取消时检查是否需要重置当前值
                    val current = SettingsCenter.get_value_seekVideo_runnableGapMs()
                    if (current < 8L){
                        choose_Video_generalSeek_updateMs_Core(8L)
                    }

                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()


            return

        }else{
            //执行
            execute()
        }

    }
    private fun set_video_generalSeek_updateMs_input() {
        UI_FUNCTION.show_number_input_Dialog_v1_v2 (
            context = context,
            ui_title = "设置播放器连续寻帧间隔",
            ui_description = "间隔过低时，设备温度和功耗将大幅上升",
            ui_hint = "以毫秒为单位",
            ui_button_text = "确定",
            validate = { inputValue ->
                when {
                    inputValue < 0L -> ValidationResult.Reject("连续寻帧间隔不得为负")
                    inputValue > 1000L -> ValidationResult.Reject("连续寻帧间隔不能大于2秒")
                    else -> ValidationResult.Ok
                }
            },
            onValid = { inputValue ->
                //写入寻帧间隔
                choose_Video_generalSeek_updateMs_Core(inputValue.toLong())
            },
            onReject = { message ->
                showCustomToast(message, 3)
            }
        )

    }
    private fun update_video_generalSeek_updateMs_Text() {
        val BT_SeekLoop_Millis = findViewById<TextView>(R.id.ButtonTextSeekHandlerGap)
        val generalSeek_updateMs = SettingsCenter.get_value_seekVideo_runnableGapMs()
        //重置文字颜色
        BT_SeekLoop_Millis.setTextColor(
            ContextCompat.getColor(context, R.color.Text_Level_2)
        )
        //写入文字
        when(generalSeek_updateMs){
            0L -> {
                BT_SeekLoop_Millis.text = "无间隔 (高危)"
                BT_SeekLoop_Millis.setTextColor(
                    ContextCompat.getColor(context, R.color.RED)
                )
            }
            8L -> BT_SeekLoop_Millis.text = "120 Hz"
            16L -> BT_SeekLoop_Millis.text = "60 Hz"
            12L -> BT_SeekLoop_Millis.text = "90 Hz"
            33L -> BT_SeekLoop_Millis.text = "30 Hz"
            66L -> BT_SeekLoop_Millis.text = "15 Hz"
            else -> BT_SeekLoop_Millis.text = "$generalSeek_updateMs Ms"
        }
    }
    //视频页 时间戳/时间窗口 刷新间隔 value
    private fun choose_Video_timeStamp_updateMs_Menu(anchor:View) {
        UI_FUNCTION.show_popup_Menu_v1_v1_base_xml(context, anchor, R.menu.activity_settings_popup_timer_update_gap) { item ->
            when (item.itemId) {
                R.id.menu_item_120hz -> {
                    vibrate()
                    choose_Video_timeStamp_updateMs_Core(8L)
                    true
                }
                R.id.menu_item_90hz -> {
                    vibrate()
                    choose_Video_timeStamp_updateMs_Core(12L)
                    true
                }
                R.id.menu_item_60hz -> {
                    vibrate()
                    choose_Video_timeStamp_updateMs_Core(16L)
                    true
                }
                R.id.menu_item_30hz -> {
                    vibrate()
                    choose_Video_timeStamp_updateMs_Core(33L)
                    true
                }
                R.id.menu_item_15hz -> {
                    vibrate()
                    choose_Video_timeStamp_updateMs_Core(66L)
                    true
                }
                R.id.menu_item_Input -> {
                    vibrate()
                    set_video_timerStamp_updateMs_input()
                    true
                }
                else -> true
            }
        }
    }
    private fun choose_Video_timeStamp_updateMs_Core(value: Long) {
        fun execute(){
            //写入设置
            SettingsCenter.set_value_timeStamp_updateGapMs(value)
            //刷新显示
            update_video_timerStamp_updateMs_Text()
        }

        if (value < 15L){
            AlertDialog.Builder(context)
                .setTitle("提示")
                .setMessage("此值过低时，若同时寻帧间隔也过低，可能导致界面轻微卡顿。是否继续?")
                .setPositiveButton("确认") { dialog, _ ->
                    vibrate()

                    execute()

                    dialog.dismiss()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    vibrate()

                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        }else{
            execute()
        }

    }
    private fun set_video_timerStamp_updateMs_input() {
        UI_FUNCTION.show_number_input_Dialog_v1_v2 (
            context = context,
            ui_title = "自定义时间戳更新间隔",
            ui_description = "仅控制进度条滚动时的时间戳更新间隔",
            ui_hint = "以毫秒为单位",
            ui_button_text = "确定",
            validate = { inputValue ->
                when {
                    inputValue <= 0L -> ValidationResult.Reject("请设置更高的值")
                    inputValue > 1000L -> ValidationResult.Reject("更新间隔不能大于1秒")
                    else -> ValidationResult.Ok
                }
            },
            onValid = { inputValue ->
                //执行设置
                choose_Video_timeStamp_updateMs_Core(inputValue.toLong())
            },
            onReject = { message ->
                showCustomToast(message, 3)
            }
        )

    }
    private fun update_video_timerStamp_updateMs_Text() {
        val ButtonTextTimerUpdateGap = findViewById<TextView>(R.id.ButtonTextTimerUpdateGap)
        when(val timerUpdateGap = SettingsCenter.get_value_timeStamp_updateGapMs()){
            8L -> ButtonTextTimerUpdateGap.text = "120 Hz"
            12L -> ButtonTextTimerUpdateGap.text = "90 Hz"
            16L -> ButtonTextTimerUpdateGap.text = "60 Hz"
            33L -> ButtonTextTimerUpdateGap.text = "30 Hz"
            66L -> ButtonTextTimerUpdateGap.text = "15 Hz"
            else -> ButtonTextTimerUpdateGap.text = "$timerUpdateGap 毫秒"
        }
    }
    //视频页 SCROLLER 刷新间隔 value
    private fun choose_Video_scroller_updateMs_Menu(anchor:View) {
        UI_FUNCTION.show_popup_Menu_v1_v1_base_xml(context, anchor, R.menu.popup_menu_video_scroller_update_millis) { item ->
            when (item.itemId) {
                R.id.menu_item_30hz -> {
                    vibrate()
                    choose_Video_scroller_updateMs_Core(33L)
                    true
                }
                R.id.menu_item_15hz -> {
                    vibrate()
                    choose_Video_scroller_updateMs_Core(66L)
                    true
                }
                R.id.menu_item_Input -> {
                    vibrate()
                    set_video_scroller_updateMs_input()
                    true
                }
                else -> true
            }
        }

    }
    private fun choose_Video_scroller_updateMs_Core(gap: Long) {
        fun execute(){
            //写入设置
            SettingsCenter.set_value_syncScroller_runnableGapMs(gap)
            //刷新显示
            update_video_scroller_updateMs_Text()
        }

        execute()

    }
    private fun set_video_scroller_updateMs_input() {
        UI_FUNCTION.show_number_input_Dialog_v1_v2 (
            context = context,
            ui_title = "自定义进度条更新间隔",
            ui_description = "仅控制进度条自主滚动时的更新间隔",
            ui_hint = "以毫秒为单位",
            ui_button_text = "确定",
            validate = { inputValue ->
                when {
                    inputValue < 0L -> ValidationResult.Reject("更新间隔不得为负")
                    inputValue < 66L -> ValidationResult.Reject("更新频率不能高于15Hz (66 Ms)")
                    inputValue > 3000L -> ValidationResult.Reject("更新间隔不能大于3秒")
                    else -> ValidationResult.Ok
                }
            },
            onValid = { inputValue ->
                //写入设置
                choose_Video_scroller_updateMs_Core(inputValue.toLong())
            },
            onReject = { message ->
                showCustomToast(message, 3)
            }
        )

    }
    private fun update_video_scroller_updateMs_Text() {
        val ButtonTextScrollerUpdateGap = findViewById<TextView>(R.id.ButtonText_scrollerUpdateGap)
        val scrollerUpdateGap = SettingsCenter.get_value_syncScroller_runnableGapMs()
        when(scrollerUpdateGap){
            0L -> ButtonTextScrollerUpdateGap.text = "120 Hz"
            12L -> ButtonTextScrollerUpdateGap.text = "90 Hz"
            16L -> ButtonTextScrollerUpdateGap.text = "60 Hz"
            33L -> ButtonTextScrollerUpdateGap.text = "30 Hz"
            66L -> ButtonTextScrollerUpdateGap.text = "15 Hz"
            else -> ButtonTextScrollerUpdateGap.text = "$scrollerUpdateGap 毫秒"
        }
    }
    //视频页 SEEK_BAR 刷新间隔 value
    private fun choose_video_syncSeekBar_updateMs_Menu(anchor:View) {
        UI_FUNCTION.show_popup_Menu_v1_v2_base_dynamic_add(anchor, listOf(
            "10 ms"   to { choose_video_syncSeekBar_updateMs_Core(10L) },
            "100 ms"  to { choose_video_syncSeekBar_updateMs_Core(100L) },
            "500 ms"  to { choose_video_syncSeekBar_updateMs_Core(500L) },
            "1000 ms" to { choose_video_syncSeekBar_updateMs_Core(1000L) },
            "手动输入" to { set_video_syncSeekBar_updateMs_input() },
            "自适应"   to { choose_video_syncSeekBar_updateMs_Core(3001L) },
        ))

    }
    private fun choose_video_syncSeekBar_updateMs_Core(gap: Long) {
        fun execute(){
            //写入设置
            SettingsCenter.set_value_syncSeekbar_runnableGapMs(gap)
            //刷新显示
            update_video_syncSeekBar_updateMS_Text()
        }

        execute()

    }
    private fun set_video_syncSeekBar_updateMs_input() {
        UI_FUNCTION.show_number_input_Dialog_v1_v2 (
            context = context,
            ui_title = "设置进度条更新间隔",
            ui_description = "仅控制进度条自主滚动时的更新间隔",
            ui_hint = "以毫秒为单位",
            ui_button_text = "确定",
            validate = { inputValue ->
                when {
                    inputValue < 0L -> ValidationResult.Reject("时间更新间隔不能小于0")
                    inputValue > 3000L -> ValidationResult.Reject("时间更新间隔不能大于3秒")
                    else -> ValidationResult.Ok
                }
            },
            onValid = { inputValue ->
                //写入设置
                choose_video_syncSeekBar_updateMs_Core(inputValue.toLong())
            },
            onReject = { message ->
                showCustomToast(message, 3)
            }
        )
    }
    private fun update_video_syncSeekBar_updateMS_Text() {
        val ButtonTextSeekBarUpdateGap = findViewById<TextView>(R.id.ButtonText_seekbarUpdateGap)
        val seekBarUpdateGap = SettingsCenter.get_value_syncSeekbar_runnableGapMs()
        when(seekBarUpdateGap){
            0L -> ButtonTextSeekBarUpdateGap.text = "无间隔(推荐设为更高值)"
            in 1L..50L -> ButtonTextSeekBarUpdateGap.text = "$seekBarUpdateGap 毫秒 (推荐设为更高值)"
            in 50L..999L -> ButtonTextSeekBarUpdateGap.text = "$seekBarUpdateGap 毫秒"
            in 1000L..3000L -> ButtonTextSeekBarUpdateGap.text = "${seekBarUpdateGap/1000f}秒"
            3001L -> ButtonTextSeekBarUpdateGap.text = "自适应"
            else -> ButtonTextSeekBarUpdateGap.text = "$seekBarUpdateGap 毫秒"
        }
    }
    //</editor-fold>
    //音乐播放页设置
    //<editor-fold desc="////音乐播放页设置函数">
    //音乐页SeekBar刷新间隔
    private fun choose_audio_syncSeekBar_updateMs_Menu(anchor:View) {
        UI_FUNCTION.show_popup_Menu_v1_v1_base_xml(context, anchor, R.menu.popup_menu_video_seekbar_update_millis) { item ->
            when (item.itemId) {
                R.id.menu_item_10ms   -> { choose_audio_syncSeekBar_updateMs_Core(10L);  true }
                R.id.menu_item_100ms  -> { choose_audio_syncSeekBar_updateMs_Core(100L); true }
                R.id.menu_item_500ms  -> { choose_audio_syncSeekBar_updateMs_Core(500L); true }
                R.id.menu_item_1000ms -> { choose_audio_syncSeekBar_updateMs_Core(1000L); true }
                R.id.menu_item_Input  -> { set_audio_syncSeekBar_updateMs_input(); true }
                R.id.menu_item_Adapt  -> { choose_audio_syncSeekBar_updateMs_Core(3001L); true }
                else -> true
            }
        }
    }
    private fun choose_audio_syncSeekBar_updateMs_Core(gap: Long) {
        fun execute(){
            //写入设置
            SettingsCenter.set_value_audio_syncSeekbar_runnableGapMs(gap)
            //刷新显示
            update_audio_syncSeekBar_updateMS_Text()
        }

        execute()

    }
    private fun set_audio_syncSeekBar_updateMs_input() {
        UI_FUNCTION.show_number_input_Dialog_v1_v2 (
            context = context,
            ui_title = "自定义进度条更新间隔",
            ui_description = "仅控制进度条自主滚动时的更新间隔",
            ui_hint = "以毫秒为单位",
            ui_button_text = "确定",
            validate = { inputValue ->
                when {
                    inputValue < 0L -> ValidationResult.Reject("时间更新间隔不能小于0")
                    inputValue > 3000L -> ValidationResult.Reject("时间更新间隔不能大于3秒")
                    else -> ValidationResult.Ok
                }
            },
            onValid = { inputValue ->
                //执行设置写入
                choose_audio_syncSeekBar_updateMs_Core(inputValue.toLong())
            },
            onReject = { message ->
                showCustomToast(message, 3)
            }
        )
    }
    private fun update_audio_syncSeekBar_updateMS_Text() {
        val ButtonText_audioSeekBar_updateMs = findViewById<TextView>(R.id.ButtonText_audioSeekBar_updateMs)
        //获取当前进度条更新间隔
        val value_audioSeekBar_updateMs = SettingsCenter.get_value_audio_syncSeekbar_runnableGapMs()
        //根据选择的进度条更新间隔，更新显示
        when(value_audioSeekBar_updateMs) {
            0L -> ButtonText_audioSeekBar_updateMs.text = "无间隔(推荐设为更高值)"
            in 1L..10L -> ButtonText_audioSeekBar_updateMs.text = "$value_audioSeekBar_updateMs 毫秒 (推荐设为更高值)"
            in 10L..999L -> ButtonText_audioSeekBar_updateMs.text = "$value_audioSeekBar_updateMs 毫秒"
            in 1000L..3000L -> ButtonText_audioSeekBar_updateMs.text = "${value_audioSeekBar_updateMs/1000f}秒"
            3001L -> ButtonText_audioSeekBar_updateMs.text = "自适应"
            else -> ButtonText_audioSeekBar_updateMs.text = "$value_audioSeekBar_updateMs 毫秒"
        }
    }
    //</editor-fold>
    //交互相关设置
    //<editor-fold desc="////交互相关设置函数">
    //振动模式
    private fun chooseVibrateMode_Menu(anchor:View){
        UI_FUNCTION.show_popup_Menu_v1_v1_base_xml(context, anchor, R.menu.popup_menu_interactive_vibrate_mode) { item ->
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
    }
    private fun chooseVibrateMode(mode: Int) {
        //振动模式表
        // 0 = No Vibrate
        // 1 = VibrationEffect.EFFECT_CLICK
        // 2 = VibrationEffect.EFFECT_TICK
        // 3 = VibrationEffect.EFFECT_DOUBLE_CLICK
        // 4 = VibrationEffect.EFFECT_HEAVY_CLICK

        //写入设置
        ToolVibrate.SET_PREFS_VibrateMode(mode)
        //振动一次展示效果
        vibrate()
        //刷新显示
        updateVibrateModeText()

    }
    private fun updateVibrateModeText() {
        val ButtonTextVibrateMode = findViewById<TextView>(R.id.ButtonTextVibrateMode)
        //获取当前振动模式
        val vibrateMode = ToolVibrate.GET_PREFS_VibrateMode()
        //根据选择的振动模式，更新显示
        when(vibrateMode) {
            0 -> ButtonTextVibrateMode.text = "无振动"
            1 -> ButtonTextVibrateMode.text = "EFFECT_CLICK"
            2 -> ButtonTextVibrateMode.text = "EFFECT_TICK"
            3 -> ButtonTextVibrateMode.text = "EFFECT_DOUBLE_CLICK"
            4 -> ButtonTextVibrateMode.text = "EFFECT_HEAVY_CLICK"
            5 -> ButtonTextVibrateMode.text = "100Ms 默认振动 (OPPO专用)"
        }
    }
    //MiniView底部抬高高度设置
    private fun setMiniViewBottomPadding() {
        //读取当前高度
        val current_height_Dp = SettingsCenter.get_value_MiniView_BottomPadding_Dp()
        //显示弹窗
        UI_FUNCTION.show_number_input_Dialog_v1_v2 (
            context = context,
            ui_title = "设置MiniView底部抬高高度",
            ui_description = "应设置在 0 Dp 到 50 Dp 之间",
            ui_hint = "当前为 $current_height_Dp Dp",
            ui_button_text = "确定",
            validate = { v ->
                when {
                    v < 0 -> ValidationResult.Reject("高度不能小于0")
                    v > 50 -> ValidationResult.Reject("高度不能大于 50 Dp")
                    else -> ValidationResult.Ok
                }
            },
            onValid = { inputValue ->
                //执行设置写入
                SettingsCenter.set_value_MiniView_BottomPadding_Dp(inputValue)
                //发布设置高度消息
                EventBus_Embedded.emit(AppEvent(
                    event = AppEventList.app_event_c_miniview_padding_updated,
                    eventSlotInt = inputValue,
                ))
            },
            onReject = { message ->
                showCustomToast(message, 3)
            }
        )

    }
    //</editor-fold>

    //删除缩略图缓存
    private fun chooseDeleteFrameItem(anchor: View) {
        UI_FUNCTION.show_popup_Menu_v1_v1_base_xml(context, anchor, R.menu.popup_menu_setting_delete_frame) { item ->
            when (item.itemId) {
                R.id.delete_all -> {
                    vibrate()

                    deleteArtworkFrameCache(
                        context = context,
                        deleteVideo = true,
                        deleteAudio = true
                    )

                    true
                }
                R.id.delete_video -> {
                    vibrate()

                    deleteArtworkFrameCache(
                        context = context,
                        deleteVideo = true,
                        deleteAudio = false
                    )

                    true
                }
                R.id.delete_audio -> {
                    vibrate()

                    deleteArtworkFrameCache(
                        context = context,
                        deleteVideo = false,
                        deleteAudio = true
                    )

                    true
                }
                R.id.delete_video_custom -> {
                    vibrate()

                    deleteCustomFrameCache(
                        context = context,
                        deleteVideo = true,
                        deleteAudio = false
                    )

                    true
                }
                R.id.delete_audio_custom -> {
                    vibrate()

                    deleteCustomFrameCache(
                        context = context,
                        deleteVideo = false,
                        deleteAudio = true
                    )

                    true
                }
                else -> true
            }
        }
    }
    private fun deleteArtworkFrameCache(context: Context, deleteVideo:Boolean = false, deleteAudio:Boolean = false) {
        AlertDialog.Builder(context)
            .setTitle("确定删除选中的默认封面吗?")
            .setMessage("仅删除自动生成的封面，保留自定义封面")
            .setPositiveButton("确认") { dialog, _ ->
                vibrate()


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
                vibrate()

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
    private fun deleteCustomFrameCache(context: Context, deleteVideo:Boolean = false, deleteAudio:Boolean = false) {
        AlertDialog.Builder(context)
            .setTitle("确定删除选中的自定义封面吗?")
            .setMessage("更建议您在播放页删除单个媒体的自定义封面")
            .setPositiveButton("确认") { dialog, _ ->
                vibrate()

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
                vibrate()

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
    //删除媒体数据缓存
    private fun chooseDeleteDB(anchor: View) {
        UI_FUNCTION.show_popup_Menu_v1_v1_base_xml(context, anchor, R.menu.popup_menu_setting_delete_db) { item ->
            when (item.itemId) {
                R.id.delete_video -> {
                    vibrate()

                    deleteDB(
                        context = context,
                        deleteVideo = true,
                        deleteAudio = false
                    )

                    true
                }
                R.id.delete_audio -> {
                    vibrate()

                    deleteDB(
                        context = context,
                        deleteVideo = false,
                        deleteAudio = true
                    )

                    true
                }
                else -> true
            }
        }
    }
    private fun deleteDB(context: Context, deleteVideo:Boolean = false, deleteAudio:Boolean = false) {
        AlertDialog.Builder(context)
            .setTitle(if (deleteVideo )"确定删除视频数据缓存吗?" else "确定删除音频数据缓存吗?")
            .setMessage("回到主页后会触发再次读取,仅作为清除异常数据使用")
            .setPositiveButton("确认") { dialog, _ ->
                vibrate()

                lifecycleScope.launch(Dispatchers.IO){
                    deleteDB_Core(context,deleteVideo,deleteAudio)
                }

                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                vibrate()

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
    suspend fun deleteDB_Core(context: Context, deleteVideo:Boolean = false, deleteAudio:Boolean = false) {
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

    //取消隐私政策同意
    private fun revokePrivacyAgreementAlert(){
        AlertDialog.Builder(context)
            .setTitle("确定撤回同意吗?")
            .setMessage("若确认，App将自动退出")
            .setPositiveButton("确认") { dialog, _ ->
                vibrate()

                revokePrivacyAgreementCore()

                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                vibrate()

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()

    }
    private fun revokePrivacyAgreementCore(){
        //关闭正在播放的媒体
        PlayerSingleton.clearMediaItem()
        PlayerSingleton.stopPlayEngineBundle()

        //清除媒体记录
        val MediaRecordManager = MediaRecordManager()
        MediaRecordManager.clear_MediaInfo(context)

        //写入隐私政策同意状态
        val PrivacyPermissionHelper = PrivacyPermissionHelper()
        PrivacyPermissionHelper.setPrivacyAgreed(context,false)

        //延时自动退出
        Handler(Looper.getMainLooper()).postDelayed({
            //退出
            finishAffinity()
            //结束进程
            exitProcess(0)
        }, 500)
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
                    //showCustomToast("检查更新成功: 最新发布版本为 ${latestRelease.version}, 下载链接: ${latestRelease.downloadUrl}")
                    //询问是否下载
                    AlertDialog.Builder(context)
                        .setTitle("发现新版本 ${latestRelease.version}")
                        .setMessage("是否下载最新版本? (将下载至Download文件夹)")
                        .setPositiveButton("确定") { _, _ ->
                            //展开下载进度区域
                            expandDownloadProgress()
                            //下载最新版本
                            downloadLatestVersion(latestRelease)
                        }
                        .setNegativeButton("取消") { dialog, _ ->

                            dialog.dismiss()
                        }
                        .show()
                }
            }else{
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
    //展开下载进度区域
    private fun expandDownloadProgress(){

        //将区域高度设为0并使用动画展开到当前高度
        progressLayout.expand()

    }
    fun View.expand(duration: Long = 300) {
        //先设置为可见
        visibility = View.VISIBLE

        //测量目标高度
        measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = measuredHeight

        //如果还没测量好，延迟执行
        if (targetHeight == 0) {
            post { expand(duration) }
            return
        }

        //设置初始高度为0
        layoutParams.height = 0
        requestLayout()

        //执行动画
        ValueAnimator.ofInt(0, targetHeight).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener {
                layoutParams.height = it.animatedValue as Int
                requestLayout()
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    requestLayout()
                }
            })
        }.start()
    }
    //下载最新版本
    private lateinit var progressLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private var downloadJob: Job? = null
    private fun downloadLatestVersion(releaseInfo: ReleaseInfo){
        val downloadUrl = releaseInfo.downloadUrl
        val version = releaseInfo.version
        //consoleLog("下载最新版本：$version  $downloadUrl")

        downloadJob = lifecycleScope.launch(Dispatchers.Main) {
            try {

                //更新进度条
                val result = DownloadManager.downloadApk(context = context, url = downloadUrl,version = version){ progress ->

                    val percent = (progress * 100).toInt()
                    progressBar.progress = percent
                    progressText.text = "下载中：$percent%"

                }

                //下载结果
                result.onSuccess { file ->
                    progressText.text = "下载完成"

                    delay(500.milliseconds)

                    //发起安装
                    installApk(file)

                }.onFailure { error ->
                    progressText.text = "下载失败"
                    consoleLog("下载失败：${error.message}")

                    //显示错误对话框
                    AlertDialog.Builder(context)
                        .setTitle("下载失败")
                        .setMessage("请检查网络后重试")
                        .setPositiveButton("重试") { _, _ ->
                            downloadLatestVersion(releaseInfo)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }

            }catch(e: Exception){
                consoleLog("下载异常:${e.message}")
                progressText.text = "下载异常"
            }
        }
    }
    //发起安装Apk
    private fun installApk(file: File) {
        if (!file.exists()){
            showCustomToast("文件竟然不见了")
            consoleLog("installApk -文件不存在 -路径:${file.absolutePath}")
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${packageName}.fileprovider",
            file
        )


        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            consoleLog("启动安装失败：${e.message}")
            // 如果 FileProvider 配置有问题，降级到旧方式
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(fallbackIntent)
        }
    }






    //顶栏效果
    private var isTopBarTitleVisible = true
    private fun topBarEffect_Out_Title() {
        if (!isTopBarTitleVisible) return
        isTopBarTitleVisible = false

        AppBarTitle.animate()
            .alpha(0f)
            .setDuration(animDuration)
            .withEndAction { AppBarTitle.visibility = View.GONE }
            .start()

    }
    private fun topBarEffect_In_Title() {
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
    private fun topBarEffect_Out_Brush() {
        if (!isTopBarBrushVisible) return
        isTopBarBrushVisible = false

        AppBarGradientMask.alpha = 1f
        AppBarGradientMask.animate()
            .alpha(0f)
            .setDuration(animDuration)
            .withEndAction { AppBarGradientMask.visibility = View.GONE }
            .start()
    }
    private fun topBarEffect_In_Brush() {
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
    private fun setupScrollContentListener() {
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
    private fun display() {
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        //初始化顶部栏
        AppBarGradientMask = findViewById(R.id.AppBarGradientMask)
        AppBarCore = findViewById(R.id.AppBarCore)
        AppBarSpacer = findViewById(R.id.AppBarSpacer)
        AppBarTitle = findViewById(R.id.AppBarTitle)
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
    private fun onStatusBarHeightGet(statusBarHeight: Int) {
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



    //设置变更锁
    private var set_switch_from_process = false
    private fun cancel(): Boolean{
        //确保状态清空
        set_switch_from_process = false

        return true
    }


    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "SettingsActivity: $msg")
        }
    }

}