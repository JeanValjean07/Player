package com.suming.player.FuncionalPack

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log

object DeviceInfo {

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "DeviceInfo: $msg")
        }
    }

    //状态栏高度
    var statusBarHeight : Int = 0



    //屏幕宽高
    private var screenWidth_px : Int = 0
    private var screenHeight_px : Int = 0



    //Android版本
    var AndroidVersion : Int = 0


    //不要在这里统一承载DisplayMetrics,因为切横屏时不方便更新,不如每个活动自带一个
    /*
    private var DisplayMetrics: DisplayMetrics ?= null
    private fun initDisplayMetrics(context: Context){
        if (DisplayMetrics == null){
            DisplayMetrics = context.resources.displayMetrics
        }

    }

    //获取屏幕宽高
    fun GET_INFO_screenWidthHeight(context: Context): Pair<Int, Int>{
        initDisplayMetrics(context)

        if (screenWidth_px == 0){
            screenWidth_px = DisplayMetrics?.widthPixels ?: 0
        }
        if (screenHeight_px == 0){
            screenHeight_px = DisplayMetrics?.heightPixels ?: 0
        }

        return Pair(screenWidth_px, screenHeight_px)
    }
    fun GET_INFO_screenHeight_px(context: Context): Int{
        initDisplayMetrics(context)

        if (screenHeight_px == 0){
            screenHeight_px = DisplayMetrics?.heightPixels ?: 0
        }


        return screenHeight_px
    }
    fun GET_INFO_screenWidth_px(context: Context): Int{
        initDisplayMetrics(context)

        if (screenWidth_px == 0){
            screenWidth_px = DisplayMetrics?.widthPixels ?: 0
        }

        return screenWidth_px
    }
    //获取屏幕density
    fun GET_INFO_screenDensity(context: Context): Float{
        initDisplayMetrics(context)

        return DisplayMetrics?.density ?: 0f
    }

     */


}