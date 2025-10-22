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

class SettingsActivity: AppCompatActivity() {

    private lateinit var Switch_GenerateThumbSync: SwitchCompat
    private lateinit var Switch_ExitWhenEnd: SwitchCompat
    private lateinit var Switch_UseLongScroller: SwitchCompat
    private lateinit var Switch_UseLongSeekGap: SwitchCompat
    private lateinit var Switch_UseBlackBackground: SwitchCompat
    private lateinit var Switch_UseHighRefreshRate: SwitchCompat
    private lateinit var Switch_UseCompatScroller: SwitchCompat
    private lateinit var Switch_CloseVideoTrack: SwitchCompat

    private var generateThumbSYNC = true
    private var exitWhenEnd = false
    private var useLongScroller = false
    private var useLongSeekGap = false
    private var useBlackBackground = false
    private var useHighRefreshRate = false
    private var useCompatScroller = false
    private var closeVideoTrack = false

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
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



        Switch_GenerateThumbSync = findViewById(R.id.generateThumbSYNC)
        Switch_GenerateThumbSync.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_GenerateThumbSYNC", isChecked)
        }
        restoreSwitchState("PREFS_GenerateThumbSYNC")

        restoreSwitchState("PREFS_SeekSYNC")
        Switch_ExitWhenEnd = findViewById(R.id.exitWhenEnd)
        Switch_ExitWhenEnd.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_ExitWhenEnd", isChecked)
        }
        restoreSwitchState("PREFS_ExitWhenEnd")
        Switch_UseLongScroller = findViewById(R.id.useLongScroller)
        Switch_UseLongScroller.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_UseLongScroller", isChecked)
        }
        restoreSwitchState("PREFS_UseLongScroller")
        Switch_UseLongSeekGap = findViewById(R.id.useLongSeekGap)
        Switch_UseLongSeekGap.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_UseLongSeekGap", isChecked)
        }
        restoreSwitchState("PREFS_UseLongSeekGap")
        Switch_UseBlackBackground = findViewById(R.id.useBlackBackground)
        Switch_UseBlackBackground.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_UseBlackBackground", isChecked)
        }
        restoreSwitchState("PREFS_UseBlackBackground")

        restoreSwitchState("PREFS_UseMVVMPlayer")
        Switch_UseHighRefreshRate = findViewById(R.id.useHighRefreshRate)
        Switch_UseHighRefreshRate.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_UseHighRefreshRate", isChecked)
        }
        restoreSwitchState("PREFS_UseHighRefreshRate")
        Switch_UseCompatScroller = findViewById(R.id.useCompatScroller)
        Switch_UseCompatScroller.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_UseCompatScroller", isChecked)
        }
        restoreSwitchState("PREFS_UseCompatScroller")
        Switch_CloseVideoTrack = findViewById(R.id.closeVideoTrack)
        Switch_CloseVideoTrack.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_CloseVideoTrack", isChecked)
        }
        restoreSwitchState("PREFS_CloseVideoTrack")


    } //onCreate END

    private val settingsPrefs = mapOf(
        "PREFS_GenerateThumbSYNC"         to ::generateThumbSYNC,
        "PREFS_ExitWhenEnd"               to ::exitWhenEnd,
        "PREFS_UseLongScroller"           to ::useLongScroller,
        "PREFS_UseLongSeekGap"            to ::useLongSeekGap,
        "PREFS_UseBlackBackground"        to ::useBlackBackground,
        "PREFS_UseHighRefreshRate"        to ::useHighRefreshRate,
        "PREFS_UseCompatScroller"         to ::useCompatScroller,
        "PREFS_CloseVideoTrack"           to ::closeVideoTrack,
    )

    private fun saveSwitchState(key: String, isChecked: Boolean) {
        val prop = settingsPrefs[key] ?: return
        if (isChecked) 1 else 0
        prop.set(isChecked)
        getSharedPreferences("PREFS", MODE_PRIVATE)
            .edit { putBoolean(key, isChecked).apply() }
    }

    private fun restoreSwitchState(key: String) {
        val sharedPreferences = getSharedPreferences("PREFS", MODE_PRIVATE)
        if (key == "PREFS_GenerateThumbSYNC"){
            generateThumbSYNC = sharedPreferences.getBoolean("PREFS_GenerateThumbSYNC", true)
            if (generateThumbSYNC){
                Switch_GenerateThumbSync.isChecked = true
            }
            else{
                Switch_GenerateThumbSync.isChecked = false
            }
        }
        if (key == "PREFS_ExitWhenEnd"){
            exitWhenEnd = sharedPreferences.getBoolean("PREFS_ExitWhenEnd", false)
            if (exitWhenEnd){
                Switch_ExitWhenEnd.isChecked = true
            }
            else{
                Switch_ExitWhenEnd.isChecked = false
            }
        }
        if (key == "PREFS_UseLongScroller"){
            useLongScroller = sharedPreferences.getBoolean("PREFS_UseLongScroller", false)
            if (useLongScroller){
                Switch_UseLongScroller.isChecked = true
            }
            else{
                Switch_UseLongScroller.isChecked = false
            }
        }
        if (key == "PREFS_UseLongSeekGap"){
            useLongSeekGap = sharedPreferences.getBoolean("PREFS_UseLongSeekGap", false)
            if (useLongSeekGap){
                Switch_UseLongSeekGap.isChecked = true
            }
            else{
                Switch_UseLongSeekGap.isChecked = false
            }
        }
        if (key == "PREFS_UseBlackBackground"){
            useBlackBackground = sharedPreferences.getBoolean("PREFS_UseBlackBackground", false)
            if (useBlackBackground){
                Switch_UseBlackBackground.isChecked = true
            }
            else{
                Switch_UseBlackBackground.isChecked = false
            }
        }
        if (key == "PREFS_UseHighRefreshRate"){
            useHighRefreshRate = sharedPreferences.getBoolean("PREFS_UseHighRefreshRate", false)
            if (useHighRefreshRate){
                Switch_UseHighRefreshRate.isChecked = true
            }
            else{
                Switch_UseHighRefreshRate.isChecked = false
            }
        }
        if (key == "PREFS_UseCompatScroller"){
            useCompatScroller = sharedPreferences.getBoolean("PREFS_UseCompatScroller", false)
            if (useCompatScroller){
                Switch_UseCompatScroller.isChecked = true
            }
            else{
                Switch_UseCompatScroller.isChecked = false
            }
        }
        if (key == "PREFS_CloseVideoTrack"){
            if (!sharedPreferences.contains("PREFS_CloseVideoTrack")){
                sharedPreferences.edit { putBoolean("PREFS_CloseVideoTrack", true).apply() }
            }
            closeVideoTrack = sharedPreferences.getBoolean("PREFS_CloseVideoTrack", true)
            if (closeVideoTrack){
                Switch_CloseVideoTrack.isChecked = true
            }
            else{
                Switch_CloseVideoTrack.isChecked = false
            }
        }
    }
}