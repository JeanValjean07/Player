package com.suming.player.FuncionalPack

object SettingsHelper {


    //默认设置判断器
    //启用媒体会话艺术图(返回值 1 = 允许开启   2 = 不允许开启   3 = 允许开启但提示未测试)
    fun spySupport_EnableMediaSessionArtWork():Int{
        val brand = DeviceInfo.GET_BRAND()
        val android_version = DeviceInfo.GET_AndroidVersion()

        when(brand){
            "huawei","honor" -> {
                when{
                    android_version == 29 -> {
                        return 1
                    }
                    android_version == 31 -> {
                        return 2
                    }
                    else -> {
                        return 3
                    }
                }
            }
            "samsung" -> {
                when{
                    (android_version >= 31) -> {
                        return 1
                    }
                    else -> {
                        return 3
                    }
                }
            }
            else -> {
                return 3
            }
        }
    }









}