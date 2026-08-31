package com.suming.player.FuncionalPack

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



    //Android版本
    var AndroidVersion : Int = 0


    //获取安卓版本(安卓10-api29,安卓11-api30,安卓12-api31,安卓13-api33,安卓14-api34,15-api35,16-api36,17-api37)
    fun GET_AndroidVersion(): Int{
        if (AndroidVersion == 0){
            AndroidVersion = android.os.Build.VERSION.SDK_INT
        }

        return AndroidVersion
    }



    //BRAND
    var BRAND : String = ""

    //获取BRAND
    fun GET_BRAND(): String{
        if (BRAND == ""){
            BRAND = android.os.Build.BRAND.lowercase()
        }

        return BRAND

    }


}