package com.suming.player

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.Player

class SettingsActivity: AppCompatActivity() {

    private lateinit var Switch_CloseVideoTrack: SwitchCompat
    private lateinit var Switch_SwitchPortraitWhenExit: SwitchCompat
    private lateinit var Switch_EnableRoomDatabase: SwitchCompat
    private lateinit var Switch_ExitWhenEnd: SwitchCompat
    private lateinit var Switch_UseLongScroller: SwitchCompat
    private lateinit var Switch_UseLongSeekGap: SwitchCompat
    private lateinit var Switch_UseCompatScroller: SwitchCompat
    private lateinit var Switch_GenerateThumbSync: SwitchCompat
    private lateinit var Switch_UseBlackBackground: SwitchCompat
    private lateinit var Switch_UseHighRefreshRate: SwitchCompat

    private var PREFS_CloseVideoTrack = false
    private var PREFS_SwitchPortraitWhenExit = true
    private var PREFS_EnableRoomDatabase = false
    private var PREFS_ExitWhenEnd = false
    private var PREFS_UseLongScroller = false
    private var PREFS_UseLongSeekGap = false
    private var PREFS_UseCompatScroller = false
    private var PREFS_GenerateThumbSYNC = true
    private var PREFS_UseBlackBackground = false
    private var PREFS_UseHighRefreshRate = false






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
        val buttonBack = findViewById<ImageButton>(R.id.buttonExit)
        buttonBack.setOnClickListener {
            finish()
        }
        //按钮：前往项目Github仓库页
        val buttonGoGithub = findViewById<TextView>(R.id.buttonGoGithubRelease)
        buttonGoGithub.setOnClickListener {
            val url = "https://github.com/JeanValjean07/Player/releases"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }
        //超链接：开放源代码许可
        val openSourceLicense = findViewById<TextView>(R.id.openSourceLicense)
        openSourceLicense.paint.isUnderlineText = true
        openSourceLicense.setOnClickListener {
            startActivity(
                Intent(this, com.google.android.gms.oss.licenses.OssLicensesMenuActivity::class.java)
            )
        }

