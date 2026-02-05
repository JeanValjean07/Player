package com.suming.player

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit


object ValueManager {


    private lateinit var PREFS_ValueManager : SharedPreferences
    private var state_valueList_initialized = false

    private fun initValueManager(context: Context){
        PREFS_ValueManager = context.getSharedPreferences("PREFS_ValueManager", 0)
    }



    //播放区域上移距离px
    private var value_PlayAreaMoveDistance: Float = 0f
    fun get_Value_PlayAreaMoveDistance(context: Context): Float{
        if (!state_valueList_initialized) initValueManager(context)

        value_PlayAreaMoveDistance = PREFS_ValueManager.getFloat("value_PlayAreaMoveDistance", 0f)
        if (value_PlayAreaMoveDistance == 0f) calculate_value_PlayAreaMoveDistance(context)

        return value_PlayAreaMoveDistance
    }
    private fun calculate_value_PlayAreaMoveDistance(context: Context){
        if (!state_valueList_initialized) initValueManager(context)

        val displayMetrics = context.resources.displayMetrics
        //屏幕宽高px
        val widthPx = displayMetrics.widthPixels
        val heightPx = displayMetrics.heightPixels
        //卡片上剩余高度px
        val areaHeightPx = heightPx * 0.3
        //中心点
        val normalCenterMarginTop = heightPx / 2f
        val areaCenterMarginTop = (areaHeightPx / 2f)
        //中心点移动距离
        val value_PlayAreaMoveDistance = (normalCenterMarginTop - areaCenterMarginTop).toFloat()


        PREFS_ValueManager.edit{ putFloat("value_PlayAreaMoveDistance", value_PlayAreaMoveDistance) }

    }



}