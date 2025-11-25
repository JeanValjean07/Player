package com.suming.player

import android.os.Build

object RomUtils {

    fun getRomName(): String = when (getProp("ro.build.version.emui").takeIf { it.isNotEmpty() }) {
        null -> when (getProp("ro.build.version.opporom").takeIf { it.isNotEmpty() }) {
            null -> when (getProp("ro.build.version.rom").takeIf { it.isNotEmpty() }) {
                null -> when {
                    Build.DISPLAY.contains("ONE UI", ignoreCase = true) -> "One UI"
                    Build.DISPLAY.contains("HARMONY", ignoreCase = true) -> "HarmonyOS"
                    else -> ""
                }
                else -> "MIUI"
            }
            else -> "ColorOS"
        }
        else -> "EMUI"
    }

    private fun getProp(key: String): String =
        try {
            Runtime.getRuntime().exec("getprop $key").inputStream.bufferedReader().use { it.readText().trim() }
        }
        catch (ignore: Throwable) {
            ""
        }
}