package com.suming.player.BASE_TOOL_BUNDLE

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

//通用工具集：所有项目大概率都会用到的工具
@Suppress("unused")
class BASE_TOOL_BUNDLE {

    //空字符串
    val Undefined = ""
    //安卓版本
    var AndroidSDKVersion: Int = 0
    //应用版本号
    var AppVersion: String = ""



    //Dp 转 Px
    fun dpToPx(dp: Float): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }
    //Px 转 Dp
    fun pxToDp(px: Int): Int {
        return (px / Resources.getSystem().displayMetrics.density).toInt()
    }


    //检测当前是release版还是debug版
    @RequiresApi(Build.VERSION_CODES.P)
    fun isDebugVersion(context: Context): Boolean {
        //获取安卓版本
        if (AndroidSDKVersion == 0) AndroidSDKVersion = Build.VERSION.SDK_INT
        //仅支持安卓9及以上版本
        if (AndroidSDKVersion < Build.VERSION_CODES.P) return false


        //获取签名信息
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signingInfo = packageInfo.signingInfo
        //
        if (signingInfo == null) return false
        if (signingInfo.hasMultipleSigners()) return false


        val signatures = signingInfo.signingCertificateHistory
        for (sig in signatures) {
            val cert = sig.toByteArray()
            val input = ByteArrayInputStream(cert)
            val cf = CertificateFactory.getInstance("X509")
            val c = cf.generateCertificate(input) as X509Certificate
            val name = c.subjectDN.name
            if (name.contains("Android Debug")) {
                return true
            }else{
                return false
            }
        }

        return true
    }
    //获取版本号
    fun getAppVersion(context: Context): String {
        if (AppVersion == Undefined){
            AppVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
        }

        return AppVersion
    }


}