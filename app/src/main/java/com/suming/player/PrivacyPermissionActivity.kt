package com.suming.player

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.suming.player.FuncionalPack.PrivacyPermissionHelper
import com.suming.player.ViewWidget.CircleButton
import android.provider.Settings
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.FuncionalPack.ActivityResultConnector
import com.suming.player.FuncionalPack.DeviceInfo

@SuppressLint("NewApi")
class PrivacyPermissionActivity: AppCompatActivity() {


    //隐私政策权限类
    val PrivacyPermissionHelper = PrivacyPermissionHelper()

    //隐私政策同意状态
    private var isPrivacyAgreed: Boolean = false
    //储存权限有效状态
    private var isStoragePermissionValid: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_privacy_permission)
        //初始化
        init()

        //注册操作
        register()

        //启动主任务线
        mainBusiness()

    }

    override fun onResume() {
        super.onResume()

        //检查权限状态
        checkPermissionState()
    }
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        //使用收起动画
        overridePendingTransition(
                    R.anim.slide_just_appear,
                    R.anim.slide_out_vertical
                )

    }

    //初始化
    private fun init() {
        level_permissions = findViewById(R.id.level_permissions)
        level_privacy = findViewById(R.id.level_privacy)
        //获取状态栏高度
        if (DeviceInfo.statusBarHeight == 0){
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                DeviceInfo.statusBarHeight = systemBars.top

                //重组主要视图
                display()

                insets
            }
        }else{
            display()
        }
    }
    private fun display(){
        val topBar_topSafeArea = findViewById<View>(R.id.topBar_topSafeArea)
        topBar_topSafeArea.layoutParams.height = DeviceInfo.statusBarHeight + 10

        val topBar = findViewById<LinearLayout>(R.id.topBar)
        topBar.post {

            val topBar_totalHeight = topBar.height

            val scrollArea_topSafeArea = findViewById<View>(R.id.scrollArea_topSafeArea)
            scrollArea_topSafeArea.layoutParams.height = topBar_totalHeight + 10
            scrollArea_topSafeArea.requestLayout()
        }





    }
    //注册操作
    private fun register(){
        //退出按钮
        val ButtonExit = findViewById<CircleButton>(R.id.ButtonExit)
        ButtonExit.setOnClickListener {
            exit()
        }
        //隐私政策同意页面
        val privacy_button_agree = findViewById<CardView>(R.id.privacy_button_agree)
        privacy_button_agree.setOnClickListener {
            ToolVibrate().vibrate(this@PrivacyPermissionActivity)

            PrivacyPermissionHelper.setPrivacyAgreed(this, true)
            checkPermissionState()
        }
        val privacy_button_disagree = findViewById<CardView>(R.id.privacy_button_disagree)
        privacy_button_disagree.setOnClickListener {
            ToolVibrate().vibrate(this@PrivacyPermissionActivity)

            PrivacyPermissionHelper.setPrivacyAgreed(this, false)
            exit()
        }

        //授予权限
        val button_permission_all_file = findViewById<CardView>(R.id.button_permission_all_file)
        button_permission_all_file.setOnClickListener {
            ToolVibrate().vibrate(this@PrivacyPermissionActivity)

            requestAllFilePermission()
        }
        val button_permission_basic_storage = findViewById<CardView>(R.id.button_permission_basic_storage)
        button_permission_basic_storage.setOnClickListener {
            ToolVibrate().vibrate(this@PrivacyPermissionActivity)

            requestBasicStoragePermission()
        }

        //在不授予权限的情况下继续
        val button_continue_without_permission = findViewById<CardView>(R.id.button_continue_without_permission)
        button_continue_without_permission.setOnClickListener {
            ToolVibrate().vibrate(this@PrivacyPermissionActivity)

            sendActivityResult(ActivityResultConnector.ARAPI_Privacy_continue_without_storage_permission)
            finish()
        }
        //继续且不再提示
        val button_continue_and_never_alert = findViewById<CardView>(R.id.button_continue_and_never_alert)
        button_continue_and_never_alert.setOnClickListener {
            ToolVibrate().vibrate(this@PrivacyPermissionActivity)

            PrivacyPermissionHelper.SET_PREFS_IgnoreStorageNeverAlert(this, true)
            sendActivityResult(ActivityResultConnector.ARAPI_Privacy_continue_without_storage_permission)
            finish()
        }


        //监听返回手势(DialogFragment)
        /*
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                exitFragment()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

         */
        //监听系统手势返回
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exit()
            }
        })
    }
    //主任务线
    private fun mainBusiness(){
        //检查权限状态
        checkPermissionState()

        //显示目标页面
        showTargetPage()

        //显示权限提示
        showPermissionPrompt()
    }



    //检查权限状态
    private fun checkPermissionState(){
        //检查隐私政策同意状态
        isPrivacyAgreed = PrivacyPermissionHelper.checkPrivacyAgreed(this)
        //检查储存权限有效性
        isStoragePermissionValid = PrivacyPermissionHelper.checkPermissionValidity(this)

        //consoleLog("隐私政策已同意: $isPrivacyAgreed  储存权限有效: $isStoragePermissionValid")

        //权限均有效时主动退出
        if (isPrivacyAgreed && isStoragePermissionValid){
            successfullyGetPermission()
            //showCustomToast("权限与隐私检查已通过,本页面将自动退出")
            finish()
        }else{
            showTargetPage()
        }

    }

    //显示目标页面
    private fun showTargetPage(){
        if (isPrivacyAgreed){
            level_privacy.visibility = View.GONE
            level_permissions.visibility = View.VISIBLE
        }else{
            level_privacy.visibility = View.VISIBLE
            level_permissions.visibility = View.GONE
        }
    }
    //页面视图
    private lateinit var level_privacy: LinearLayout
    private lateinit var level_permissions: LinearLayout

    //显示权限提示
    @SuppressLint("SetTextI18n")
    private fun showPermissionPrompt(){
        val permission_prompt_text = findViewById<TextView>(R.id.permission_prompt_text)
        //确保已缓存安卓版本号
        if (DeviceInfo.AndroidVersion == 0){
            DeviceInfo.AndroidVersion = Build.VERSION.SDK_INT
        }
        //根据安卓版本显示
        when{
            DeviceInfo.AndroidVersion >= Build.VERSION_CODES.TIRAMISU -> {
                permission_prompt_text.text = permission_prompt_text_tiramisu + permission_prompt_safe_mode + permission_prompt_all_file_access
            }
            else -> {
                permission_prompt_text.text = permission_prompt_text_snow_cone + permission_prompt_safe_mode + permission_prompt_all_file_access
            }
        }


    }
    val permission_prompt_text_tiramisu = "在安卓13及以上版本，需要同时开启视频和音频的访问权限。开启“所有文件访问权限”可获得最高自由度。也可选择不开启任何权限，选择文件播放。"
    val permission_prompt_text_snow_cone = "在安卓12及以下版本，需要开启储存权限。开启“所有文件访问权限”可获得最高自由度。"
    val permission_prompt_safe_mode = "\n\n也可选择不开启任何权限，选择文件播放。"
    val permission_prompt_all_file_access = "\n\n如果您需要访问非公有文件夹和被.nomedia标记的文件夹内的媒体，则必须开启“所有文件访问权限”。"

    //要求所有文件访问权限
    private fun requestAllFilePermission(){
        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        startActivity(intent)
    }
    //要求基本储存权限
    private fun requestBasicStoragePermission(){
        if (DeviceInfo.AndroidVersion == 0){
            DeviceInfo.AndroidVersion = Build.VERSION.SDK_INT
        }
        when{
            DeviceInfo.AndroidVersion >= Build.VERSION_CODES.TIRAMISU -> {
                val permissionsToRequest = arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    )

                val ungrantedPermissions = permissionsToRequest.filter {
                    ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                }.toTypedArray()

                permissionLauncher.launch(ungrantedPermissions)

            }
            else -> {
                val permissionsToRequest = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                val ungrantedPermissions = permissionsToRequest.filter {
                    ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                }.toTypedArray()
                permissionLauncher.launch(ungrantedPermissions)
            }
        }

    }
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap ->
        val allGranted = resultMap.values.all { it }

        if (!allGranted) {
            //有权限被拒绝，检查是否被永久拒绝
            val permanentlyDenied = resultMap.filter { !it.value }
                .keys
                .any { permission ->
                    !shouldShowRequestPermissionRationale(permission)
                }

            if (permanentlyDenied) {
                showCustomToast("请开启“音乐和音频”与“照片和视频”权限")
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = "package:$packageName".toUri() }
                startActivity(intent)
            }
        }
    }

    //ActivityResultAPI
    private fun sendActivityResult( dataString: String?) {
        val resultIntent = Intent().apply {
            putExtra(ActivityResultConnector.ARAPI_Privacy, dataString)
        }
        setResult(RESULT_OK, resultIntent)
    }

    //检查通过
    private fun successfullyGetPermission(){

        sendActivityResult(ActivityResultConnector.ARAPI_Privacy_continue_with_success_permit)
    }


    //退出决策程序
    private fun exit(){
        if (!isPrivacyAgreed){
            showCustomToast("未同意隐私政策,程序将退出")
            finishAffinity()
            //import android.os.Process
            //Process.killProcess(Process.myPid())
            //exitProcess(0)
            return
        }
        if (!isStoragePermissionValid){
            finishAffinity()
            return
        }

        finish()

    }


    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PrivacyPermissionActivity: $msg")
        }
    }

}