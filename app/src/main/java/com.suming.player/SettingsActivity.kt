package com.suming.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

    private lateinit var Switch1: SwitchCompat
    private lateinit var Switch2: SwitchCompat
    private lateinit var Switch3: SwitchCompat
    private var generateThumbSYNC = 1
    private var seekSYNC = 1
    private var exitWhenEnd = 1


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
        //按钮：开放源代码许可
        val buttonLicense = findViewById<Button>(R.id.buttonLicense)
        buttonLicense.setOnClickListener {
            startActivity(
                Intent(this, com.google.android.gms.oss.licenses.OssLicensesMenuActivity::class.java)
            )
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
        //按钮：前往项目Github仓库页
        val buttonGoGithub = findViewById<TextView>(R.id.buttonGoGithub)
        buttonGoGithub.setOnClickListener {
            val url = "https://github.com/JeanValjean07/Player"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }



        Switch1 = findViewById(R.id.generateThumbSYNC)
        Switch2 = findViewById(R.id.seekSYNC)
        Switch3 = findViewById(R.id.exitWhenEnd)
        Switch1.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("generateThumbSYNC", isChecked)
        }
        restoreSwitchState("generateThumbSYNC")
        Switch2.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("seekSYNC", isChecked)
        }
        restoreSwitchState("seekSYNC")
        Switch3.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("exitWhenEnd", isChecked)
        }
        restoreSwitchState("exitWhenEnd")







    }


    private val settingsPrefs = mapOf(
        "generateThumbSYNC"          to ::generateThumbSYNC,
        "seekSYNC"                   to ::seekSYNC,
        "exitWhenEnd"                to ::exitWhenEnd,
    )

    private fun saveSwitchState(key: String, isChecked: Boolean) {
        val prop = settingsPrefs[key] ?: return
        val value = if (isChecked) 1 else 0
        prop.set(value)
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit { putInt(key, value).apply() }
    }

    private fun restoreSwitchState(key: String) {
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (key == "generateThumbSYNC"){
            generateThumbSYNC = sharedPreferences.getInt("generateThumbSYNC", 1)
            if (generateThumbSYNC == 1){
                Switch1.isChecked = true
            }
            else{
                Switch1.isChecked = false
            }
        }
        if (key == "seekSYNC"){
            seekSYNC = sharedPreferences.getInt("seekSYNC", 1)
            if (seekSYNC == 1){
                Switch2.isChecked = true
            }
            else{
                Switch2.isChecked = false
            }
        }
        if (key == "exitWhenEnd"){
            exitWhenEnd = sharedPreferences.getInt("exitWhenEnd", 1)
            if (exitWhenEnd == 1){
                Switch3.isChecked = true
            }
            else{
                Switch3.isChecked = false
            }
        }
    }
}