        //读取设置
        val PREFS = getSharedPreferences("PREFS", MODE_PRIVATE)
        val PREFS_Editor = PREFS.edit()
        if (!PREFS.contains("PREFS_CloseVideoTrack")) {
            PREFS_Editor.putBoolean("PREFS_CloseVideoTrack", true)
            PREFS_CloseVideoTrack = true
        } else {
            PREFS_CloseVideoTrack = PREFS.getBoolean("PREFS_CloseVideoTrack", false)
        }
        if (!PREFS.contains("PREFS_SwitchPortraitWhenExit")){
            PREFS_Editor.putBoolean("PREFS_SwitchPortraitWhenExit", true)
            PREFS_SwitchPortraitWhenExit = true
        } else {
            PREFS_SwitchPortraitWhenExit = PREFS.getBoolean("PREFS_SwitchPortraitWhenExit", true)
        }
        if (!PREFS.contains("PREFS_EnableRoomDatabase")) {
            PREFS_Editor.putBoolean("PREFS_EnableRoomDatabase", false)
            PREFS_EnableRoomDatabase = false
        } else {
            PREFS_EnableRoomDatabase = PREFS.getBoolean("PREFS_EnableRoomDatabase", false)
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
        PREFS_Editor.apply()

        //开关初始化
        Switch_CloseVideoTrack = findViewById(R.id.closeVideoTrack)
        Switch_SwitchPortraitWhenExit = findViewById(R.id.SwitchPortraitWhenExit)
        Switch_EnableRoomDatabase = findViewById(R.id.EnableRoomDatabase)
        Switch_ExitWhenEnd = findViewById(R.id.exitWhenEnd)
        Switch_UseLongScroller = findViewById(R.id.useLongScroller)
        Switch_UseLongSeekGap = findViewById(R.id.useLongSeekGap)
        Switch_UseCompatScroller = findViewById(R.id.useCompatScroller)
        Switch_GenerateThumbSync = findViewById(R.id.generateThumbSYNC)
        Switch_UseBlackBackground = findViewById(R.id.useBlackBackground)
        Switch_UseHighRefreshRate = findViewById(R.id.useHighRefreshRate)

        //开关预置位
        Switch_CloseVideoTrack.isChecked = PREFS_CloseVideoTrack
        Switch_SwitchPortraitWhenExit.isChecked = PREFS_SwitchPortraitWhenExit
        Switch_EnableRoomDatabase.isChecked = PREFS_EnableRoomDatabase
        Switch_ExitWhenEnd.isChecked = PREFS_ExitWhenEnd
        Switch_UseLongScroller.isChecked = PREFS_UseLongScroller
        Switch_UseLongSeekGap.isChecked = PREFS_UseLongSeekGap
        Switch_UseCompatScroller.isChecked = PREFS_UseCompatScroller
        Switch_GenerateThumbSync.isChecked = PREFS_GenerateThumbSYNC
        Switch_UseBlackBackground.isChecked = PREFS_UseBlackBackground
        Switch_UseHighRefreshRate.isChecked = PREFS_UseHighRefreshRate

        //开关更改操作
        Switch_CloseVideoTrack.setOnCheckedChangeListener { _, isChecked ->
            PREFS_CloseVideoTrack = isChecked
        }
        Switch_SwitchPortraitWhenExit.setOnCheckedChangeListener { _, isChecked ->
            PREFS_SwitchPortraitWhenExit = isChecked
        }
        Switch_EnableRoomDatabase.setOnCheckedChangeListener { _, isChecked ->
            PREFS_EnableRoomDatabase = isChecked
        }
        Switch_ExitWhenEnd.setOnCheckedChangeListener { _, isChecked ->
            PREFS_ExitWhenEnd = isChecked
        }
        Switch_UseLongScroller.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseLongScroller = isChecked
        }
        Switch_UseLongSeekGap.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseLongSeekGap = isChecked
        }
        Switch_UseCompatScroller.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseCompatScroller = isChecked
        }
        Switch_GenerateThumbSync.setOnCheckedChangeListener { _, isChecked ->
            PREFS_GenerateThumbSYNC = isChecked
        }
        Switch_UseBlackBackground.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseBlackBackground = isChecked
        }
        Switch_UseHighRefreshRate.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseHighRefreshRate = isChecked
        }


    } //onCreate END

    @SuppressLint("CommitPrefEdits", "UseKtx")
    override fun onDestroy() {
        super.onDestroy()
        val PREFS = getSharedPreferences("PREFS", MODE_PRIVATE)
        val PREFS_Editor = PREFS.edit()
        PREFS_Editor.putBoolean("PREFS_CloseVideoTrack", PREFS_CloseVideoTrack)
        PREFS_Editor.putBoolean("PREFS_SwitchPortraitWhenExit", PREFS_SwitchPortraitWhenExit)
        PREFS_Editor.putBoolean("PREFS_EnableRoomDatabase", PREFS_EnableRoomDatabase)
        PREFS_Editor.putBoolean("PREFS_ExitWhenEnd", PREFS_ExitWhenEnd)
        PREFS_Editor.putBoolean("PREFS_UseLongScroller", PREFS_UseLongScroller)
        PREFS_Editor.putBoolean("PREFS_UseLongSeekGap", PREFS_UseLongSeekGap)
        PREFS_Editor.putBoolean("PREFS_UseCompatScroller", PREFS_UseCompatScroller)
        PREFS_Editor.putBoolean("PREFS_GenerateThumbSYNC", PREFS_GenerateThumbSYNC)
        PREFS_Editor.putBoolean("PREFS_UseBlackBackground", PREFS_UseBlackBackground)
        PREFS_Editor.putBoolean("PREFS_UseHighRefreshRate", PREFS_UseHighRefreshRate)
        PREFS_Editor.apply()
    }

}