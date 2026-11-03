package com.suming.player.BASE_TOOL_BUNDLE

import android.content.res.Resources

//通用工具集：所有项目大概率都会用到的工具
@Suppress("unused")
class BASE_TOOL_BUNDLE {




    //Dp 转 Px
    fun dpToPx(dp: Float): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }
    //Px 转 Dp
    fun pxToDp(px: Int): Int {
        return (px / Resources.getSystem().displayMetrics.density).toInt()
    }




}