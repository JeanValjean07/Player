package com.suming.player.AddonTools

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
object ToolVibrate {
    //context
    private lateinit var context: Application
    fun setContext(context: Context){
        //检查是不是applicationContext
        if (context is Application) {

            this.context = context
        }
    }


    //安卓版本标识
    private var state_SDK_version = 0

    //振动设置
    private var private_PREFS: SharedPreferences ?= null
    const val private_PREFS_Name = "private_ToolVibrate_PREFS_1145"
    private fun init_PREFS(){
        if (private_PREFS == null){
            private_PREFS = context.getSharedPreferences(private_PREFS_Name, Context.MODE_PRIVATE)
        }
    }
    private var PREFS_VibrateMode = -1
    const val PREFS_VibrateMode_Name = "PREFS_Vibrate"
    fun GET_PREFS_VibrateMode(): Int {
        init_PREFS()

        if (PREFS_VibrateMode == -1){
            PREFS_VibrateMode = private_PREFS?.getInt(PREFS_VibrateMode_Name,-1) ?: -1
            if (PREFS_VibrateMode == -1){
                PREFS_VibrateMode = 0
                private_PREFS?.edit { putInt(PREFS_VibrateMode_Name,0)}
            }
        }

        return PREFS_VibrateMode
    }
    fun SET_PREFS_VibrateMode(vibrateMode: Int) {
        init_PREFS()

        PREFS_VibrateMode = vibrateMode
        private_PREFS?.edit { putInt(PREFS_VibrateMode_Name, vibrateMode) }
    }



    //重复振动过滤
    private var last_vibrate_millis = 0L



    //振动核心函数
    @Suppress("DEPRECATION")
    private fun Context.vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    //
    private val coroutine_vibrate = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    //振动模式表
    /*
    0 = No Vibrate
    1 = VibrationEffect.EFFECT_CLICK
    2 = VibrationEffect.EFFECT_TICK
    3 = VibrationEffect.EFFECT_DOUBLE_CLICK
    4 = VibrationEffect.EFFECT_HEAVY_CLICK
    5 = 100Ms x DEFAULT_AMPLITUDE (OPPO专用)
    */

    //振动调用
    fun vibrate() {
        coroutine_vibrate.launch {
            //检查sdk版本,低版本时不振动
            if (state_SDK_version == 0){
                state_SDK_version = Build.VERSION.SDK_INT
            }
            if (state_SDK_version < Build.VERSION_CODES.Q ) return@launch

            //重复时过滤
            val current_vibrate_millis = System.currentTimeMillis()
            if (current_vibrate_millis - last_vibrate_millis < 50L) return@launch
            last_vibrate_millis = current_vibrate_millis


            //确保振动模式设置已经获取
            if (PREFS_VibrateMode == -1) GET_PREFS_VibrateMode()

            //执行振动
            val vib = context.vibrator()
            //根据模式振动
            when (PREFS_VibrateMode) {
                0 -> {
                    return@launch
                }
                1 -> {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    vib.vibrate(effect)
                }
                2 -> {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    vib.vibrate(effect)
                }
                3 -> {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                    vib.vibrate(effect)
                }
                4 -> {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    vib.vibrate(effect)
                }
                5 -> {
                    val effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    vib.vibrate(effect)
                }

            }
        }
    }

}