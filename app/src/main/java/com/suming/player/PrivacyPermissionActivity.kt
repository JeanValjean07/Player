package com.suming.player

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.suming.player.FuncionalPack.PrivacyPermissionHelper
import com.suming.player.ViewWidget.CircleButton
import kotlin.system.exitProcess
import android.os.Process
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.suming.player.FuncionalPack.DeviceInfo

class PrivacyPermissionActivity: AppCompatActivity() {

    //隐私政策权限类
    val PrivacyPermissionHelper = PrivacyPermissionHelper()

    //隐私政策同意状态
    private var isPrivacyAgreed: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_privacy_permission)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //初始化
        init()

        //注册操作
        register()

        //启动主任务线
        mainBusiness()

    }

    //初始化
    private fun init() {
        level_permissions = findViewById(R.id.level_permissions)
        level_privacy = findViewById(R.id.level_privacy)
        //获取状态栏高度
        if (DeviceInfo.statusBarHeight == 0){
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_root_constraint)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                DeviceInfo.statusBarHeight = systemBars.top

                //重组主要视图
                display()

                insets
            }
        }
    }
    private fun display(){

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
            PrivacyPermissionHelper.setPrivacyAgreed(this, true)
            showTargetPage()
        }
        val privacy_button_disagree = findViewById<CardView>(R.id.privacy_button_disagree)
        privacy_button_disagree.setOnClickListener {
            PrivacyPermissionHelper.setPrivacyAgreed(this, false)
            exit()
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
        //显示目标页面
        showTargetPage()
    }




    //显示目标页面
    private fun showTargetPage(){
        isPrivacyAgreed = PrivacyPermissionHelper.checkPrivacyAgreed(this)
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

    //退出决策程序
    private fun exit(){
        if (!isPrivacyAgreed){
            finishAffinity()
            //import android.os.Process
            Process.killProcess(Process.myPid())
            exitProcess(0)

        }else{
            finish()
        }
    }


    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "FragmentPrivacyPermission: $msg")
        }
    }

}