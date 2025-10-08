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
import com.google.android.gms.oss.licenses.OssLicensesActivity


class SettingsActivity: AppCompatActivity() {

    private lateinit var Switch1: SwitchCompat

    private lateinit var Switch3: SwitchCompat
    private lateinit var Switch4: SwitchCompat
    private lateinit var Switch5: SwitchCompat
    private lateinit var Switch6: SwitchCompat

    private lateinit var Switch8: SwitchCompat
    private lateinit var Switch9: SwitchCompat
    private lateinit var Switch10: SwitchCompat

    private var generateThumbSYNC = true

    private var exitWhenEnd = false
    private var useLongScroller = false
    private var useLongSeekGap = false
    private var useBlackScreenInLandscape = false

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
        val buttonGoGithub = findViewById<TextView>(R.id.buttonGoGithub)
        buttonGoGithub.setOnClickListener {
            val url = "https://github.com/JeanValjean07/Player"
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



        Switch1 = findViewById(R.id.generateThumbSYNC)
        Switch1.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_GenerateThumbSYNC", isChecked)
        }
        restoreSwitchState("PREFS_GenerateThumbSYNC")

        restoreSwitchState("PREFS_SeekSYNC")
        Switch3 = findViewById(R.id.exitWhenEnd)
        Switch3.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_ExitWhenEnd", isChecked)
        }
        restoreSwitchState("PREFS_ExitWhenEnd")
        Switch4 = findViewById(R.id.useLongScroller)
        Switch4.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_UseLongScroller", isChecked)
        }
        restoreSwitchState("PREFS_UseLongScroller")
        Switch5 = findViewById(R.id.useLongSeekGap)
        Switch5.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_UseLongSeekGap", isChecked)
        }
        restoreSwitchState("PREFS_UseLongSeekGap")
        Switch6 = findViewById(R.id.useBlackScreenInLandscape)
        Switch6.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_UseBlackScreenInLandscape", isChecked)
        }
        restoreSwitchState("PREFS_UseBlackScreenInLandscape")

        restoreSwitchState("PREFS_UseMVVMPlayer")
        Switch8 = findViewById(R.id.useHighRefreshRate)
        Switch8.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_UseHighRefreshRate", isChecked)
        }
        restoreSwitchState("PREFS_UseHighRefreshRate")
        Switch9 = findViewById(R.id.useCompatScroller)
        Switch9.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_UseCompatScroller", isChecked)
        }
        restoreSwitchState("PREFS_UseCompatScroller")
        Switch10 = findViewById(R.id.closeVideoTrack)
        Switch10.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("PREFS_CloseVideoTrack", isChecked)
        }
        restoreSwitchState("PREFS_CloseVideoTrack")


    } //onCreate END

    private val settingsPrefs = mapOf(
        "PREFS_GenerateThumbSYNC"         to ::generateThumbSYNC,
        "PREFS_ExitWhenEnd"               to ::exitWhenEnd,
        "PREFS_UseLongScroller"           to ::useLongScroller,
        "PREFS_UseLongSeekGap"            to ::useLongSeekGap,
        "PREFS_UseBlackScreenInLandscape" to ::useBlackScreenInLandscape,
        "PREFS_UseHighRefreshRate"        to ::useHighRefreshRate,
        "PREFS_UseCompatScroller"         to ::useCompatScroller,
        "PREFS_CloseVideoTrack"           to ::closeVideoTrack,
    )

    private fun saveSwitchState(key: String, isChecked: Boolean) {
        val prop = settingsPrefs[key] ?: return
        if (isChecked) 1 else 0
        prop.set(isChecked)
        getSharedPreferences("PREFS_Player", MODE_PRIVATE)
            .edit { putBoolean(key, isChecked).apply() }
    }

    private fun restoreSwitchState(key: String) {
        val sharedPreferences = getSharedPreferences("PREFS_Player", MODE_PRIVATE)
        if (key == "PREFS_GenerateThumbSYNC"){
            generateThumbSYNC = sharedPreferences.getBoolean("PREFS_GenerateThumbSYNC", true)
            if (generateThumbSYNC){
                Switch1.isChecked = true
            }
            else{
                Switch1.isChecked = false
            }
        }

        if (key == "PREFS_ExitWhenEnd"){
            exitWhenEnd = sharedPreferences.getBoolean("PREFS_ExitWhenEnd", false)
            if (exitWhenEnd){
                Switch3.isChecked = true
            }
            else{
                Switch3.isChecked = false
            }
        }
        if (key == "PREFS_UseLongScroller"){
            useLongScroller = sharedPreferences.getBoolean("PREFS_UseLongScroller", false)
            if (useLongScroller){
                Switch4.isChecked = true
            }
            else{
                Switch4.isChecked = false
            }
        }
        if (key == "PREFS_UseLongSeekGap"){
            useLongSeekGap = sharedPreferences.getBoolean("PREFS_UseLongSeekGap", false)
            if (useLongSeekGap){
                Switch5.isChecked = true
            }
            else{
                Switch5.isChecked = false
            }
        }
        if (key == "PREFS_UseBlackScreenInLandscape"){
            useBlackScreenInLandscape = sharedPreferences.getBoolean("PREFS_UseBlackScreenInLandscape", false)
            if (useBlackScreenInLandscape){
                Switch6.isChecked = true
            }
            else{
                Switch6.isChecked = false
            }
        }

        if (key == "PREFS_UseHighRefreshRate"){
            useHighRefreshRate = sharedPreferences.getBoolean("PREFS_UseHighRefreshRate", false)
            if (useHighRefreshRate){
                Switch8.isChecked = true
            }
            else{
                Switch8.isChecked = false
            }
        }
        if (key == "PREFS_UseCompatScroller"){
            useCompatScroller = sharedPreferences.getBoolean("PREFS_UseCompatScroller", false)
            if (useCompatScroller){
                Switch9.isChecked = true
            }
            else{
                Switch9.isChecked = false
            }
        }
        if (key == "PREFS_CloseVideoTrack"){
            if (!sharedPreferences.contains("PREFS_CloseVideoTrack")){
                sharedPreferences.edit { putBoolean("PREFS_CloseVideoTrack", true).apply() }
            }
            closeVideoTrack = sharedPreferences.getBoolean("PREFS_CloseVideoTrack", true)
            if (closeVideoTrack){
                Switch10.isChecked = true
            }
            else{
                Switch10.isChecked = false
            }
        }
    }
}