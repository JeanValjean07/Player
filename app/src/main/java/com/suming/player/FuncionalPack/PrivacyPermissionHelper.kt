package com.suming.player.FuncionalPack

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlin.collections.mutableListOf
import androidx.core.content.edit


class PrivacyPermissionHelper {

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PermissionHelper: $msg")
        }
    }

    //设置清单
    private var Pandora_PrivacyPermission: SharedPreferences? = null
    val Pandora_PrivacyPermission_Name = "Pandora_PrivacyPermission"
    private fun initSharedPreferences(context: Context){
        if (Pandora_PrivacyPermission == null){
            Pandora_PrivacyPermission = context.getSharedPreferences(Pandora_PrivacyPermission_Name, 0)
        }
    }


    //外部发起检查隐私政策是否同意
    fun checkPrivacyAgreed(context: Context): Boolean{
        initSharedPreferences(context)

        //检查隐私政策是否同意
        isPrivacyAgreed = Pandora_PrivacyPermission!!.getBoolean(Pandora_PrivacyPermission_Name, false)

        return isPrivacyAgreed
    }
    fun setPrivacyAgreed(context: Context, agreed: Boolean){
        initSharedPreferences(context)

        //设置隐私政策是否同意
        Pandora_PrivacyPermission!!.edit { putBoolean(Pandora_PrivacyPermission_Name, agreed) }
    }


    //检查储存权限有效性
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkPermissionValidity(context: Context): Boolean{
        val isBasicPermissionGranted = checkBasicStoragePermission(context)
        val isManagerPermissionGranted = isAllFilesAccessGranted()

        //任意一个有效就有效
        return isBasicPermissionGranted || isManagerPermissionGranted

    }


    //检查隐私政策是否同意
    var isPrivacyAgreed: Boolean = false
    //检查权限是否授予
    var isStoragePermissionGranted: Boolean = false


    //检查基本储存权限(返回true代表检查通过,返回false代表需要弹出请求页面)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkBasicStoragePermission(context: Context): Boolean{
        return when{
            //安卓13及以上需检查
            DeviceInfo.AndroidVersion >= 13 -> {
                isVideoAndAudioAccessGranted(context)

            }
            DeviceInfo.AndroidVersion in 11..12 -> {
                false
            }
            DeviceInfo.AndroidVersion in 8..10 -> {
                false
            }
            else -> {
                false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getGrantedStorageListAboveTiramisu(context: Context): List<String> {
        val permissionsToRequest = mutableListOf<String>()

        permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
        permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)

        val grantedPermissions = permissionsToRequest.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        return grantedPermissions

    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun isVideoAndAudioAccessGranted(context: Context): Boolean {
        val list = getGrantedStorageListAboveTiramisu(context)

        return list.contains(Manifest.permission.READ_MEDIA_VIDEO) &&
                list.contains(Manifest.permission.READ_MEDIA_AUDIO)
    }

    //单独检查视频权限(安卓13及以上才有)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun isVideoAccessGranted(context: Context): Boolean {
        return getGrantedStorageListAboveTiramisu(context).contains(Manifest.permission.READ_MEDIA_VIDEO)
    }
    //单独检查音频权限(安卓13及以上才有)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun isAudioAccessGranted(context: Context): Boolean {
        return getGrantedStorageListAboveTiramisu(context).contains(Manifest.permission.READ_MEDIA_AUDIO)
    }

    //检查访问所有文件(安卓11及以上才有)
    @RequiresApi(Build.VERSION_CODES.R)
    fun isAllFilesAccessGranted(): Boolean {

        return Environment.isExternalStorageManager()
    }


    //检查读取储存权限(安卓12及以下使用)
    fun isBasicStorageGranted(context: Context): Boolean {

        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }









}