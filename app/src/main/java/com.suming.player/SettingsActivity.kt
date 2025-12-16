package com.suming.player

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.AudioFocusRequest
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import data.DataBaseMediaItem.MediaItemDataBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class SettingsActivity: AppCompatActivity() {
    //开关初始化
    //<editor-fold desc="开关初始化">
    private lateinit var Switch_ReadNewOnEachStart: SwitchCompat
    private lateinit var Switch_CloseVideoTrack: SwitchCompat
    private lateinit var Switch_SwitchPortraitWhenExit: SwitchCompat
    private lateinit var Switch_KeepPlayingWhenExit: SwitchCompat
    private lateinit var Switch_DisableSmallPlayer: SwitchCompat
    private lateinit var Switch_UseDataBaseForScrollerSetting: SwitchCompat
    private lateinit var Switch_ExitWhenEnd: SwitchCompat
    private lateinit var Switch_UseLongScroller: SwitchCompat
    private lateinit var Switch_UseLongSeekGap: SwitchCompat
    private lateinit var Switch_UseCompatScroller: SwitchCompat
    private lateinit var Switch_GenerateThumbSync: SwitchCompat
    private lateinit var Switch_UseOnlySyncFrame: SwitchCompat
    private lateinit var Switch_UseBlackBackground: SwitchCompat
    private lateinit var Switch_UseHighRefreshRate: SwitchCompat
    private lateinit var Switch_CloseFragmentGesture: SwitchCompat
    private lateinit var Switch_EnablePlayAreaMove: SwitchCompat
    private lateinit var Switch_UsePlayerWithSeekBar: SwitchCompat
    private lateinit var Switch_UseTestingPlayer: SwitchCompat
    private lateinit var Switch_UseSyncFrameWhenScrollerStop: SwitchCompat
    //</editor-fold>
    //开关变量数值量
    //<editor-fold desc="开关变量数值量">
    private var PREFS_ReadNewOnEachStart = false
    private var PREFS_CloseVideoTrack = false
    private var PREFS_SwitchPortraitWhenExit = true
    private var PREFS_KeepPlayingWhenExit = false
    private var PREFS_DisableSmallPlayer = false
    private var PREFS_UseDataBaseForScrollerSetting = false
    private var PREFS_ExitWhenEnd = false
    private var PREFS_UseLongScroller = false
    private var PREFS_UseLongSeekGap = false
    private var PREFS_UseCompatScroller = false
    private var PREFS_GenerateThumbSYNC = true
    private var PREFS_UseOnlySyncFrame = false
    private var PREFS_TimeUpdateGap = 16L
    private var PREFS_UseBlackBackground = false
    private var PREFS_UseHighRefreshRate = false
    private var PREFS_SeekHandlerGap = 0L
    private var PREFS_CloseFragmentGesture = false
    private var PREFS_EnablePlayAreaMove = false
    private var PREFS_UsePlayerWithSeekBar = false
    private var PREFS_UseTestingPlayer = false
    private var PREFS_UseSyncFrameWhenScrollerStop = false
    //</editor-fold>
    //按钮循环：清除所有视频缓存
    private var ButtonRemoveAllThumbPathIndex = 0
    //设置清单
    private lateinit var PREFS: SharedPreferences
    private lateinit var PREFS_Editor: SharedPreferences.Editor
    private lateinit var PREFS_MediaStore: SharedPreferences
    //震动时间
    private var PREFS_VibrateMillis = 0L
    private var PREFS_UseSysVibrate = false

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded", "UseKtx")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_settings)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //显示版本
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        val versionText = findViewById<TextView>(R.id.version)
        versionText.text = "版本: $version"
        //按钮：返回
        val ButtonBack = findViewById<ImageButton>(R.id.buttonExit)
        ButtonBack.setOnClickListener {
            ToolVibrate().vibrate(this)
            finish()
        }
        //按钮：前往项目Github仓库页
        val ButtonGoGithub = findViewById<TextView>(R.id.buttonGoGithubRelease)
        ButtonGoGithub.setOnClickListener {
            ToolVibrate().vibrate(this)

            val url = "https://github.com/JeanValjean07/Player/releases"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }
        //超链接：开放源代码许可
        val openSourceLicense = findViewById<TextView>(R.id.openSourceLicense)
        openSourceLicense.paint.isUnderlineText = true
        openSourceLicense.setOnClickListener {
            ToolVibrate().vibrate(this)
            val isMicroG_Exist = checkMicroG()
            if (packageNumber == 1){
                showCustomToast("无法读取应用列表,拒绝打开此页面",Toast.LENGTH_SHORT,3)
                return@setOnClickListener
            }
            if (isMicroG_Exist){
                showCustomToast("已安装MicroG服务的设备不支持打开此页",Toast.LENGTH_SHORT,3)
            }
            else{
                startActivity(Intent(this,
                    com.google.android.gms.oss.licenses.OssLicensesMenuActivity::class.java
                ))
            }
        }
        //超链接：设备信息
        val DeviceInfoPage = findViewById<TextView>(R.id.DeviceInfoPage)
        DeviceInfoPage.paint.isUnderlineText = true
        DeviceInfoPage.setOnClickListener {
            ToolVibrate().vibrate(this)
            startActivity(Intent(this, DeviceInfoActivity::class.java))
        }

        //静态操作部分:::
        //读取设置
        PREFS = getSharedPreferences("PREFS", MODE_PRIVATE)
        PREFS_Editor = PREFS.edit()
        if (!PREFS.contains("PREFS_CloseVideoTrack")) {
            PREFS_Editor.putBoolean("PREFS_CloseVideoTrack", true)
            PREFS_CloseVideoTrack = true
        } else {
            PREFS_CloseVideoTrack = PREFS.getBoolean("PREFS_CloseVideoTrack", false)
        }
        if (!PREFS.contains("PREFS_SwitchPortraitWhenExit")) {
            PREFS_Editor.putBoolean("PREFS_SwitchPortraitWhenExit", false)
            PREFS_SwitchPortraitWhenExit = false
        } else {
            PREFS_SwitchPortraitWhenExit = PREFS.getBoolean("PREFS_SwitchPortraitWhenExit", false)
        }
        if (PREFS.contains("PREFS_KeepPlayingWhenExit")) {
            PREFS_KeepPlayingWhenExit = PREFS.getBoolean("PREFS_KeepPlayingWhenExit", true)
        } else {
            PREFS_Editor.putBoolean("PREFS_KeepPlayingWhenExit", true)
            PREFS_KeepPlayingWhenExit = true
        }
        if (!PREFS.contains("PREFS_SeekHandlerGap")) {
            PREFS_Editor.putLong("PREFS_SeekHandlerGap", 0L)
            PREFS_SeekHandlerGap = 0L
        } else {
            PREFS_SeekHandlerGap = PREFS.getLong("PREFS_SeekHandlerGap", 0L)
        }
        if (PREFS.contains("PREFS_DisableSmallPlayer")){
            PREFS_DisableSmallPlayer = PREFS.getBoolean("PREFS_DisableSmallPlayer", false)
        }else{
            PREFS.edit { putBoolean("PREFS_DisableSmallPlayer", false).apply() }
        }
        if (!PREFS.contains("PREFS_UseDataBaseForScrollerSetting")) {
            PREFS_Editor.putBoolean("PREFS_UseDataBaseForScrollerSetting", false)
            PREFS_UseDataBaseForScrollerSetting = false
        } else {
            PREFS_UseDataBaseForScrollerSetting = PREFS.getBoolean("PREFS_UseDataBaseForScrollerSetting", false)
        }
        if (!PREFS.contains("PREFS_ExitWhenEnd")) {
            PREFS_Editor.putBoolean("PREFS_ExitWhenEnd", false)
            PREFS_ExitWhenEnd = false
        } else {
            PREFS_ExitWhenEnd = PREFS.getBoolean("PREFS_ExitWhenEnd", false)
        }
        if (!PREFS.contains("PREFS_UseLongScroller")) {
            PREFS_Editor.putBoolean("PREFS_UseLongScroller", false)
            PREFS_UseLongScroller = false
        } else {
            PREFS_UseLongScroller = PREFS.getBoolean("PREFS_UseLongScroller", false)
        }
        if (!PREFS.contains("PREFS_UseLongSeekGap")) {
            PREFS_Editor.putBoolean("PREFS_UseLongSeekGap", false)
            PREFS_UseLongSeekGap = false
        } else {
            PREFS_UseLongSeekGap = PREFS.getBoolean("PREFS_UseLongSeekGap", false)
        }
        if (!PREFS.contains("PREFS_UseCompatScroller")) {
            PREFS_Editor.putBoolean("PREFS_UseCompatScroller", false)
            PREFS_UseCompatScroller = false
        } else {
            PREFS_UseCompatScroller = PREFS.getBoolean("PREFS_UseCompatScroller", false)
        }
        if (!PREFS.contains("PREFS_GenerateThumbSYNC")) {
            PREFS_Editor.putBoolean("PREFS_GenerateThumbSYNC", true)
            PREFS_GenerateThumbSYNC = true
        } else {
            PREFS_GenerateThumbSYNC = PREFS.getBoolean("PREFS_GenerateThumbSYNC", true)
        }
        if (!PREFS.contains("PREFS_UseOnlySyncFrame")) {
            PREFS_Editor.putBoolean("PREFS_UseOnlySyncFrame", true)
            PREFS_UseOnlySyncFrame = true
        } else {
            PREFS_UseOnlySyncFrame = PREFS.getBoolean("PREFS_UseOnlySyncFrame", true)
        }
        if (!PREFS.contains("PREFS_TimeUpdateGap")) {
            PREFS_Editor.putLong("PREFS_TimeUpdateGap", 16L)
            PREFS_TimeUpdateGap = 16L
        } else {
            PREFS_TimeUpdateGap = PREFS.getLong("PREFS_TimeUpdateGap", 16L)
        }
        if (!PREFS.contains("PREFS_UseBlackBackground")) {
            PREFS_Editor.putBoolean("PREFS_UseBlackBackground", false)
            PREFS_UseBlackBackground = false
        } else {
            PREFS_UseBlackBackground = PREFS.getBoolean("PREFS_UseBlackBackground", false)
        }
        if (!PREFS.contains("PREFS_UseHighRefreshRate")) {
            PREFS_Editor.putBoolean("PREFS_UseHighRefreshRate", false)
            PREFS_UseHighRefreshRate = false
        } else {
            PREFS_UseHighRefreshRate = PREFS.getBoolean("PREFS_UseHighRefreshRate", false)
        }
        if (!PREFS.contains("PREFS_CloseFragmentGesture")) {
            PREFS_Editor.putBoolean("PREFS_CloseFragmentGesture", false)
            PREFS_CloseFragmentGesture = false
        } else {
            PREFS_CloseFragmentGesture = PREFS.getBoolean("PREFS_CloseFragmentGesture", false)
        }
        if (!PREFS.contains("PREFS_EnablePlayAreaMove")){
            PREFS_Editor.putBoolean("PREFS_EnablePlayAreaMove", true)
            PREFS_EnablePlayAreaMove = true
        }else{
            PREFS_EnablePlayAreaMove = PREFS.getBoolean("PREFS_EnablePlayAreaMove", true)
        }
        if (!PREFS.contains("PREFS_TimeUpdateGap")) {
            PREFS_Editor.putLong("PREFS_TimeUpdateGap", 66L)
            PREFS_TimeUpdateGap = 66L
        } else {
            PREFS_TimeUpdateGap = PREFS.getLong("PREFS_TimeUpdateGap", 66L)
        }
        if (!PREFS.contains("PREFS_VibrateMillis")) {
            PREFS_Editor.putLong("PREFS_VibrateMillis", 50L)
            PREFS_VibrateMillis = 50L
        } else {
            PREFS_VibrateMillis = PREFS.getLong("PREFS_VibrateMillis", 50L)
        }
        if (!PREFS.contains("PREFS_UseSysVibrate")) {
            PREFS_Editor.putBoolean("PREFS_UseSysVibrate", true)
            PREFS_UseSysVibrate = true
        } else {
            PREFS_UseSysVibrate = PREFS.getBoolean("PREFS_UseSysVibrate", true)
        }
        if (!PREFS.contains("PREFS_UsePlayerWithSeekBar")){
            PREFS_Editor.putBoolean("PREFS_UsePlayerWithSeekBar", false)
            PREFS_UsePlayerWithSeekBar = false
        } else {
            PREFS_UsePlayerWithSeekBar = PREFS.getBoolean("PREFS_UsePlayerWithSeekBar", false)
        }
        if (!PREFS.contains("PREFS_UseTestingPlayer")){
            PREFS_Editor.putBoolean("PREFS_UseTestingPlayer", false)
            PREFS_UseTestingPlayer = false
        } else {
            PREFS_UseTestingPlayer = PREFS.getBoolean("PREFS_UseTestingPlayer", false)
        }
        if (!PREFS.contains("PREFS_UseSyncFrameWhenScrollerStop")){
            PREFS_Editor.putBoolean("PREFS_UseSyncFrameWhenScrollerStop", false)
            PREFS_UseSyncFrameWhenScrollerStop = false
        } else {
            PREFS_UseSyncFrameWhenScrollerStop = PREFS.getBoolean("PREFS_UseSyncFrameWhenScrollerStop", false)
        }
        PREFS_Editor.apply()
        PREFS_MediaStore = getSharedPreferences("PREFS_MediaStore", MODE_PRIVATE)
        if (PREFS_MediaStore.contains("PREFS_ReadNewOnEachStart")){
            PREFS_ReadNewOnEachStart = PREFS_MediaStore.getBoolean("PREFS_ReadNewOnEachStart", false)
        }else{
            PREFS_MediaStore.edit { putBoolean("PREFS_ReadNewOnEachStart", false).apply() }
            PREFS_ReadNewOnEachStart = false
        }


        //开关初始化
        Switch_ReadNewOnEachStart = findViewById(R.id.ReadNewOnEachStart)
        Switch_CloseVideoTrack = findViewById(R.id.closeVideoTrack)
        Switch_SwitchPortraitWhenExit = findViewById(R.id.SwitchPortraitWhenExit)
        Switch_KeepPlayingWhenExit = findViewById(R.id.SwitchKeepPlayingWhenExit)
        Switch_DisableSmallPlayer = findViewById(R.id.SwitchDisableSmallPlayer)
        Switch_UseDataBaseForScrollerSetting = findViewById(R.id.UseDataBaseForScrollerSetting)
        Switch_ExitWhenEnd = findViewById(R.id.exitWhenEnd)
        Switch_UseLongScroller = findViewById(R.id.useLongScroller)
        Switch_UseLongSeekGap = findViewById(R.id.useLongSeekGap)
        Switch_UseCompatScroller = findViewById(R.id.useCompatScroller)
        Switch_GenerateThumbSync = findViewById(R.id.generateThumbSYNC)
        Switch_UseOnlySyncFrame = findViewById(R.id.UseOnlySyncFrame)
        Switch_UseBlackBackground = findViewById(R.id.useBlackBackground)
        Switch_UseHighRefreshRate = findViewById(R.id.useHighRefreshRate)
        Switch_CloseFragmentGesture = findViewById(R.id.closeFragmentGesture)
        Switch_EnablePlayAreaMove = findViewById(R.id.EnablePlayAreaMove)
        Switch_UsePlayerWithSeekBar = findViewById(R.id.UsePlayerWithSeekBar)
        Switch_UseTestingPlayer = findViewById(R.id.UseTestingPlayer)
        Switch_UseSyncFrameWhenScrollerStop = findViewById(R.id.UseSyncFrameWhenScrollerStop)
        //开关预置位
        Switch_ReadNewOnEachStart.isChecked = PREFS_ReadNewOnEachStart
        Switch_CloseVideoTrack.isChecked = PREFS_CloseVideoTrack
        Switch_SwitchPortraitWhenExit.isChecked = PREFS_SwitchPortraitWhenExit
        Switch_KeepPlayingWhenExit.isChecked = PREFS_KeepPlayingWhenExit
        Switch_DisableSmallPlayer.isChecked = PREFS_DisableSmallPlayer
        Switch_UseDataBaseForScrollerSetting.isChecked = PREFS_UseDataBaseForScrollerSetting
        Switch_ExitWhenEnd.isChecked = PREFS_ExitWhenEnd
        Switch_UseLongScroller.isChecked = PREFS_UseLongScroller
        Switch_UseLongSeekGap.isChecked = PREFS_UseLongSeekGap
        Switch_UseCompatScroller.isChecked = PREFS_UseCompatScroller
        Switch_GenerateThumbSync.isChecked = PREFS_GenerateThumbSYNC
        Switch_UseOnlySyncFrame.isChecked = PREFS_UseOnlySyncFrame
        Switch_UseBlackBackground.isChecked = PREFS_UseBlackBackground
        Switch_UseHighRefreshRate.isChecked = PREFS_UseHighRefreshRate
        Switch_CloseFragmentGesture.isChecked = PREFS_CloseFragmentGesture
        Switch_EnablePlayAreaMove.isChecked = PREFS_EnablePlayAreaMove
        Switch_UsePlayerWithSeekBar.isChecked = PREFS_UsePlayerWithSeekBar
        Switch_UseTestingPlayer.isChecked = PREFS_UseTestingPlayer
        Switch_UseSyncFrameWhenScrollerStop.isChecked = PREFS_UseSyncFrameWhenScrollerStop


        //文本信息预写
        val currentSeekHandlerGap = findViewById<TextView>(R.id.currentSeekHandlerGap)
        if (PREFS_SeekHandlerGap == 0L) {
            currentSeekHandlerGap.text = "默认 (无寻帧间隔)"
        } else {
            currentSeekHandlerGap.text = "$PREFS_SeekHandlerGap 毫秒"
        }
        val currentTimeUpdateGap = findViewById<TextView>(R.id.currentTimeUpdateGap)
        if (PREFS_TimeUpdateGap == 66L) {
            currentTimeUpdateGap.text = "默认 (66毫秒丨15Hz)"
        } else {
            currentTimeUpdateGap.text = "$PREFS_TimeUpdateGap 毫秒"
        }
        val currentVibrateMillis = findViewById<TextView>(R.id.currentVibratorMillis)
        if (PREFS_UseSysVibrate) {
            currentVibrateMillis.text = "跟随系统"
        } else {
            if (PREFS_VibrateMillis == 0L) {
                currentVibrateMillis.text = "关闭"
            }else{
                currentVibrateMillis.text = "$PREFS_VibrateMillis 毫秒"
            }
        }


        //动态操作部分:::
        //开关更改操作
        Switch_ReadNewOnEachStart.setOnCheckedChangeListener { _, isChecked ->
            PREFS_ReadNewOnEachStart = isChecked
            ToolVibrate().vibrate(this)
            PREFS_MediaStore.edit { putBoolean("PREFS_ReadNewOnEachStart", isChecked).apply() }
        }
        Switch_CloseVideoTrack.setOnCheckedChangeListener { _, isChecked ->
            PREFS_CloseVideoTrack = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_CloseVideoTrack", isChecked).apply()
        }
        Switch_SwitchPortraitWhenExit.setOnCheckedChangeListener { _, isChecked ->
            PREFS_SwitchPortraitWhenExit = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_SwitchPortraitWhenExit", isChecked).apply()
        }
        Switch_KeepPlayingWhenExit.setOnCheckedChangeListener { _, isChecked ->
            PREFS_KeepPlayingWhenExit = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_KeepPlayingWhenExit", isChecked).apply()
        }
        Switch_DisableSmallPlayer.setOnCheckedChangeListener { _, isChecked ->
            PREFS_DisableSmallPlayer = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_DisableSmallPlayer", isChecked).apply()
        }
        Switch_UseDataBaseForScrollerSetting.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseDataBaseForScrollerSetting = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_UseDataBaseForScrollerSetting", isChecked).apply()
        }
        Switch_ExitWhenEnd.setOnCheckedChangeListener { _, isChecked ->
            PREFS_ExitWhenEnd = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_ExitWhenEnd", isChecked).apply()
        }
        Switch_UseLongScroller.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseLongScroller = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_UseLongScroller", isChecked).apply()
        }
        Switch_UseLongSeekGap.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseLongSeekGap = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_UseLongSeekGap", isChecked).apply()
        }
        Switch_UseCompatScroller.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseCompatScroller = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_UseCompatScroller", isChecked).apply()
        }
        Switch_GenerateThumbSync.setOnCheckedChangeListener { _, isChecked ->
            PREFS_GenerateThumbSYNC = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_GenerateThumbSYNC", isChecked).apply()
        }
        Switch_UseOnlySyncFrame.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseOnlySyncFrame = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_UseOnlySyncFrame", isChecked).apply()
        }
        Switch_UseBlackBackground.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseBlackBackground = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_UseBlackBackground", isChecked).apply()
        }
        Switch_UseHighRefreshRate.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseHighRefreshRate = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_UseHighRefreshRate", isChecked).apply()
        }
        Switch_CloseFragmentGesture.setOnCheckedChangeListener { _, isChecked ->
            PREFS_CloseFragmentGesture = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_CloseFragmentGesture", isChecked).apply()
        }
        Switch_EnablePlayAreaMove.setOnCheckedChangeListener { _, isChecked ->
            PREFS_EnablePlayAreaMove = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_EnablePlayAreaMove", isChecked).apply()
        }
        Switch_UsePlayerWithSeekBar.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UsePlayerWithSeekBar = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_UsePlayerWithSeekBar", isChecked).apply()
            if (PREFS_UseTestingPlayer){
                showCustomToast("使用测试版页面已开始,此选项不会生效", Toast.LENGTH_SHORT,3)
            }
        }
        Switch_UseTestingPlayer.setOnCheckedChangeListener { _, isChecked ->
            ToolVibrate().vibrate(this)
            PREFS_UseTestingPlayer = isChecked
            check_PREFS_UseTestingPlayer()
        }
        Switch_UseSyncFrameWhenScrollerStop.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseSyncFrameWhenScrollerStop = isChecked
            ToolVibrate().vibrate(this)
            PREFS_Editor.putBoolean("PREFS_UseSyncFrameWhenScrollerStop", isChecked).apply()
        }



        //seek间隔
        val ButtonSeekHandlerGap = findViewById<TextView>(R.id.ButtonSeekHandlerGap)
        ButtonSeekHandlerGap.setOnClickListener { item ->
            ToolVibrate().vibrate(this)
            val popup = PopupMenu(this, ButtonSeekHandlerGap)
            popup.menuInflater.inflate(R.menu.activity_settings_popup_seek_gap, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.MenuAction_NoGap -> {
                        chooseSeekHandlerGap(0); true
                    }

                    R.id.MenuAction_50 -> {
                        chooseSeekHandlerGap(50); true
                    }

                    R.id.MenuAction_100 -> {
                        chooseSeekHandlerGap(100); true
                    }

                    R.id.MenuAction_200 -> {
                        chooseSeekHandlerGap(200); true
                    }

                    R.id.MenuAction_Input -> {
                        setSeekHandlerGap(); true
                    }

                    else -> true
                }
            }
            popup.show()
        }
        //时间更新间隔
        val ButtonTimeUpdateGap = findViewById<TextView>(R.id.ButtonTimeUpdateGap)
        ButtonTimeUpdateGap.setOnClickListener { item ->
            ToolVibrate().vibrate(this)
            val popup = PopupMenu(this, ButtonTimeUpdateGap)
            popup.menuInflater.inflate(R.menu.activity_settings_popup_time_update_gap, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.MenuAction_8 -> {
                        chooseTimeUpdateGap(8L); true
                    }

                    R.id.MenuAction_12 -> {
                        chooseTimeUpdateGap(12L); true
                    }

                    R.id.MenuAction_16 -> {
                        chooseTimeUpdateGap(16L); true
                    }

                    R.id.MenuAction_33 -> {
                        chooseTimeUpdateGap(33L); true
                    }

                    R.id.MenuAction_66 -> {
                        chooseTimeUpdateGap(66L); true
                    }

                    R.id.MenuAction_Input -> {
                        setTimeUpdateGap(); true
                    }

                    else -> true
                }
            }
            popup.show()
        }
        //振动时长
        val ButtonVibratorMillis = findViewById<TextView>(R.id.ButtonVibratorMillis)
        ButtonVibratorMillis.setOnClickListener { item ->
            ToolVibrate().vibrate(this)
            val popup = PopupMenu(this, ButtonVibratorMillis)
            popup.menuInflater.inflate(R.menu.activity_settings_popup_vibrate_millis, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.MenuAction_NoMillis -> {
                        chooseVibrateMillis(0L); true
                    }
                    R.id.MenuAction_UseSys -> {
                        chooseVibrateMillis(-1L); true
                    }
                    R.id.MenuAction_20 -> {
                        chooseVibrateMillis(20L); true
                    }
                    R.id.MenuAction_50 -> {
                        chooseVibrateMillis(50L); true
                    }
                    R.id.MenuAction_100 -> {
                        chooseVibrateMillis(100L); true
                    }
                    R.id.MenuAction_Input -> {
                        setVibrateMillis(); true
                    }
                    else -> true
                }
            }
            popup.show()
        }

        //重新生成封面
        val ButtonRemoveAllThumbPath = findViewById<TextView>(R.id.RemoveAllThumbPath)
        ButtonRemoveAllThumbPath.setOnClickListener { item ->
            ToolVibrate().vibrate(this)
            if (ButtonRemoveAllThumbPathIndex == 0) {
                ButtonRemoveAllThumbPathIndex = 1
                ButtonRemoveAllThumbPath.text = "请再次点击确认重新生成"
            }
            else if (ButtonRemoveAllThumbPathIndex == 1) {
                ButtonRemoveAllThumbPathIndex = 0
                ButtonRemoveAllThumbPath.text = "已确认重新生成"

                File(filesDir, "miniature/cover").deleteRecursively()

                showCustomToast("重启APP后会重新截取封面", Toast.LENGTH_SHORT, 3)
            }
        }

    //onCreate END
    }

    //Functions
    //测试版可用性检查
    private fun turnOFF_Switch_UseTestPlayer(){
        PREFS_UseTestingPlayer = false
        Switch_UseTestingPlayer.isChecked = false
        PREFS.edit { putBoolean("PREFS_UseTestingPlayer", false) }

    }
    private fun turnON_Switch_UseTestPlayer(){
        PREFS_UseTestingPlayer = true
        PREFS.edit { putBoolean("PREFS_UseTestingPlayer", true) }

    }
    private fun check_PREFS_UseTestingPlayer(){

        if (PREFS_UseTestingPlayer){
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = packageInfo.signingInfo

            if (signingInfo == null) {
                showCustomToast("签名错误", Toast.LENGTH_SHORT, 3)
                turnOFF_Switch_UseTestPlayer()
                return
            }

            if (signingInfo.hasMultipleSigners()) {
                showCustomToast("签名错误", Toast.LENGTH_SHORT, 3)
                turnOFF_Switch_UseTestPlayer()
                return
            }

            val signatures = signingInfo.signingCertificateHistory
            for (sig in signatures) {
                val cert = sig.toByteArray()
                val input = ByteArrayInputStream(cert)
                val cf = CertificateFactory.getInstance("X509")
                val c = cf.generateCertificate(input) as X509Certificate
                val name = c.subjectDN.name
                if (name.contains("Android Debug")) {
                    showCustomToast("当前程序可使用测试版界面", Toast.LENGTH_SHORT, 3)
                    turnON_Switch_UseTestPlayer()
                }else{
                    showCustomToast("Release版本不能使用测试版页面", Toast.LENGTH_SHORT, 3)
                    turnOFF_Switch_UseTestPlayer()
                }
            }
        }else{
            turnOFF_Switch_UseTestPlayer()
        }

    }
    //数值设置
    @SuppressLint("SetTextI18n")
    private fun chooseSeekHandlerGap(gap: Long) {
        ToolVibrate().vibrate(this)
        PREFS_SeekHandlerGap = gap
        PREFS.edit { putLong("PREFS_SeekHandlerGap", gap) }
        val currentSeekHandlerGap = findViewById<TextView>(R.id.currentSeekHandlerGap)
        if (PREFS_SeekHandlerGap == 50L) {
            currentSeekHandlerGap.text = "默认 (50 毫秒)"
        } else {
            currentSeekHandlerGap.text = "$PREFS_SeekHandlerGap 毫秒"
        }
    }
    @SuppressLint("InflateParams", "SetTextI18n")
    private fun setSeekHandlerGap() {
        ToolVibrate().vibrate(this)
        val dialog = Dialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_player_dialog_input_value, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description: TextView = dialogView.findViewById(R.id.dialog_description)
        val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
        val Button: Button = dialogView.findViewById(R.id.dialog_button)

        title.text = "自定义：播放器寻帧间隔"
        Description.text = "输入自定义滚动进度条时的寻帧间隔"
        EditText.hint = "单位：毫秒丨默认值：50"
        Button.text = "确定"

        val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        Button.setOnClickListener {
            val gapInput = EditText.text.toString().toLongOrNull()
            if (gapInput == null || gapInput == 0L) {
                showCustomToast("未输入内容", Toast.LENGTH_SHORT, 3)
                dialog.dismiss()
                return@setOnClickListener
            }
            else if (gapInput > 1000) {
                showCustomToast("寻帧间隔不能大于1秒", Toast.LENGTH_SHORT, 3)
                dialog.dismiss()
                return@setOnClickListener
            }
            else {
                PREFS_SeekHandlerGap = gapInput
                PREFS.edit { putLong("PREFS_SeekHandlerGap", gapInput).apply() }
                //界面刷新
                val currentSeekHandlerGap = findViewById<TextView>(R.id.currentSeekHandlerGap)
                currentSeekHandlerGap.text = "$PREFS_SeekHandlerGap 毫秒"
                dialog.dismiss()
            }
            dialog.dismiss()
        }
        dialog.show()
        //自动弹出键盘程序
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            EditText.requestFocus()
            imm.showSoftInput(EditText, InputMethodManager.SHOW_IMPLICIT)
        }

    }
    @SuppressLint("SetTextI18n")
    private fun chooseTimeUpdateGap(gap: Long) {
        ToolVibrate().vibrate(this)
        PREFS_TimeUpdateGap = gap
        PREFS.edit { putLong("PREFS_TimeUpdateGap", gap) }
        val currentTimeUpdateGap = findViewById<TextView>(R.id.currentTimeUpdateGap)
        if (PREFS_TimeUpdateGap == 66L) {
            currentTimeUpdateGap.text = "默认 (66 毫秒)"
        } else {
            currentTimeUpdateGap.text = "$PREFS_TimeUpdateGap 毫秒"
        }
    }
    @SuppressLint("InflateParams", "SetTextI18n")
    private fun setTimeUpdateGap() {
        ToolVibrate().vibrate(this)
        val dialog = Dialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_player_dialog_input_value, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description: TextView = dialogView.findViewById(R.id.dialog_description)
        val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
        val Button: Button = dialogView.findViewById(R.id.dialog_button)

        title.text = "自定义：播放器时间更新间隔"
        Description.text = "输入自定义时间更新间隔"
        EditText.hint = "单位：毫秒丨默认值：66"
        Button.text = "确定"

        val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        Button.setOnClickListener {
            val gapInput = EditText.text.toString().toLongOrNull()
            if (gapInput == null || gapInput == 0L) {
                showCustomToast("未输入内容", Toast.LENGTH_SHORT, 3)
                dialog.dismiss()
                return@setOnClickListener

            }
            else if (gapInput > 1000) {
                showCustomToast("时间更新间隔不能大于1秒", Toast.LENGTH_SHORT, 3)
                dialog.dismiss()
                return@setOnClickListener
            }
            else {
                PREFS_TimeUpdateGap = gapInput
                PREFS.edit { putLong("PREFS_TimeUpdateGap", gapInput).apply() }
                //界面刷新
                val currentTimeUpdateGap = findViewById<TextView>(R.id.currentTimeUpdateGap)
                currentTimeUpdateGap.text = "$PREFS_TimeUpdateGap 毫秒"
                dialog.dismiss()
            }
        }
        dialog.show()
        //自动弹出键盘程序
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            EditText.requestFocus()
            imm.showSoftInput(EditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    @SuppressLint("SetTextI18n")
    private fun chooseVibrateMillis(gap: Long) {
        //跟随系统
        if (gap == -1L) {
            PREFS_UseSysVibrate = true
            PREFS.edit { putBoolean("PREFS_UseSysVibrate", true) }
            val currentVibrateMillis = findViewById<TextView>(R.id.currentVibratorMillis)
            currentVibrateMillis.text = "跟随系统"
            ToolVibrate().readVibrateSetting(this)
            ToolVibrate().vibrate(this)
        }
        //自定时长
        else{
            PREFS_VibrateMillis = gap
            PREFS_UseSysVibrate = false
            PREFS.edit { putLong("PREFS_VibrateMillis", gap).apply() }
            PREFS.edit { putBoolean("PREFS_UseSysVibrate", false).apply() }
            val currentVibrateMillis = findViewById<TextView>(R.id.currentVibratorMillis)
            if (PREFS_VibrateMillis == 0L) {
                currentVibrateMillis.text = "关闭"
            } else {
                currentVibrateMillis.text = "$PREFS_VibrateMillis 毫秒"
            }
            ToolVibrate().readVibrateSetting(this)
            ToolVibrate().vibrate(this)
        }
    }
    @SuppressLint("InflateParams", "SetTextI18n")
    private fun setVibrateMillis() {
        ToolVibrate().vibrate(this)
        val dialog = Dialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_player_dialog_input_value, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description: TextView = dialogView.findViewById(R.id.dialog_description)
        val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
        val Button: Button = dialogView.findViewById(R.id.dialog_button)

        title.text = "自定义：振动时长"
        Description.text = "输入自定义振动时长"
        EditText.hint = "单位：毫秒"
        Button.text = "确定"

        val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        Button.setOnClickListener {
            val gapInput = EditText.text.toString().toLongOrNull()
            if (gapInput == null || gapInput == 0L) {
                showCustomToast("未输入内容", Toast.LENGTH_SHORT, 3)
                dialog.dismiss()
                return@setOnClickListener

            }
            else if (gapInput > 300L) {
                Toast.makeText(this, "振动时长不能大于300毫秒", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            }
            else {
                PREFS_VibrateMillis = gapInput
                PREFS_UseSysVibrate = false
                PREFS.edit { putLong("PREFS_VibrateMillis", gapInput).apply() }
                PREFS.edit { putBoolean("PREFS_UseSysVibrate", false).apply() }
                //界面刷新
                val currentVibrateMillis = findViewById<TextView>(R.id.currentVibratorMillis)
                if (PREFS_VibrateMillis == 0L) {
                    currentVibrateMillis.text = "关闭"
                } else {
                    currentVibrateMillis.text = "$PREFS_VibrateMillis 毫秒"
                }
                dialog.dismiss()
            }
        }
        dialog.show()
        //自动弹出键盘程序
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            EditText.requestFocus()
            imm.showSoftInput(EditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    //检查应用列表
    private var packageNumber = 0
    private fun checkMicroG(): Boolean {
        packageNumber = 0
        val packageManager = packageManager
        val installedPackages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        var packageName = ""
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

}