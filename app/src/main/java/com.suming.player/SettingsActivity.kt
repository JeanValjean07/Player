package com.suming.player

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
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
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import data.MediaItemDao
import data.MediaItemDataBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsActivity: AppCompatActivity() {

    //开关初始化
    private lateinit var Switch_CloseVideoTrack: SwitchCompat
    private lateinit var Switch_SwitchPortraitWhenExit: SwitchCompat
    private lateinit var Switch_EnableRoomDatabase: SwitchCompat
    private lateinit var Switch_ExitWhenEnd: SwitchCompat
    private lateinit var Switch_UseLongScroller: SwitchCompat
    private lateinit var Switch_UseLongSeekGap: SwitchCompat
    private lateinit var Switch_UseCompatScroller: SwitchCompat
    private lateinit var Switch_GenerateThumbSync: SwitchCompat
    private lateinit var Switch_UseOnlySyncFrame: SwitchCompat
    private lateinit var Switch_UseBlackBackground: SwitchCompat
    private lateinit var Switch_UseHighRefreshRate: SwitchCompat
    private lateinit var Switch_CloseFragmentGesture: SwitchCompat

    //开关变量 + 数值量
    private var PREFS_CloseVideoTrack = false
    private var PREFS_SwitchPortraitWhenExit = true
    private var PREFS_EnableRoomDatabase = false
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

    //按钮循环：清除所有视频缓存
    private var ButtonRemoveAllThumbPathIndex = 0


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
                Intent(
                    this,
                    com.google.android.gms.oss.licenses.OssLicensesMenuActivity::class.java
                )
            )
        }

        //静态操作部分:::
        //读取设置
        val PREFS = getSharedPreferences("PREFS", MODE_PRIVATE)
        val PREFS_Editor = PREFS.edit()
        if (!PREFS.contains("PREFS_CloseVideoTrack")) {
            PREFS_Editor.putBoolean("PREFS_CloseVideoTrack", true)
            PREFS_CloseVideoTrack = true
        } else {
            PREFS_CloseVideoTrack = PREFS.getBoolean("PREFS_CloseVideoTrack", false)
        }
        if (!PREFS.contains("PREFS_SwitchPortraitWhenExit")) {
            PREFS_Editor.putBoolean("PREFS_SwitchPortraitWhenExit", true)
            PREFS_SwitchPortraitWhenExit = true
        } else {
            PREFS_SwitchPortraitWhenExit = PREFS.getBoolean("PREFS_SwitchPortraitWhenExit", true)
        }
        if (!PREFS.contains("PREFS_SeekHandlerGap")) {
            PREFS_Editor.putLong("PREFS_SeekHandlerGap", 20L)
            PREFS_SeekHandlerGap = 20L
        } else {
            PREFS_SeekHandlerGap = PREFS.getLong("PREFS_SeekHandlerGap", 20L)
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
        if (!PREFS.contains("PREFS_UseOnlySyncFrame")) {
            PREFS_Editor.putBoolean("PREFS_UseOnlySyncFrame", false)
            PREFS_UseOnlySyncFrame = false
        } else {
            PREFS_UseOnlySyncFrame = PREFS.getBoolean("PREFS_UseOnlySyncFrame", false)
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
        Switch_UseOnlySyncFrame = findViewById(R.id.UseOnlySyncFrame)
        Switch_UseBlackBackground = findViewById(R.id.useBlackBackground)
        Switch_UseHighRefreshRate = findViewById(R.id.useHighRefreshRate)
        Switch_CloseFragmentGesture = findViewById(R.id.closeFragmentGesture)
        //开关预置位
        Switch_CloseVideoTrack.isChecked = PREFS_CloseVideoTrack
        Switch_SwitchPortraitWhenExit.isChecked = PREFS_SwitchPortraitWhenExit
        Switch_EnableRoomDatabase.isChecked = PREFS_EnableRoomDatabase
        Switch_ExitWhenEnd.isChecked = PREFS_ExitWhenEnd
        Switch_UseLongScroller.isChecked = PREFS_UseLongScroller
        Switch_UseLongSeekGap.isChecked = PREFS_UseLongSeekGap
        Switch_UseCompatScroller.isChecked = PREFS_UseCompatScroller
        Switch_GenerateThumbSync.isChecked = PREFS_GenerateThumbSYNC
        Switch_UseOnlySyncFrame.isChecked = PREFS_UseOnlySyncFrame
        Switch_UseBlackBackground.isChecked = PREFS_UseBlackBackground
        Switch_UseHighRefreshRate.isChecked = PREFS_UseHighRefreshRate
        Switch_CloseFragmentGesture.isChecked = PREFS_CloseFragmentGesture

        //文本信息预写
        val currentSeekHandlerGap = findViewById<TextView>(R.id.currentSeekHandlerGap)
        if (PREFS_SeekHandlerGap == 20L) {
            currentSeekHandlerGap.text = "默认 (20毫秒)"
        } else {
            currentSeekHandlerGap.text = "$PREFS_SeekHandlerGap 毫秒"
        }
        val currentTimeUpdateGap = findViewById<TextView>(R.id.currentTimeUpdateGap)
        if (PREFS_TimeUpdateGap == 16L) {
            currentTimeUpdateGap.text = "默认 (16毫秒丨60Hz)"
        } else {
            currentTimeUpdateGap.text = "$PREFS_TimeUpdateGap 毫秒"
        }


        //动态操作部分:::
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
        Switch_UseOnlySyncFrame.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseOnlySyncFrame = isChecked
        }
        Switch_UseBlackBackground.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseBlackBackground = isChecked
        }
        Switch_UseHighRefreshRate.setOnCheckedChangeListener { _, isChecked ->
            PREFS_UseHighRefreshRate = isChecked
        }
        Switch_CloseFragmentGesture.setOnCheckedChangeListener { _, isChecked ->
            PREFS_CloseFragmentGesture = isChecked
        }

        //seek间隔
        val ButtonSeekHandlerGap = findViewById<TextView>(R.id.ButtonSeekHandlerGap)
        ButtonSeekHandlerGap.setOnClickListener { item ->
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


        //重新生成封面
        val ButtonRemoveAllThumbPath = findViewById<TextView>(R.id.RemoveAllThumbPath)
        ButtonRemoveAllThumbPath.setOnClickListener { item ->
            if (ButtonRemoveAllThumbPathIndex == 0) {
                ButtonRemoveAllThumbPathIndex = 1
                ButtonRemoveAllThumbPath.text = "请再次点击确认重新生成"
            } else if (ButtonRemoveAllThumbPathIndex == 1) {
                ButtonRemoveAllThumbPathIndex = 0
                ButtonRemoveAllThumbPath.text = "已确认重新生成"
                lifecycleScope.launch {
                    val db = MediaItemDataBase.get(this@SettingsActivity)
                    val dao = db.mediaItemDao()
                    dao.removeAllThumbPath("")
                }
                showCustomToast("重启APP后会重新截取封面", Toast.LENGTH_SHORT, 3)
            }
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
        PREFS_Editor.putBoolean("PREFS_CloseFragmentGesture", PREFS_CloseFragmentGesture)
        PREFS_Editor.putBoolean("PREFS_UseOnlySyncFrame", PREFS_UseOnlySyncFrame)
        PREFS_Editor.apply()
    }

    //Functions
    @SuppressLint("SetTextI18n")
    private fun chooseSeekHandlerGap(gap: Long) {
        PREFS_SeekHandlerGap = gap
        val PREFS = getSharedPreferences("PREFS", MODE_PRIVATE)
        PREFS.edit {
            putLong("PREFS_SeekHandlerGap", gap)
        }
        val currentSeekHandlerGap = findViewById<TextView>(R.id.currentSeekHandlerGap)
        if (PREFS_SeekHandlerGap == 0L) {
            currentSeekHandlerGap.text = "默认 (0 毫秒)"
        } else {
            currentSeekHandlerGap.text = "$PREFS_SeekHandlerGap 毫秒"
        }
    }
    @SuppressLint("InflateParams", "SetTextI18n")
    private fun setSeekHandlerGap() {
        val dialog = Dialog(this)
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.activity_player_dialog_input_value, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description: TextView = dialogView.findViewById(R.id.dialog_description)
        val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
        val Button: Button = dialogView.findViewById(R.id.dialog_button)

        title.text = "自定义：播放器寻帧间隔"
        Description.text = "输入自定义滚动进度条时的寻帧间隔"
        EditText.hint = "单位：毫秒丨默认值：20"
        Button.text = "确定"

        val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        Button.setOnClickListener {
            val gapInput = EditText.text.toString().toLongOrNull()

            if (gapInput == null || gapInput == 0L) {
                Toast.makeText(this, "未输入内容", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            } else if (gapInput > 1000) {
                Toast.makeText(this, "寻帧间隔不能大于1秒", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            } else {
                PREFS_SeekHandlerGap = gapInput
                val PREFS = getSharedPreferences("PREFS", MODE_PRIVATE)
                PREFS.edit { putLong("PREFS_SeekHandlerGap", gapInput).commit() }
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
        PREFS_TimeUpdateGap = gap
        val PREFS = getSharedPreferences("PREFS", MODE_PRIVATE)
        PREFS.edit {
            putLong("PREFS_TimeUpdateGap", gap)
        }
        val currentTimeUpdateGap = findViewById<TextView>(R.id.currentTimeUpdateGap)
        if (PREFS_TimeUpdateGap == 16L) {
            currentTimeUpdateGap.text = "默认 (16 毫秒)"
        } else {
            currentTimeUpdateGap.text = "$PREFS_TimeUpdateGap 毫秒"
        }
    }
    @SuppressLint("InflateParams", "SetTextI18n")
    private fun setTimeUpdateGap() {
        val dialog = Dialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_player_dialog_input_value, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description: TextView = dialogView.findViewById(R.id.dialog_description)
        val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
        val Button: Button = dialogView.findViewById(R.id.dialog_button)

        title.text = "自定义：播放器时间更新间隔"
        Description.text = "输入自定义时间更新间隔"
        EditText.hint = "单位：毫秒丨默认值：16"
        Button.text = "确定"

        val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        Button.setOnClickListener {
            val gapInput = EditText.text.toString().toLongOrNull()
            if (gapInput == null || gapInput == 0L) {
                Toast.makeText(this, "未输入内容", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener

            }
            else if (gapInput > 1000) {
                Toast.makeText(this, "时间更新间隔不能大于1秒", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            }
            else {
                PREFS_TimeUpdateGap = gapInput
                val PREFS = getSharedPreferences("PREFS", MODE_PRIVATE)
                PREFS.edit { putLong("PREFS_TimeUpdateGap", gapInput).commit() }
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


}