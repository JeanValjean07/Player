package com.suming.player

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.security.MessageDigest

object ToolIdentifyMicroG {
    //GMS官方证书哈希
    private val GOOGLE_CERT_SHA256 = listOf(
        "38918a453d07199354f8b19af05ec6562ced2788d4ec7b53f58250727e986f18",
        "7ce83c1b71f3ff9f7d9e9a6f8c9e5d5a6e5f6d7c8b9a0b1c2d3e4f5a6b7c8d9e0f",
        "7ce83c1b71f3d572fed04c8d40c5cb10ff75e6d87d9df6fbd53f0468c2905053",
    )
    //类型枚举
    enum class GmsType {
        OFFICIAL,   //官方GMS
        MICRO_G,    //microG
        MISSING     //未安装GMS
    }

    fun check(context: Context): GmsType {
        val pkg = "com.google.android.gms"
        val packageManager = context.packageManager
        return try {
            val sigs = packageManager.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners

            if (sigs?.isEmpty() ?: true) return GmsType.MISSING
            val sha256 = bytesToHex(
                MessageDigest.getInstance("SHA-256")
                .digest(sigs[0].toByteArray())).lowercase()

            Log.d("SuMing", "check: $sha256")

            if (sha256 in GOOGLE_CERT_SHA256) GmsType.OFFICIAL
            else GmsType.MICRO_G

        }
        catch (_: PackageManager.NameNotFoundException) {
            GmsType.MISSING
        }
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

}