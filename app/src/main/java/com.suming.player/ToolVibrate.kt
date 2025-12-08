package com.suming.player

import android.content.Context
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class ToolVibrate() {
    //振动配置
    private var state_vibrateSettingExist = false
    private var PREFS_UseSysVibrate = true
    private var PREFS_VibrateMillis = 50L

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
        if (!state_vibrateSettingExist) {
            readVibrateSetting(context)
        }
        val vib = context.vibrator()
        if (PREFS_UseSysVibrate) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            vib.vibrate(effect)
        }
        else{
            if (PREFS_VibrateMillis <= 0L) {
                return
            }
            else{
                vib.vibrate(VibrationEffect.createOneShot(PREFS_VibrateMillis, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    //读取振动配置
    fun readVibrateSetting(context: Context) {
        val PREFS = context.getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        PREFS_UseSysVibrate = PREFS.getBoolean("PREFS_UseSysVibrate", true)
        PREFS_VibrateMillis = PREFS.getLong("PREFS_VibrateMillis", 50L)
        state_vibrateSettingExist = true
    }


}