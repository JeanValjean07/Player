package com.suming.player.FuncionalPack

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlin.collections.mutableListOf


object PermissionHelper {

    //引用
    private lateinit var context: Application
    fun setContext(context: Context){
        //检查是不是applicationContext
        if (context is Application) {
            consoleLog("PlayerSingleton.setContext")
            this.context = context
        }else{
            consoleLog("PlayerSingleton.setContext error")
        }
    }
    fun getApplicationContext(): Context = context.applicationContext
    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PermissionHelper: $msg")
        }
    }



    //检查权限
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkStoragePermission(){
        when{
            //安卓13及以上需检查
            DeviceInfo.AndroidVersion >= 13 -> {


            }
            DeviceInfo.AndroidVersion in 11..12 -> {

            }
            DeviceInfo.AndroidVersion in 8..10 -> {

            }
            else -> {

            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getGrantedStorageListAboveTiramisu(): List<String> {
        val permissionsToRequest = mutableListOf<String>()

        permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
        permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)

        val grantedPermissions = permissionsToRequest.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        return grantedPermissions

    }

    //单独检查视频权限(安卓13及以上才有)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun isVideoAccessGranted(): Boolean {
        return getGrantedStorageListAboveTiramisu().contains(Manifest.permission.READ_MEDIA_VIDEO)
    }
    //单独检查音频权限(安卓13及以上才有)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun isAudioAccessGranted(): Boolean {
        return getGrantedStorageListAboveTiramisu().contains(Manifest.permission.READ_MEDIA_AUDIO)
    }

    //检查访问所有文件(安卓11及以上才有)
    @RequiresApi(Build.VERSION_CODES.R)
    fun isAllFilesAccessGranted(): Boolean {

        return Environment.isExternalStorageManager()
    }


    //检查读取储存权限(安卓12及以下使用)
    fun isBasicStorageGranted(): Boolean {

        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }









}