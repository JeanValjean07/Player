package com.suming.player

import android.content.Context
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
class ToolVibrate() {
    //振动配置
    private var state_SDK_version = 0
    private var PREFS_VibrateMode = -1

    //振动
    private fun Context.vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

    //震动控制
    fun vibrate(context: Context) {
        //检查sdk版本,低版本时不振动
        if (state_SDK_version == 0){ state_SDK_version = Build.VERSION.SDK_INT }
        if (state_SDK_version < Build.VERSION_CODES.Q){ return }

        //确保振动配置已初始化
        if (PREFS_VibrateMode == -1) { loadVibrateSetting(context) }


        val vib = context.vibrator()
        //根据模式振动
        when (PREFS_VibrateMode) {
            0 -> {
                return
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
        }
    }
    //振动模式表
    /*
    0 = No Vibrate
    1 = VibrationEffect.EFFECT_CLICK
    2 = VibrationEffect.EFFECT_TICK
    3 = VibrationEffect.EFFECT_DOUBLE_CLICK
    4 = VibrationEffect.EFFECT_HEAVY_CLICK
    */

    //读取振动配置
    private fun loadVibrateSetting(context: Context): Int {
        val PREFS = context.getSharedPreferences("PREFS_Vibrate", Context.MODE_PRIVATE)
        PREFS_VibrateMode = PREFS.getInt("PREFS_VibrateMode", 0)

        return PREFS_VibrateMode
    }

    //公共函数丨Public Functions
    //获取振动模式
    fun getVibrateMode(context: Context): Int {
        if (PREFS_VibrateMode == -1) {
            PREFS_VibrateMode = loadVibrateSetting(context)
        }

        return PREFS_VibrateMode
    }
    //设置振动模式
    fun setVibrateMode(context: Context, vibrateMode: Int) {
        val PREFS = context.getSharedPreferences("PREFS_Vibrate", Context.MODE_PRIVATE)
        PREFS_VibrateMode = vibrateMode
        PREFS.edit().putInt("PREFS_VibrateMode", vibrateMode).apply()
    }


}

