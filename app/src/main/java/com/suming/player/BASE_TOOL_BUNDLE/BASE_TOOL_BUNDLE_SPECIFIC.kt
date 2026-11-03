package com.suming.player.BASE_TOOL_BUNDLE

import android.annotation.SuppressLint

//特定工具集：只有当前项目才常用的工具
@Suppress("unused")
class BASE_TOOL_BUNDLE_SPECIFIC {


    //格式化时间
    @SuppressLint("DefaultLocale")
    fun FormatTime_withChar(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours == 0L){
            String.format("%02d分%02d秒",  minutes, seconds)
        }else{
            String.format("%02d时%02d分%02d秒",  hours, minutes, seconds)
        }
    }






}